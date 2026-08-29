package com.retail.notification.consumer;

import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.retail.common.DomainEventMessage;
import com.retail.notification.domain.ProcessedOrderEvent;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.repository.ProcessedOrderEventRepository;
import com.retail.notification.service.NotificationService;

@Component
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class OrderEventConsumer {

    private static final Set<String> NOTIFIABLE_EVENT_TYPES = Set.of(
            "OrderPlacedEvent", "OrderCancelledEvent", "OrderStatusChangedEvent");
    private static final String ORDER_MESSAGE_PREFIX = "Your order #";

    private final ProcessedOrderEventRepository processedOrderEventRepository;
    private final NotificationService notificationService;

    public OrderEventConsumer(ProcessedOrderEventRepository processedOrderEventRepository,
            NotificationService notificationService) {
        this.processedOrderEventRepository = processedOrderEventRepository;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${retail.events.order-topic}", groupId = "${retail.events.consumer.group-id}")
    @Transactional
    public void onOrderEvent(DomainEventMessage event) {
        if (!NOTIFIABLE_EVENT_TYPES.contains(event.eventType())
                || processedOrderEventRepository.existsById(event.eventId())) {
            return;
        }

        Long customerId = asLong(event.payload().get("customerId"));
        Long orderId = asLong(event.payload().get("orderId"));
        if (customerId == null || orderId == null) {
            return;
        }

        processedOrderEventRepository.save(new ProcessedOrderEvent(event.eventId()));
        notificationService.createNotificationForCustomerId(customerId,
                new NotificationCreateRequest(messageFor(event.eventType(), orderId, event.payload().get("status"))));
    }

    private String messageFor(String eventType, Long orderId, Object status) {
        return switch (eventType) {
            case "OrderPlacedEvent" -> ORDER_MESSAGE_PREFIX + orderId + " has been placed.";
            case "OrderCancelledEvent" -> ORDER_MESSAGE_PREFIX + orderId + " has been cancelled.";
            case "OrderStatusChangedEvent" -> ORDER_MESSAGE_PREFIX + orderId + " status has changed to " + status + ".";
            default -> throw new IllegalArgumentException("Unsupported order event type: " + eventType);
        };
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}