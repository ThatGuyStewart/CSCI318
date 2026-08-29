package com.retail.common;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class OutboxEventDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, DomainEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    OutboxEventDispatcher(OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, DomainEventMessage> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${retail.events.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void dispatchPendingEvents() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                DomainEventMessage event = objectMapper.readValue(outboxEvent.getPayload(), DomainEventMessage.class);
                kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getEventKey(), event).get();
                outboxEvent.markPublished();
            } catch (JsonProcessingException | InterruptedException | ExecutionException | RuntimeException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                outboxEvent.recordFailedAttempt();
                LOGGER.error("Failed to publish outbox event to topic {} after {} attempt(s)", outboxEvent.getTopic(),
                        outboxEvent.getAttempts(), exception);
            }
        }
    }
}