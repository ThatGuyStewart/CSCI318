package com.retail.customer.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.domain.Customer;
import com.retail.customer.domain.CustomerDomainEvent;
import com.retail.customer.repository.CustomerDomainEventRepository;

@Service
public class CustomerEventRecorder {

    private final CustomerDomainEventRepository eventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final CustomerProjectionService projectionService;
    private final ObjectMapper objectMapper;
    private final boolean streamsEnabled;
    private final String eventTopic;

    public CustomerEventRecorder(CustomerDomainEventRepository eventRepository,
            DomainEventPublisher domainEventPublisher, CustomerProjectionService projectionService,
            ObjectMapper objectMapper,
            @Value("${retail.events.streams.enabled:true}") boolean streamsEnabled,
            @Value("${retail.events.topic}") String eventTopic) {
        this.eventRepository = eventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
        this.streamsEnabled = streamsEnabled;
        this.eventTopic = eventTopic;
    }

    public DomainEventMessage record(Customer customer, String eventType, Map<String, Object> payload,
            UUID causationId) {
        try {
            CustomerDomainEvent event = new CustomerDomainEvent(UUID.randomUUID(), "Customer",
                    customer.getCustomerId(), customer.getEmail(), customer.getAggregateVersion(), eventType,
                    Instant.now(), null, causationId, objectMapper.writeValueAsString(payload));
            DomainEventMessage message = new DomainEventMessage(event.getEventId(), event.getAggregateType(),
                    event.getAggregateId(), event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(),
                    event.getCorrelationId(), event.getCausationId(), payload);
            eventRepository.save(event);
            domainEventPublisher.publish(eventTopic, message);
            if (!streamsEnabled) {
                projectionService.apply(message);
            }
            return message;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize customer event payload", exception);
        }
    }
}
