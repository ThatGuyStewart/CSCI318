package com.retail.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DomainEventMessageTest {

    @Test
    void defaultsLegacyJsonWithoutSchemaVersionToVersionOne() throws Exception {
        String legacyJson = """
                {
                  "eventId": "49d1a55e-a31c-4e27-8fd5-77fc8355a2bd",
                  "aggregateType": "Customer",
                  "aggregateId": 42,
                  "aggregateVersion": 3,
                  "eventType": "CustomerUpdatedEvent",
                  "occurredAt": "2026-01-01T00:00:00Z",
                  "correlationId": null,
                  "causationId": null,
                  "payload": {"customerId": 42}
                }
                """;

        DomainEventMessage event = new ObjectMapper().findAndRegisterModules()
                .readValue(legacyJson, DomainEventMessage.class);

        assertEquals(DomainEventMessage.CURRENT_SCHEMA_VERSION, event.schemaVersion());
    }
}
