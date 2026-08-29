package com.retail.customer.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_order_events")
public class ProcessedOrderEvent {

    @Id
    private UUID eventId;

    protected ProcessedOrderEvent() {
    }

    public ProcessedOrderEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}