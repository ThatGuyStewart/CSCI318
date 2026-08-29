package com.retail.notification.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_views")
public class NotificationView {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sent;

    public NotificationView() {
    }

    public NotificationView(Long id, Long customerId, DeliveryType type, String message, LocalDateTime sent) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.message = message;
        this.sent = sent;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public DeliveryType getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getSent() { return sent; }
}