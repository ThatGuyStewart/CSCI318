package com.retail.order.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.order.domain.OrderDomainEvent;
import com.retail.order.repository.OrderDomainEventRepository;
import com.retail.order.repository.OrderProjectionCheckpointRepository;
import com.retail.order.repository.OrderSummaryViewRepository;

@Service
public class OrderProjectionRebuilder {

    private static final String ORDER_AGGREGATE_TYPE = "Order";

    private final OrderDomainEventRepository eventRepository;
    private final OrderSummaryViewRepository summaryRepository;
    private final OrderProjectionCheckpointRepository checkpointRepository;
    private final OrderProjectionService projectionService;
    private final ObjectMapper objectMapper;

    public OrderProjectionRebuilder(OrderDomainEventRepository eventRepository,
            OrderSummaryViewRepository summaryRepository,
            OrderProjectionCheckpointRepository checkpointRepository,
            OrderProjectionService projectionService, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.summaryRepository = summaryRepository;
        this.checkpointRepository = checkpointRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int rebuild() {
        List<OrderDomainEvent> events = eventRepository
                .findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(ORDER_AGGREGATE_TYPE);
        summaryRepository.deleteAllInBatch();
        checkpointRepository.deleteAllInBatch();
        for (OrderDomainEvent event : events) {
            projectionService.apply(toMessage(event));
        }
        return events.size();
    }

    private DomainEventMessage toMessage(OrderDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
            return new DomainEventMessage(event.getEventId(), event.getAggregateType(), event.getAggregateId(),
                    event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(), event.getCorrelationId(),
                    event.getCausationId(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize persisted Order event " + event.getEventId(),
                    exception);
        }
    }
}
