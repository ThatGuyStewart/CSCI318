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
        Map<String, Object> payload) {
}