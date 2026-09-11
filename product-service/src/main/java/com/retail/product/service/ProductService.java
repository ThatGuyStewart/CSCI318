package com.retail.product.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.product.domain.Product;
import com.retail.product.domain.ProductCategory;
import com.retail.product.domain.ProductDomainEvent;
import com.retail.product.domain.ProductView;
import com.retail.product.dto.DomainEventEnvelope;
import com.retail.product.dto.ProductCreateRequest;
import com.retail.product.dto.ProductResponse;
import com.retail.product.dto.ProductUpdateRequest;
import com.retail.product.exception.BadRequestException;
import com.retail.product.exception.ResourceNotFoundException;
import com.retail.product.repository.ProductDomainEventRepository;
import com.retail.product.repository.ProductRepository;
import com.retail.product.repository.ProductViewRepository;

@Service
@Transactional
public class ProductService {

    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product not found with id: ";
    private static final String PRODUCT_ID_REQUIRED_MESSAGE = "Product id is required";
    private static final String PRODUCT_AGGREGATE_TYPE = "Product";

    private final ProductRepository productRepository;
    private final ProductViewRepository productViewRepository;
    private final ProductDomainEventRepository productDomainEventRepository;
    private final ObjectMapper objectMapper;
        private final DomainEventPublisher domainEventPublisher;
        private final String eventTopic;

    public ProductService(ProductRepository productRepository, ProductViewRepository productViewRepository,
            ProductDomainEventRepository productDomainEventRepository,
            ObjectMapper objectMapper, DomainEventPublisher domainEventPublisher,
            @Value("${retail.events.topic}") String eventTopic) {
        this.productRepository = productRepository;
        this.productViewRepository = productViewRepository;
        this.productDomainEventRepository = productDomainEventRepository;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Product name is required");
        }
        if (request.getCategory() == null) {
            throw new BadRequestException("Product category is required");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new BadRequestException("Product price must be greater than 0");
        }

