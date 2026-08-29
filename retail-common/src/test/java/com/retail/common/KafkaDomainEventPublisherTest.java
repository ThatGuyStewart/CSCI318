package com.retail.common;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, DomainEventMessage> kafkaTemplate;

    @Test
    void partitionsEventsByAggregateTypeAndId() {
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Customer", 42L, 3,
                "CustomerUpdatedEvent", Instant.now(), null, null, Map.of());
        when(kafkaTemplate.send("customer.events", "Customer:42", event))
                .thenReturn(CompletableFuture.completedFuture(null));

        new KafkaDomainEventPublisher(kafkaTemplate).publish("customer.events", event);

        verify(kafkaTemplate).send("customer.events", "Customer:42", event);
    }

    @Test
    void publishesOnlyAfterTransactionCommit() {
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Customer", 42L, 3,
                "CustomerUpdatedEvent", Instant.now(), null, null, Map.of());
        when(kafkaTemplate.send("customer.events", "Customer:42", event))
                .thenReturn(CompletableFuture.completedFuture(null));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            new KafkaDomainEventPublisher(kafkaTemplate).publish("customer.events", event);

            org.mockito.Mockito.verifyNoInteractions(kafkaTemplate);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(kafkaTemplate).send("customer.events", "Customer:42", event);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}