package com.retail.product.agentic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.retail.product.domain.ProductCategory;
import com.retail.product.domain.ProductView;
import com.retail.product.repository.ProductViewRepository;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

@Component

public class Tools {

    private static final Logger log = LoggerFactory.getLogger(Tools.class);

    private final ProductViewRepository productViewRepository;
    @SuppressWarnings("unused")
    private final dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> inMemoryEmbeddingStore;
    private final EmbeddingModel embeddingModel; // may be null if not configured
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String CUSTOMER_NOT_FOUND = "Customer not found.";
    private static final String ERROR_CONTACTING_SERVICE = "Error contacting %s: ";
    private static final String CUSTOMER_SERVICE = "customer service";
    private static final String NO_MATCHING_PRODUCTS = "No matching products.";
    private static final String NO_CUSTOMER_ID = "No customer id provided.";

    @Value("${customer.service.url:http://localhost:8081}")
    private String customerServiceUrl;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

    public Tools(ProductViewRepository productViewRepository,
            dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> inMemoryEmbeddingStore,
            java.util.Optional<EmbeddingModel> embeddingModelOpt) {
        this.productViewRepository = productViewRepository;
        this.inMemoryEmbeddingStore = inMemoryEmbeddingStore;
        this.embeddingModel = embeddingModelOpt.orElse(null);
    }

    @Tool("Lookup a customer by ID and return a minimal, anonymized preference profile for the agent")
    public String getCustomerProfileById(String customerIdStr) {
        if (customerIdStr == null || customerIdStr.isBlank()) {
            return NO_CUSTOMER_ID;
        }
        try {
            String customerUrl = customerServiceUrl + "/customer/" + customerIdStr.trim();
            Object customer = restTemplate.getForObject(customerUrl, Object.class);
            if (customer == null) {
                return CUSTOMER_NOT_FOUND;
            }
            return buildAnonymizedProfile(customerIdStr.trim());
        } catch (RestClientException ex) {
            return String.format(ERROR_CONTACTING_SERVICE, CUSTOMER_SERVICE) + ex.getMessage();
        }
    }

    @Tool("Lookup a customer by email and return a minimal, anonymized preference profile for the agent")
    public String getCustomerProfileByEmail(String email) {
        if (email == null || email.isBlank()) {
            return "No email provided.";
        }
        try {
            String customerUrl = customerServiceUrl + "/customer/email/" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
            Object customer = restTemplate.getForObject(customerUrl, Object.class);
            if (customer == null) {
                return CUSTOMER_NOT_FOUND;
            }
            // customer response contains id; try to extract id if present
            String id = extractIdFromObject(customer).orElse("unknown");
            return buildAnonymizedProfile(id);
        } catch (RestClientException ex) {
            return String.format(ERROR_CONTACTING_SERVICE, CUSTOMER_SERVICE) + ex.getMessage();
        }
    }

