package com.retail.common;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable message persisted by a service and published on its event topic.
 */
public record DomainEventMessage(
        UUID eventId,
        String aggregateType,
        Long aggregateId,
        long aggregateVersion,
        String eventType,
        Instant occurredAt,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payload,
        Integer schemaVersion) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public DomainEventMessage {
        if (schemaVersion == null) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
    }

    public DomainEventMessage(UUID eventId, String aggregateType, Long aggregateId, long aggregateVersion,
            String eventType, Instant occurredAt, UUID correlationId, UUID causationId, Map<String, Object> payload) {
        this(eventId, aggregateType, aggregateId, aggregateVersion, eventType, occurredAt, correlationId, causationId,
                payload, CURRENT_SCHEMA_VERSION);
    }

    public void requireCurrentSchemaVersion() {
        if (!Integer.valueOf(CURRENT_SCHEMA_VERSION).equals(schemaVersion)) {
            throw new UnsupportedEventSchemaVersionException(schemaVersion);
        }
    }
}