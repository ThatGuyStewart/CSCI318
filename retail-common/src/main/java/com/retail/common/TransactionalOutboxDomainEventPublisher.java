package com.retail.common;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class TransactionalOutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    TransactionalOutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, DomainEventMessage event) {
        String resolvedTopic = Objects.requireNonNull(topic, "Event topic is required");
        DomainEventMessage resolvedEvent = Objects.requireNonNull(event, "Domain event is required");
        String aggregateType = Objects.requireNonNull(resolvedEvent.aggregateType(), "Aggregate type is required");
        Long aggregateId = Objects.requireNonNull(resolvedEvent.aggregateId(), "Aggregate ID is required");
        try {
            outboxEventRepository.save(new OutboxEvent(resolvedEvent.eventId(), resolvedTopic,
                    aggregateType + ":" + aggregateId, objectMapper.writeValueAsString(resolvedEvent)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize domain event for the outbox", exception);
        }
    }
}