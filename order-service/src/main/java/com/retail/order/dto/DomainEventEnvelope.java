package com.retail.order.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

public class DomainEventEnvelope {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private String eventType;
    private Long aggregateId;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;

    public DomainEventEnvelope() {
        this.timestamp = LocalDateTime.now(DEFAULT_ZONE);
    }

    public DomainEventEnvelope(String eventType, Long aggregateId, Map<String, Object> payload) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.timestamp = LocalDateTime.now(DEFAULT_ZONE);
    }

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

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
