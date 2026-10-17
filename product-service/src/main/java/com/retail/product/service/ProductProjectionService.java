package com.retail.product.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.product.domain.ProductCategory;
import com.retail.product.domain.ProductProjectionCheckpoint;
import com.retail.product.domain.ProductView;
import com.retail.product.repository.ProductProjectionCheckpointRepository;
import com.retail.product.repository.ProductViewRepository;

@Service
public class ProductProjectionService {

    private static final String PRODUCT_AGGREGATE_TYPE = "Product";

    private final ProductViewRepository productViewRepository;
    private final ProductProjectionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    public ProductProjectionService(ProductViewRepository productViewRepository,
            ProductProjectionCheckpointRepository checkpointRepository, ObjectMapper objectMapper) {
        this.productViewRepository = productViewRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean apply(DomainEventMessage event) {
        Objects.requireNonNull(event, "Product event is required");
        if (!PRODUCT_AGGREGATE_TYPE.equals(event.aggregateType())) {
            throw new IllegalArgumentException("Unexpected aggregate type for Product projection");
        }
        Long productId = Objects.requireNonNull(event.aggregateId(), "Product event aggregate ID is required");
        if (event.aggregateVersion() < 1) {
            throw new IllegalArgumentException("Product event must have an aggregate ID and positive version");
        }

        ProductProjectionCheckpoint checkpoint = checkpointRepository.findById(productId).orElse(null);
        if (checkpoint != null && event.aggregateVersion() <= checkpoint.getAggregateVersion()) {
            return false;
        }

        if ("ProductDeletedEvent".equals(event.eventType())) {
            productViewRepository.deleteById(productId);
        } else if ("ProductCreatedEvent".equals(event.eventType())
                || "ProductUpdatedEvent".equals(event.eventType())) {
            productViewRepository.save(Objects.requireNonNull(toProductView(event)));
        } else {
            throw new IllegalArgumentException("Unsupported Product event type: " + event.eventType());
        }

        checkpointRepository.save(new ProductProjectionCheckpoint(productId, event.aggregateVersion()));
        return true;
    }

    private ProductView toProductView(DomainEventMessage event) {
        JsonNode payload = objectMapper.valueToTree(event.payload());
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Product event payload must be a JSON object");
        }
        JsonNode name = payload.get("name");
        JsonNode category = payload.get("category");
        JsonNode price = payload.get("price");
        JsonNode description = payload.get("description");
        if (name == null || !name.isTextual()
                || category == null || !category.isTextual()
                || price == null || !price.isNumber()) {
            throw new IllegalArgumentException("Product event payload is missing required projection fields");
        }

        ProductCategory productCategory;
        try {
            productCategory = ProductCategory.valueOf(category.asText());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Product event contains an unknown category", exception);
        }

        return new ProductView(event.aggregateId(), name.asText(), productCategory, price.doubleValue(),
                description != null && !description.isNull() ? description.asText() : null,
                event.aggregateVersion());
    }
}
