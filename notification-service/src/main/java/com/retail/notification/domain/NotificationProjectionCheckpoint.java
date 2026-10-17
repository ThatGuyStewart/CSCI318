package com.retail.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_projection_checkpoints")
public class NotificationProjectionCheckpoint {

    @Id
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(nullable = false)
    private long aggregateVersion;

    protected NotificationProjectionCheckpoint() {
    }

    public NotificationProjectionCheckpoint(Long notificationId, long aggregateVersion) {
        this.notificationId = notificationId;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }
}
