package com.retail.common;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
class OutboxEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int attempts;

    protected OutboxEvent() {
    }

    OutboxEvent(UUID eventId, String topic, String eventKey, String payload) {
        this.eventId = eventId;
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    String getTopic() { return topic; }
    String getEventKey() { return eventKey; }
    String getPayload() { return payload; }
    boolean isPublished() { return published; }
    int getAttempts() { return attempts; }

    void markPublished() {
        published = true;
    }

    void recordFailedAttempt() {
        attempts++;
    }
}