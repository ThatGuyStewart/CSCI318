package com.retail.customer.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_product_events")
public class ProcessedProductEvent {

    @Id
    private UUID eventId;

    protected ProcessedProductEvent() {
    }

    public ProcessedProductEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}