        Product product = new Product(
                request.getName(),
                request.getCategory(),
                request.getPrice(),
                request.getDescription()
        );
        product.nextAggregateVersion();
        Product saved = productRepository.save(product);
        appendProductEvent("ProductCreatedEvent", saved);
        upsertProductView(saved);
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productViewRepository.findAll().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        ProductView product = productViewRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));
        return toProductResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));
        initializeAggregateVersion(product);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getPrice() != null) {
            if (request.getPrice() <= 0) {
                throw new BadRequestException("Product price must be greater than 0");
            }
            product.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        product.nextAggregateVersion();
        Product saved = productRepository.save(Objects.requireNonNull(product, "Product cannot be null"));
        appendProductEvent("ProductUpdatedEvent", saved);
        upsertProductView(saved);
        return toProductResponse(saved);
    }

    public void deleteProduct(Long id) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));
        initializeAggregateVersion(product);
        product.nextAggregateVersion();
        appendProductEvent("ProductDeletedEvent", product);
        productRepository.delete(Objects.requireNonNull(product, "Product to delete cannot be null"));
        productViewRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(ProductCategory category) {
        return productViewRepository.findByCategory(category).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            return productViewRepository.findAll().stream()
                    .map(this::toProductResponse)
                    .toList();
        }
        return productViewRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEvents(LocalDate date, LocalDate from, LocalDate to) {
        return findProductEvents(date, from, to, null, null);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEventsById(Long productId, LocalDate date, LocalDate from, LocalDate to) {
        Long resolvedProductId = Objects.requireNonNull(productId, PRODUCT_ID_REQUIRED_MESSAGE);
        boolean productExists = productViewRepository.existsById(resolvedProductId)
                || productDomainEventRepository.existsByAggregateTypeAndAggregateId(
                        PRODUCT_AGGREGATE_TYPE, resolvedProductId);
        if (!productExists) {
            throw new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + resolvedProductId);
        }
        return findProductEvents(date, from, to, resolvedProductId, null);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEventsByCategory(ProductCategory category, LocalDate date, LocalDate from, LocalDate to) {
        return findProductEvents(date, from, to, null, category);
    }

    private List<DomainEventEnvelope> findProductEvents(LocalDate date, LocalDate from, LocalDate to,
            Long productId, ProductCategory eventCategory) {
        Instant start = null;
        Instant end = null;
        if (date != null) {
            start = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            end = date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        } else if (from != null || to != null) {
            start = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            end = (to != null ? to.plusDays(1) : LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1))
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }
        List<ProductDomainEvent> events;
        if (start == null) {
            events = productId != null
                ? productDomainEventRepository.findByAggregateTypeAndAggregateIdOrderByAggregateVersionAsc(
                    PRODUCT_AGGREGATE_TYPE, productId)
                : eventCategory != null
                    ? productDomainEventRepository.findByAggregateTypeAndEventCategoryOrderByOccurredAtAscAggregateVersionAsc(
                        PRODUCT_AGGREGATE_TYPE, eventCategory)
                    : productDomainEventRepository.findByAggregateTypeOrderByOccurredAtAscAggregateVersionAsc(
                        PRODUCT_AGGREGATE_TYPE);
        } else {
            events = productId != null
                ? productDomainEventRepository.findByAggregateTypeAndAggregateIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByAggregateVersionAsc(
                    PRODUCT_AGGREGATE_TYPE, productId, start, end)
                : eventCategory != null
                    ? productDomainEventRepository.findByAggregateTypeAndEventCategoryAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                        PRODUCT_AGGREGATE_TYPE, eventCategory, start, end)
                    : productDomainEventRepository.findByAggregateTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscAggregateVersionAsc(
                        PRODUCT_AGGREGATE_TYPE, start, end);
        }
        return events.stream()
                .map(this::toDomainEventEnvelope)
                .toList();
    }

    private void appendProductEvent(String eventType, Product product) {
        try {
            Map<String, Object> payload = productPayload(product);
            ProductDomainEvent event = new ProductDomainEvent(UUID.randomUUID(), PRODUCT_AGGREGATE_TYPE,
                    product.getProductId(), product.getCategory(), product.getAggregateVersion(), eventType, Instant.now(), null, null,
                    objectMapper.writeValueAsString(payload));
            productDomainEventRepository.save(event);
            domainEventPublisher.publish(eventTopic, new DomainEventMessage(event.getEventId(), event.getAggregateType(),
                    event.getAggregateId(), event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(),
                    event.getCorrelationId(), event.getCausationId(), payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize product event payload", exception);
        }
    }

    private void initializeAggregateVersion(Product product) {
        if (!product.hasAggregateVersion()) {
            product.initializeAggregateVersion(productDomainEventRepository.findMaxAggregateVersion(
                    PRODUCT_AGGREGATE_TYPE, product.getProductId()));
        }
    }

    private Map<String, Object> productPayload(Product product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getProductId());
        payload.put("name", product.getName());
        payload.put("category", product.getCategory());
        payload.put("price", product.getPrice());
        payload.put("description", product.getDescription());
        return payload;
    }

    private void upsertProductView(Product product) {
        productViewRepository.save(new ProductView(product.getProductId(), product.getName(), product.getCategory(),
                product.getPrice(), product.getDescription()));
    }

    private DomainEventEnvelope toDomainEventEnvelope(ProductDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
            DomainEventEnvelope envelope = new DomainEventEnvelope();
            envelope.setEventId(event.getEventId());
            envelope.setAggregateType(event.getAggregateType());
            envelope.setAggregateId(event.getAggregateId());
            envelope.setAggregateVersion(event.getAggregateVersion());
            envelope.setEventType(event.getEventType());
            envelope.setOccurredAt(event.getOccurredAt());
            envelope.setCorrelationId(event.getCorrelationId());
            envelope.setCausationId(event.getCausationId());
            envelope.setPayload(payload);
            return envelope;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize product event payload", exception);
        }
    }

    public ProductResponse toProductResponse(Product product) {
        Product resolvedProduct = Objects.requireNonNull(product, "Product cannot be null");
        return new ProductResponse(
                resolvedProduct.getProductId(),
                resolvedProduct.getName(),
                resolvedProduct.getCategory(),
                resolvedProduct.getPrice(),
                resolvedProduct.getDescription()
        );
    }

    public ProductResponse toProductResponse(ProductView product) {
        return new ProductResponse(product.getProductId(), product.getName(), product.getCategory(), product.getPrice(),
                product.getDescription());
    }
}