    @Tool("Lookup a customer by phone and return a minimal, anonymized preference profile for the agent")
    public String getCustomerProfileByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "No phone provided.";
        }
        try {
            String customerUrl = customerServiceUrl + "/customer/phone/" + java.net.URLEncoder.encode(phone.trim(), java.nio.charset.StandardCharsets.UTF_8);
            Object customer = restTemplate.getForObject(customerUrl, Object.class);
            if (customer == null) {
                return CUSTOMER_NOT_FOUND;
            }
            String id = extractIdFromObject(customer).orElse("unknown");
            return buildAnonymizedProfile(id);
        } catch (RestClientException ex) {
            return String.format(ERROR_CONTACTING_SERVICE, CUSTOMER_SERVICE) + ex.getMessage();
        }
    }

    @Tool("View an anonymized summary of a customer's order history (internal use only)")
    public String getCustomerOrderHistorySummary(String customerIdStr) {
        if (customerIdStr == null || customerIdStr.isBlank()) {
            return NO_CUSTOMER_ID;
        }
        try {
            String ordersUrl = orderServiceUrl + "/order/customer/" + customerIdStr.trim();
            List<Map> orders = restTemplate.getForObject(ordersUrl, List.class);
            if (orders == null || orders.isEmpty()) {
                return "No orders found for customer.";
            }
            // aggregate counts and spend
            int ordersCount = orders.size();
            double totalSpent = 0.0;
            Map<String, Integer> categoryCounts = new HashMap<>();
            Map<String, Integer> productCounts = new HashMap<>();

            for (Map order : orders) {
                Object itemsObj = order.get("items");
                if (!(itemsObj instanceof List)) {
                    continue;
                }
                List items = (List) itemsObj;
                for (Object itemObj : items) {
                    if (!(itemObj instanceof Map)) {
                        continue;
                    }
                    Map item = (Map) itemObj;
                    Number qtyN = (Number) item.getOrDefault("quantity", 0);
                    int qty = qtyN == null ? 0 : qtyN.intValue();
                    Number priceN = (Number) item.getOrDefault("price", 0);
                    double price = priceN == null ? 0.0 : priceN.doubleValue();
                    totalSpent += price * qty;

                    Object productIdObj = item.get("productId");
                    Long pid = null;
                    if (productIdObj instanceof Number productIdNumber) {
                        pid = productIdNumber.longValue();
                    }

                    if (pid != null) {
                        productCounts.merge("P" + pid, qty, Integer::sum);
                        productViewRepository.findById(pid).ifPresent(pv -> {
                            String cat = pv.getCategory() == null ? "UNKNOWN" : pv.getCategory().name();
                            categoryCounts.merge(cat, qty, Integer::sum);
                        });
                    }
                }
            }

            String categories = categoryCounts.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(","));
            String topProducts = productCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.joining(","));

            return "orders=" + ordersCount + " | spent~=" + String.format("%.2f", totalSpent) + " | categories=" + (categories.isEmpty() ? "none" : categories)
                    + " | topProducts=" + (topProducts.isEmpty() ? "none" : topProducts);

        } catch (RestClientException ex) {
            return String.format(ERROR_CONTACTING_SERVICE, "order service") + ex.getMessage();
        }
    }

    private java.util.Optional<String> extractIdFromObject(Object obj) {
        if (!(obj instanceof Map)) {
            return java.util.Optional.empty();
        }
        Map map = (Map) obj;
        Object id = map.get("customerId");
        if (id == null) {
            id = map.get("id");
        }
        if (id instanceof Number num) {
            return java.util.Optional.of(String.valueOf(num.longValue()));
        }
        if (id instanceof String s) {
            return java.util.Optional.of(s);
        }
        return java.util.Optional.empty();
    }

    private String buildAnonymizedProfile(String customerId) {
        // Build anonymized preference profile by querying orders and summarising categories and product counts
        try {
            String ordersUrl = orderServiceUrl + "/order/customer/" + customerId;
            List<Map> orders = restTemplate.getForObject(ordersUrl, List.class);
            if (orders == null || orders.isEmpty()) {
                return "profile: no orders for customer";
            }
            Map<String, Integer> categoryCounts = new HashMap<>();
            Map<Long, Integer> productCounts = new HashMap<>();
            for (Map order : orders) {
                Object itemsObj = order.get("items");
                if (!(itemsObj instanceof List)) {
                    continue;
                }
                List items = (List) itemsObj;
                for (Object itemObj : items) {
                    if (!(itemObj instanceof Map)) {
                        continue;
                    }
                    Map item = (Map) itemObj;
                    Number qtyN = (Number) item.getOrDefault("quantity", 0);
                    int qty = qtyN == null ? 0 : qtyN.intValue();
                    Object pidObj = item.get("productId");
                    Long pid = null;
                    if (pidObj instanceof Number pidNum) {
                        pid = pidNum.longValue();
                    }
                    if (pid != null) {
                        productCounts.merge(pid, qty, Integer::sum);
                        productViewRepository.findById(pid).ifPresent(pv -> {
                            String cat = pv.getCategory() == null ? "UNKNOWN" : pv.getCategory().name();
                            categoryCounts.merge(cat, qty, Integer::sum);
                        });
                    }
                }
            }
            String categories = categoryCounts.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(","));
            String topProducts = productCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .map(e -> "P" + e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.joining(","));

            return "profile: categories=" + (categories.isEmpty() ? "none" : categories) + " | topProducts=" + (topProducts.isEmpty() ? "none" : topProducts);
        } catch (RestClientException ex) {
            return "Error building profile: " + ex.getMessage();
        }
    }

    @Tool("List all products in one of the catalogue categories")
    public String listProductsByCategory(String category) {
        ProductCategory resolved = resolveCategory(category);
        if (resolved == null) {
            return "Unknown category.";
        }
        return format(productViewRepository.findByCategory(resolved));
    }

    /*
    @Tool("Search products whose name or description contains the given keyword")
    public String searchProductsByKeyword(String keyword) {
        return format(productViewRepository.findByNameContainingIgnoreCase(keyword));
    }

    @Tool("Search products using the embedding store. Falls back to name/description substring matching if embeddings are not available.")
    public String searchProductsByEmbedding(String query) {
        if (query == null || query.isBlank()) {
            return "No query provided.";
        }
        // Build embedding for query, prefer configured EmbeddingModel
        Embedding queryEmbedding = null;
        try {
            if (embeddingModel != null) {
                TextSegment qSeg = TextSegment.from(query);
                Response<Embedding> resp = embeddingModel.embed(qSeg);
                queryEmbedding = resp.content();
            }
        } catch (Exception ex) {
            // fall back to placeholder
        }

        if (queryEmbedding == null) {
            queryEmbedding = createPlaceholderEmbedding(query);
        }

        // If no real embedding model is configured, skip semantic store search and fall back to substring/token matching.
        boolean hasRealEmbeddingModel = (embeddingModel != null);
        EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> result = null;
        if (hasRealEmbeddingModel) {
            EmbeddingSearchRequest request = new EmbeddingSearchRequest(queryEmbedding, 10, 0.0, null);
            try {
                result = inMemoryEmbeddingStore.search(request);
            } catch (Exception ex) {
                // fallthrough to fallback
                result = null;
            }
        }

        List<ProductView> found = new java.util.ArrayList<>();
        try {
            if (result != null) {
                List<EmbeddingMatch<dev.langchain4j.data.segment.TextSegment>> matches = result.matches();
                for (EmbeddingMatch<dev.langchain4j.data.segment.TextSegment> m : matches) {
                    String id = null;
                    try {
                        Method idMethod = null;
                        try {
                            idMethod = m.getClass().getMethod("id");
                        } catch (NoSuchMethodException ignored) {
                        }
                        if (idMethod == null) {
                            try {
                                idMethod = m.getClass().getMethod("getId");
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                        if (idMethod != null) {
                            Object val = idMethod.invoke(m);
                            if (val != null) {
                                id = String.valueOf(val);
                            }
                        }
                    } catch (ReflectiveOperationException | SecurityException ignored) {
                    }

                    if (id != null) {
                        try {
                            long pid = Long.parseLong(id);
                            productViewRepository.findById(pid).ifPresent(pv -> {
                                if (!found.contains(pv)) {
                                    found.add(pv);
                            
                                }});
                            continue;
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    try {
                        Method embeddedMethod = null;
                        try {
                            embeddedMethod = m.getClass().getMethod("embedded");
                        } catch (NoSuchMethodException ignored) {
                        }
                        if (embeddedMethod == null) {
                            try {
                                embeddedMethod = m.getClass().getMethod("getEmbedded");
                            } catch (NoSuchMethodException ignored) {
                            }
                        }
                        if (embeddedMethod != null) {
                            Object embedded = embeddedMethod.invoke(m);
                            if (embedded instanceof dev.langchain4j.data.segment.TextSegment) {
                                dev.langchain4j.data.segment.TextSegment ts = (dev.langchain4j.data.segment.TextSegment) embedded;
                                Long pidL = null;
                                try {
                                    pidL = ts.metadata().getLong("productId");
                                } catch (Throwable ignored) {
                                }
                                if (pidL == null) {
                                    try {
                                        String pidS = ts.metadata().getString("productId");
                                        if (pidS != null) {
                                            pidL = Long.parseLong(pidS);
                                        }
                                    } catch (Throwable ignored) {
                                    }
                                }
                                if (pidL != null) {
                                    long pid = pidL;
                                    productViewRepository.findById(pid).ifPresent(pv -> {
                                        if (!found.contains(pv)) {
                                            found.add(pv);
                                    
                                        }});
                                }
                            }
                        }
                    } catch (ReflectiveOperationException | SecurityException ignored) {
                    }
                }
            }
        } catch (Exception ex) {
            // if anything goes wrong, fallback to substring search
            List<ProductView> byName = productViewRepository.findByNameContainingIgnoreCase(query);
            List<ProductView> byDesc = productViewRepository.findAll().stream()
                    .filter(p -> p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase()))
                    .toList();
            for (ProductView p : byName) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
            for (ProductView p : byDesc) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
        }

        if (found.isEmpty()) {
            // If embedding search found nothing, fall back to substring matching
            // Prefer the first non-empty line of the query (often the product name)
            String _simpleQuery = null;
            if (query != null) {
                for (String line : query.split("\\r?\\n")) {
                    if (line != null && !line.trim().isEmpty()) {
                        _simpleQuery = line.trim();
                        break;
                    }
                }
            }
            final String simpleQuery = (_simpleQuery == null || _simpleQuery.isBlank()) ? query : _simpleQuery;
            try {
                log.info("Embedding search returned no matches for query. Falling back to substring search. query='{}'", simpleQuery);
            } catch (Throwable ignore) {
            }

            // improved fallback: split query into tokens and search by each token to increase recall
            List<ProductView> byName = new java.util.ArrayList<>();
            List<ProductView> byDesc = new java.util.ArrayList<>();
            String[] tokens = simpleQuery.split("\\s+");
            for (String t : tokens) {
                if (t == null || t.isBlank()) {
                    continue;
                }
                byName.addAll(productViewRepository.findByNameContainingIgnoreCase(t));
                byDesc.addAll(productViewRepository.findAll().stream()
                        .filter(p -> p.getDescription() != null && p.getDescription().toLowerCase().contains(t.toLowerCase()))
                        .toList());
            }
            try {
                log.info("Fallback substring search results: byName={} byDesc={}", byName.size(), byDesc.size());
                if (!byName.isEmpty()) {
                    log.info("byName names={}", byName.stream().map(ProductView::getName).toList());
                }
                if (!byDesc.isEmpty()) {
                    log.info("byDesc names={}", byDesc.stream().map(ProductView::getName).toList());
                }
            } catch (Throwable ignore) {
            }
            for (ProductView p : byName) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
            for (ProductView p : byDesc) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
            if (found.isEmpty()) {
                return NO_MATCHING_PRODUCTS;
            }
        }

        return found.stream().limit(10).map(this::formatOne).collect(Collectors.joining("\n"));
    }
     */
    @Tool("Search products using the in-memory embedding store and return matching product lines (ID=...).")
    public String searchProductsByEmbeddingStore(String query) {
        if (query == null || query.isBlank()) {
            return "No query provided.";
        }

        // Build embedding for query, prefer configured EmbeddingModel
        Embedding queryEmbedding = null;
        try {
            if (embeddingModel != null) {
                Response<Embedding> resp = embeddingModel.embed(query);
                queryEmbedding = resp.content();
            }
        } catch (Exception ex) {
            // fall back to placeholder
        }

        if (queryEmbedding == null) {
            queryEmbedding = createPlaceholderEmbedding(query);
        }

        EmbeddingSearchResult<dev.langchain4j.data.segment.TextSegment> result = null;
        EmbeddingSearchRequest request = new EmbeddingSearchRequest(queryEmbedding, 10, 0.5, null);
        try {
            result = inMemoryEmbeddingStore.search(request);
        } catch (Exception ex) {
            result = null;
        }

        List<ProductView> found = new java.util.ArrayList<>();
        try {
            if (result != null) {
                for (EmbeddingMatch<TextSegment> m : result.matches()) {
                    // Primary path: use embeddingId() — the key passed to store.add()
                    String embId = m.embeddingId();
                    if (embId != null) {
                        try {
                            long pid = Long.parseLong(embId);
                            productViewRepository.findById(pid).ifPresent(pv -> {
                                if (!found.contains(pv)) {
                                    found.add(pv);
                                }
                            });
                            continue;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    // Fallback: read productId from TextSegment metadata
                    TextSegment ts = m.embedded();
                    if (ts != null) {
                        Long pidL = null;
                        try {
                            pidL = ts.metadata().getLong("productId");
                        } catch (Throwable ignored) {
                        }
                        if (pidL == null) {
                            try {
                                String pidS = ts.metadata().getString("productId");
                                if (pidS != null) {
                                    pidL = Long.parseLong(pidS);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        if (pidL != null) {
                            long pid = pidL;
                            productViewRepository.findById(pid).ifPresent(pv -> {
                                if (!found.contains(pv)) {
                                    found.add(pv);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // fallback to substring search
            List<ProductView> byName = productViewRepository.findByNameContainingIgnoreCase(query);
            List<ProductView> byDesc = productViewRepository.findAll().stream()
                    .filter(p -> p.getDescription() != null && p.getDescription().toLowerCase().contains(query.toLowerCase()))
                    .toList();
            for (ProductView p : byName) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
            for (ProductView p : byDesc) {
                if (!found.contains(p)) {
                    found.add(p);
                }
            }
        }

        if (found.isEmpty()) {
            return NO_MATCHING_PRODUCTS;
        }

        return found.stream().limit(10).map(this::formatOne).collect(Collectors.joining("\n"));
    }

    private Embedding createPlaceholderEmbedding(String content) {
        int dim = 256;
        float[] vec = new float[dim];
        int h = content == null ? 0 : content.hashCode();
        for (int i = 0; i < dim; i++) {
            vec[i] = ((h >> (i % 32)) & 0xFF) / 255.0f;
        }
        return Embedding.from(vec);
    }

    @Tool("Get full details of a single product by its ID")
    public String getProductById(Long productId) {
        return productViewRepository.findById(productId)
                .map(this::formatOne)
                .orElse("No product found with that ID.");
    }

    /*
    @Tool("Check whether a catalogue category exists and currently has at least one product")
    public String categoryExists(String category) {
        ProductCategory resolved = resolveCategory(category);
        if (resolved == null) {
            return "false";
        }
        return productViewRepository.findByCategory(resolved).isEmpty() ? "false" : "true";
    }
     */
    private ProductCategory resolveCategory(String category) {
        if (category == null) {
            return null;
        }
        try {
            return ProductCategory.valueOf(category.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String format(List<ProductView> products) {
        if (products.isEmpty()) {
            return NO_MATCHING_PRODUCTS;
        }
        return products.stream().map(this::formatOne).collect(Collectors.joining("\n"));
    }

    private String formatOne(ProductView product) {
        return "ID=" + product.getProductId() + " | name=" + product.getName() + " | category=" + product.getCategory()
                + " | price=" + product.getPrice() + " | description=" + product.getDescription();
    }
}
