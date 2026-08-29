package com.retail.notification.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.common.DomainEventMessage;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.repository.ProcessedOrderEventRepository;
import com.retail.notification.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderEventConsumerTest {

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void createsNotificationForAnOrderStatusChange() {
        DomainEventMessage event = orderEvent(UUID.randomUUID(), "OrderStatusChangedEvent");
        when(processedOrderEventRepository.existsById(event.eventId())).thenReturn(false);

        new OrderEventConsumer(processedOrderEventRepository, notificationService).onOrderEvent(event);

        ArgumentCaptor<NotificationCreateRequest> request = ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(processedOrderEventRepository).save(any());
        verify(notificationService).createNotificationForCustomerId(org.mockito.ArgumentMatchers.eq(3L), request.capture());
        assertThat(request.getValue().getMessage()).isEqualTo("Your order #42 status has changed to Shipped.");
    }

    @Test
    void ignoresAnAlreadyProcessedOrderEvent() {
        DomainEventMessage event = orderEvent(UUID.randomUUID(), "OrderPlacedEvent");
        when(processedOrderEventRepository.existsById(event.eventId())).thenReturn(true);

        new OrderEventConsumer(processedOrderEventRepository, notificationService).onOrderEvent(event);

        verify(processedOrderEventRepository, never()).save(any());
        verify(notificationService, never()).createNotificationForCustomerId(any(), any());
    }

    @Test
    void ignoresUnrelatedEventTypes() {
        DomainEventMessage event = orderEvent(UUID.randomUUID(), "OrderItemAddedEvent");

        new OrderEventConsumer(processedOrderEventRepository, notificationService).onOrderEvent(event);

        verify(processedOrderEventRepository, never()).existsById(any());
        verify(notificationService, never()).createNotificationForCustomerId(any(), any());
    }

    private DomainEventMessage orderEvent(UUID eventId, String eventType) {
        return new DomainEventMessage(eventId, "Order", 42L, 2, eventType, Instant.now(), null, null,
                Map.of("customerId", 3L, "orderId", 42L, "status", "Shipped"));
    }
}