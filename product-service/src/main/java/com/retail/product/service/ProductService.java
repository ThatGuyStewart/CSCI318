package com.retail.product.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.product.domain.Product;
import com.retail.product.domain.ProductCategory;
import com.retail.product.dto.DomainEventEnvelope;
import com.retail.product.dto.ProductCreateRequest;
import com.retail.product.dto.ProductResponse;
import com.retail.product.dto.ProductUpdateRequest;
import com.retail.product.exception.BadRequestException;
import com.retail.product.exception.ResourceNotFoundException;
import com.retail.product.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product not found with id: ";
    private static final String PRODUCT_ID_REQUIRED_MESSAGE = "Product id is required";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
        Product saved = productRepository.save(product);
        return toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));
        return toProductResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));

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

        productRepository.save(Objects.requireNonNull(product, "Product cannot be null"));
        return toProductResponse(product);
    }

    public void deleteProduct(Long id) {
        Long productId = Objects.requireNonNull(id, PRODUCT_ID_REQUIRED_MESSAGE);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + productId));
        productRepository.delete(Objects.requireNonNull(product, "Product to delete cannot be null"));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(ProductCategory category) {
        return productRepository.findByCategory(category).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            return productRepository.findAll().stream()
                    .map(this::toProductResponse)
                    .toList();
        }
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEvents(LocalDate date, LocalDate from, LocalDate to) {
        return buildProductEvents(date, from, to);
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEventsById(Long productId, LocalDate date, LocalDate from, LocalDate to) {
        Long resolvedProductId = Objects.requireNonNull(productId, PRODUCT_ID_REQUIRED_MESSAGE);
        productRepository.findById(resolvedProductId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE + resolvedProductId));
        return findProductEvents(date, from, to, resolvedProductId);
    }

    private List<DomainEventEnvelope> findProductEvents(LocalDate date, LocalDate from, LocalDate to, Long productId) {
        return buildProductEvents(date, from, to).stream()
                .filter(event -> event.getAggregateId() != null && event.getAggregateId().equals(productId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainEventEnvelope> getProductEventsByCategory(ProductCategory category, LocalDate date, LocalDate from, LocalDate to) {
        List<Product> products = productRepository.findByCategory(category);
        Set<Long> ids = products.stream()
                .map(product -> Objects.requireNonNull(product.getId(), PRODUCT_ID_REQUIRED_MESSAGE))
            .collect(java.util.stream.Collectors.toSet());
        return buildProductEvents(date, from, to).stream()
                .filter(event -> ids.contains(event.getAggregateId()))
                .toList();
    }

    private List<DomainEventEnvelope> buildProductEvents(LocalDate date, LocalDate from, LocalDate to) {
        List<DomainEventEnvelope> events = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            Long productId = Objects.requireNonNull(product.getId(), PRODUCT_ID_REQUIRED_MESSAGE);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("productId", productId);
            payload.put("name", product.getName());
            payload.put("category", product.getCategory());
            payload.put("price", product.getPrice());
            events.add(new DomainEventEnvelope("ProductCreatedEvent", productId, payload));
        }

        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return events.stream().filter(event -> !event.getTimestamp().isBefore(start) && event.getTimestamp().isBefore(end)).toList();
        }
        if (from != null || to != null) {
            LocalDateTime start = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay();
            LocalDateTime end = (to != null ? to : LocalDate.now(ZoneId.systemDefault())).plusDays(1).atStartOfDay();
            return events.stream().filter(event -> !event.getTimestamp().isBefore(start) && event.getTimestamp().isBefore(end)).toList();
        }
        return events;
    }

    public ProductResponse toProductResponse(Product product) {
        Product resolvedProduct = Objects.requireNonNull(product, "Product cannot be null");
        return new ProductResponse(
                resolvedProduct.getId(),
                resolvedProduct.getName(),
                resolvedProduct.getCategory(),
                resolvedProduct.getPrice(),
                resolvedProduct.getDescription()
        );
    }
}
