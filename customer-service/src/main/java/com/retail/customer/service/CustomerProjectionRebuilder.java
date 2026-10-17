package com.retail.customer.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.CustomerDomainEvent;
import com.retail.customer.domain.CustomerView;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerProjectionCheckpointRepository;
import com.retail.customer.repository.CustomerViewRepository;

@Service
public class CustomerProjectionRebuilder {

    private static final String CUSTOMER_AGGREGATE_TYPE = "Customer";

    private final CustomerDomainEventRepository eventRepository;
    private final CustomerViewRepository customerViewRepository;
    private final BasketRepository basketRepository;
    private final CustomerProjectionCheckpointRepository checkpointRepository;
    private final CustomerProjectionService projectionService;
    private final ObjectMapper objectMapper;

    public CustomerProjectionRebuilder(CustomerDomainEventRepository eventRepository,
            CustomerViewRepository customerViewRepository, BasketRepository basketRepository,
            CustomerProjectionCheckpointRepository checkpointRepository,
            CustomerProjectionService projectionService, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.customerViewRepository = customerViewRepository;
        this.basketRepository = basketRepository;
        this.checkpointRepository = checkpointRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int rebuild() {
        List<CustomerDomainEvent> events = eventRepository
                .findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(CUSTOMER_AGGREGATE_TYPE);
        customerViewRepository.deleteAllInBatch();
        checkpointRepository.deleteAllInBatch();
        for (CustomerDomainEvent event : events) {
            projectionService.apply(toMessage(event));
        }
        for (CustomerView view : customerViewRepository.findAll()) {
            basketRepository.findById(Objects.requireNonNull(view.getCustomerId()))
                    .ifPresent(basket -> view.setBasketTotal(basket.getTotal()));
        }
        return events.size();
    }

    private DomainEventMessage toMessage(CustomerDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
            return new DomainEventMessage(event.getEventId(), event.getAggregateType(), event.getAggregateId(),
                    event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(), event.getCorrelationId(),
                    event.getCausationId(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize persisted Customer event " + event.getEventId(),
                    exception);
        }
    }
}
