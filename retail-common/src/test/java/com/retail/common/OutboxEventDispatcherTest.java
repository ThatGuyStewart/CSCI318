package com.retail.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventDispatcherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void marksEventPublishedAfterBrokerAcknowledgesIt() throws Exception {
        OutboxEvent outboxEvent = outboxEvent("Product:7");
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq("product.events"), eq("Product:7"), any(DomainEventMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper).dispatchPendingEvents();

        ArgumentCaptor<DomainEventMessage> eventCaptor = ArgumentCaptor.forClass(DomainEventMessage.class);
        verify(kafkaTemplate).send(eq("product.events"), eq("Product:7"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("ProductUpdatedEvent");
        assertThat(outboxEvent.isPublished()).isTrue();
        assertThat(outboxEvent.getAttempts()).isZero();
    }

    @Test
    void retainsEventAndIncrementsAttemptsWhenBrokerSendFails() throws Exception {
        OutboxEvent outboxEvent = outboxEvent("Product:7");
        CompletableFuture<SendResult<Object, Object>> failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq("product.events"), eq("Product:7"), any(DomainEventMessage.class)))
                .thenReturn(failedSend);

        new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper).dispatchPendingEvents();

        assertThat(outboxEvent.isPublished()).isFalse();
        assertThat(outboxEvent.getAttempts()).isEqualTo(1);
    }

    @Test
    void stopsBatchAndPreservesInterruptWhenDispatchIsInterrupted() throws Exception {
        OutboxEvent interruptedEvent = outboxEvent("Product:7");
        OutboxEvent laterEvent = outboxEvent("Product:8");
        CompletableFuture<SendResult<Object, Object>> interruptedSend = new CompletableFuture<>() {
            @Override
            public SendResult<Object, Object> get() throws InterruptedException {
                throw new InterruptedException("dispatch interrupted");
            }
        };
        when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(interruptedEvent, laterEvent));
        when(kafkaTemplate.send(eq("product.events"), eq("Product:7"), any(DomainEventMessage.class)))
                .thenReturn(interruptedSend);

        try {
            new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper).dispatchPendingEvents();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(interruptedEvent.getAttempts()).isEqualTo(1);
            assertThat(laterEvent.getAttempts()).isZero();
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(DomainEventMessage.class));
        } finally {
            Thread.interrupted();
        }
    }

    private OutboxEvent outboxEvent(String eventKey) throws Exception {
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
                "ProductUpdatedEvent", Instant.now(), null, null, Map.of("productId", 7L, "name", "Updated"));
        return new OutboxEvent(event.eventId(), "product.events", eventKey, objectMapper.writeValueAsString(event));
    }
}
