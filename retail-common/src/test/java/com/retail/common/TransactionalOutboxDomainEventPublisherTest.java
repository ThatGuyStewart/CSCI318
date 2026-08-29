package com.retail.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransactionalOutboxDomainEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void enqueuesEventWithAggregateKey() {
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
                "ProductUpdatedEvent", Instant.now(), null, null, Map.of("name", "Updated product"));

        new TransactionalOutboxDomainEventPublisher(outboxEventRepository, new ObjectMapper().findAndRegisterModules())
                .publish("product.events", event);

        ArgumentCaptor<OutboxEvent> outboxEvent = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxEvent.capture());
        assertThat(outboxEvent.getValue().getTopic()).isEqualTo("product.events");
        assertThat(outboxEvent.getValue().getEventKey()).isEqualTo("Product:7");
        assertThat(outboxEvent.getValue().isPublished()).isFalse();
        assertThat(outboxEvent.getValue().getAttempts()).isZero();
        assertThat(outboxEvent.getValue().getPayload()).contains("ProductUpdatedEvent");
    }
}