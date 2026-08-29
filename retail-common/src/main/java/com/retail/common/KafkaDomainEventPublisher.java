package com.retail.common;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final KafkaTemplate<String, DomainEventMessage> kafkaTemplate;

    KafkaDomainEventPublisher(KafkaTemplate<String, DomainEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @SuppressWarnings("null")
    public void publish(String topic, DomainEventMessage event) {
        String resolvedTopic = Objects.requireNonNull(topic, "Event topic is required");
        DomainEventMessage resolvedEvent = Objects.requireNonNull(event, "Domain event is required");
        String aggregateType = Objects.requireNonNull(resolvedEvent.aggregateType(), "Aggregate type is required");
        Long aggregateId = Objects.requireNonNull(resolvedEvent.aggregateId(), "Aggregate ID is required");
        String eventKey = aggregateType + ":" + aggregateId;
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(resolvedTopic, eventKey, resolvedEvent);
                }
            });
            return;
        }
        send(resolvedTopic, eventKey, resolvedEvent);
    }

    private void send(String topic, String eventKey, DomainEventMessage event) {
        try {
            kafkaTemplate.send(topic, eventKey, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            LOGGER.error("Failed to publish domain event {} to topic {}", event.eventId(), topic,
                                    exception);
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to queue domain event {} for topic {}", event.eventId(), topic, exception);
        }
    }
}