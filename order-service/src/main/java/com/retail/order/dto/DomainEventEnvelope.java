package com.retail.order.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DomainEventEnvelope {
    private UUID eventId;
    private String aggregateType;
    private String eventType;
    private Long aggregateId;
    private long aggregateVersion;
    private Instant occurredAt;
    private UUID correlationId;
    private UUID causationId;
    private Map<String, Object> payload;

    public DomainEventEnvelope() {
        // Required by Jackson when deserializing an event response.
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public long getAggregateVersion() { return aggregateVersion; }
    public void setAggregateVersion(long aggregateVersion) { this.aggregateVersion = aggregateVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public UUID getCausationId() { return causationId; }
    public void setCausationId(UUID causationId) { this.causationId = causationId; }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

}
