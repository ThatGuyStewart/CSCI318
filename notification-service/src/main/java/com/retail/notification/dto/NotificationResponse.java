package com.retail.notification.dto;

import com.retail.notification.domain.DeliveryType;
import java.time.LocalDateTime;

public class NotificationResponse {
    private Long notificationId;
    private Long customerId;
    private DeliveryType type;
    private String message;
    private LocalDateTime sentAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long notificationId, Long customerId, DeliveryType type, String message, LocalDateTime sentAt) {
        this.notificationId = notificationId;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.sentAt = sentAt;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public DeliveryType getType() {
        return type;
    }

    public void setType(DeliveryType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
