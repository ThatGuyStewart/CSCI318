package com.retail.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

class ProjectionFailureRouterTest {

    @Test
    void routesInvalidProjectionEventsToDeadLetterTopicAndContinuesWithLaterEvents() {
        StreamsBuilder builder = new StreamsBuilder();
        AtomicInteger projectionCalls = new AtomicInteger();
        JsonSerde<DomainEventMessage> sourceSerde = new JsonSerde<>(DomainEventMessage.class);
        sourceSerde.deserializer().addTrustedPackages("com.retail.common");
        sourceSerde.deserializer().ignoreTypeHeaders();
        ProjectionFailureRouter.route(builder.stream("input.events",
                Consumed.with(Serdes.String(), sourceSerde)), event -> {
            projectionCalls.incrementAndGet();
            if ("invalid".equals(event.eventType())) {
                throw new InvalidProjectionEventException("Invalid event payload");
            }
        }, "projection.dlt");

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "projection-failure-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), properties);
                JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class);
                JsonSerde<ProjectionFailure> failureSerde = new JsonSerde<>(ProjectionFailure.class)) {
            eventSerde.deserializer().addTrustedPackages("com.retail.common");
            eventSerde.deserializer().ignoreTypeHeaders();
            failureSerde.deserializer().addTrustedPackages("com.retail.common");
            failureSerde.deserializer().ignoreTypeHeaders();
            TestInputTopic<String, DomainEventMessage> input = driver.createInputTopic("input.events",
                    Serdes.String().serializer(), eventSerde.serializer());
            TestOutputTopic<String, ProjectionFailure> deadLetters = driver.createOutputTopic("projection.dlt",
                    Serdes.String().deserializer(), failureSerde.deserializer());

            DomainEventMessage invalid = event("invalid");
            DomainEventMessage unsupportedSchema = event("valid", 2);
            DomainEventMessage valid = event("valid");
            input.pipeInput("Customer:1", invalid);
            input.pipeInput("Customer:2", valid);
            input.pipeInput("Customer:3", unsupportedSchema);

            assertEquals(2, deadLetters.getQueueSize());
            ProjectionFailure failure = deadLetters.readValue();
            assertEquals(invalid.eventId(), failure.event().eventId());
            assertEquals(InvalidProjectionEventException.class.getName(), failure.errorType());
            assertEquals("Invalid event payload", failure.errorMessage());
            assertNotNull(failure.failedAt());
            ProjectionFailure schemaFailure = deadLetters.readValue();
            assertEquals(unsupportedSchema.eventId(), schemaFailure.event().eventId());
            assertEquals("Unsupported event schema version: 2", schemaFailure.errorMessage());
            assertEquals(0, deadLetters.getQueueSize());
            assertEquals(2, projectionCalls.get());
        }
    }

    @Test
    void propagatesUnexpectedProjectionFailuresForKafkaStreamsRetry() {
        StreamsBuilder builder = new StreamsBuilder();
        RuntimeException databaseFailure = new RuntimeException("database unavailable");
        JsonSerde<DomainEventMessage> sourceSerde = new JsonSerde<>(DomainEventMessage.class);
        sourceSerde.deserializer().addTrustedPackages("com.retail.common");
        sourceSerde.deserializer().ignoreTypeHeaders();
        ProjectionFailureRouter.route(builder.stream("input.events",
                Consumed.with(Serdes.String(), sourceSerde)), event -> {
            throw databaseFailure;
        }, "projection.dlt");

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "projection-transient-failure-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), properties);
                JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class)) {
            eventSerde.deserializer().addTrustedPackages("com.retail.common");
            eventSerde.deserializer().ignoreTypeHeaders();
            TestInputTopic<String, DomainEventMessage> input = driver.createInputTopic("input.events",
                    Serdes.String().serializer(), eventSerde.serializer());

            RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> input.pipeInput("Customer:1", event("valid")));
            assertEquals(databaseFailure, thrown.getCause());
        }
    }

    private DomainEventMessage event(String eventType) {
        return new DomainEventMessage(UUID.randomUUID(), "Customer", 1L, 1, eventType,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, Map.of("customerId", 1L));
    }

    private DomainEventMessage event(String eventType, int schemaVersion) {
        return new DomainEventMessage(UUID.randomUUID(), "Customer", 1L, 1, eventType,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, Map.of("customerId", 1L), schemaVersion);
    }
}
