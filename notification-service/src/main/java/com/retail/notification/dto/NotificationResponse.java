package com.retail.notification.dto;

import com.retail.notification.domain.DeliveryType;
import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private Long customerId;
    private DeliveryType type;
    private String message;
    private LocalDateTime sent;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, Long customerId, DeliveryType type, String message, LocalDateTime sent) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.sent = sent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getSent() {
        return sent;
    }

    public void setSent(LocalDateTime sent) {
        this.sent = sent;
    }
}
