package com.retail.notification.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.notification.domain.DeliveryType;
import com.retail.notification.domain.NotificationProjectionCheckpoint;
import com.retail.notification.domain.NotificationView;
import com.retail.notification.repository.NotificationProjectionCheckpointRepository;
import com.retail.notification.repository.NotificationViewRepository;

@Service
public class NotificationProjectionService {

    private static final String NOTIFICATION_AGGREGATE_TYPE = "Notification";

    private final NotificationViewRepository viewRepository;
    private final NotificationProjectionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    public NotificationProjectionService(NotificationViewRepository viewRepository,
            NotificationProjectionCheckpointRepository checkpointRepository, ObjectMapper objectMapper) {
        this.viewRepository = viewRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean apply(DomainEventMessage event) {
        Objects.requireNonNull(event, "Notification event is required");
        if (!NOTIFICATION_AGGREGATE_TYPE.equals(event.aggregateType())) {
            throw new IllegalArgumentException("Unexpected aggregate type for Notification projection");
        }
        Long notificationId = Objects.requireNonNull(event.aggregateId(),
                "Notification event aggregate ID is required");
        if (event.aggregateVersion() < 1) {
            throw new IllegalArgumentException("Notification event must have a positive aggregate version");
        }

        NotificationProjectionCheckpoint checkpoint = checkpointRepository.findById(notificationId).orElse(null);
        if (checkpoint != null && event.aggregateVersion() <= checkpoint.getAggregateVersion()) {
            return false;
        }
        if (!"NotificationCreatedEvent".equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported Notification event type: " + event.eventType());
        }

        JsonNode payload = objectMapper.valueToTree(event.payload());
        if (payload == null || !payload.isObject()
                || !payload.path("notificationId").isIntegralNumber()
                || payload.path("notificationId").longValue() != notificationId
                || !payload.path("customerId").isIntegralNumber()
                || !payload.path("type").isTextual()
                || !payload.path("message").isTextual()
                || !payload.path("sentAt").isTextual()) {
            throw new IllegalArgumentException("Notification event payload is missing required projection fields");
        }

        DeliveryType deliveryType;
        try {
            deliveryType = DeliveryType.valueOf(payload.path("type").asText());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Notification event contains an unknown delivery type", exception);
        }

        LocalDateTime sent;
        try {
            sent = objectMapper.treeToValue(payload.path("sentAt"), LocalDateTime.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Notification event contains an invalid sentAt value", exception);
        }
        if (sent == null) {
            throw new IllegalArgumentException("Notification event is missing its sentAt value");
        }

        viewRepository.save(new NotificationView(notificationId, payload.path("customerId").longValue(), deliveryType,
                payload.path("message").asText(), sent));
        checkpointRepository.save(new NotificationProjectionCheckpoint(notificationId, event.aggregateVersion()));
        return true;
    }
}
