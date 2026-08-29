package com.retail.product.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_domain_events", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
    name = "uk_product_domain_event_aggregate_version",
    columnNames = {"aggregate_type", "aggregate_id", "aggregate_version"}), indexes = @jakarta.persistence.Index(
    name = "idx_product_domain_event_category_occurred_at", columnList = "event_category, occurred_at"))
public class ProductDomainEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "event_category")
    private ProductCategory eventCategory;

    @Column(nullable = false)
    private long aggregateVersion;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    private UUID correlationId;
    private UUID causationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    protected ProductDomainEvent() {
    }

    public ProductDomainEvent(UUID eventId, String aggregateType, Long aggregateId, ProductCategory eventCategory,
            long aggregateVersion,
            String eventType, Instant occurredAt, UUID correlationId, UUID causationId, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventCategory = eventCategory;
        this.aggregateVersion = aggregateVersion;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }

    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public ProductCategory getEventCategory() { return eventCategory; }
    public long getAggregateVersion() { return aggregateVersion; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getCausationId() { return causationId; }
    public String getPayload() { return payload; }
}