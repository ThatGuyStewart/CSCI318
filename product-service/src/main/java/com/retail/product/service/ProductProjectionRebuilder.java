package com.retail.product.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.product.domain.ProductDomainEvent;
import com.retail.product.repository.ProductDomainEventRepository;
import com.retail.product.repository.ProductProjectionCheckpointRepository;
import com.retail.product.repository.ProductViewRepository;

@Service
public class ProductProjectionRebuilder {

    private static final String PRODUCT_AGGREGATE_TYPE = "Product";

    private final ProductDomainEventRepository eventRepository;
    private final ProductViewRepository productViewRepository;
    private final ProductProjectionCheckpointRepository checkpointRepository;
    private final ProductProjectionService projectionService;
    private final ObjectMapper objectMapper;

    public ProductProjectionRebuilder(ProductDomainEventRepository eventRepository,
            ProductViewRepository productViewRepository,
            ProductProjectionCheckpointRepository checkpointRepository,
            ProductProjectionService projectionService, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.productViewRepository = productViewRepository;
        this.checkpointRepository = checkpointRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int rebuild() {
        List<ProductDomainEvent> events = eventRepository
                .findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(PRODUCT_AGGREGATE_TYPE);
        productViewRepository.deleteAllInBatch();
        checkpointRepository.deleteAllInBatch();

        for (ProductDomainEvent event : events) {
            projectionService.apply(toMessage(event));
        }
        return events.size();
    }

    private DomainEventMessage toMessage(ProductDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
            return new DomainEventMessage(event.getEventId(), event.getAggregateType(), event.getAggregateId(),
                    event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(),
                    event.getCorrelationId(), event.getCausationId(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize persisted Product event " + event.getEventId(),
                    exception);
        }
    }
}
