package com.retail.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ProjectionFailureRouterEmbeddedKafkaTest.EmbeddedKafkaTestConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = {"projection-router-input", "projection-router-dlt",
        "projection-router-deserialization-dlt"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class ProjectionFailureRouterEmbeddedKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void routesInvalidEventsAndContinuesProcessingAgainstKafkaBroker() throws Exception {
        String bootstrapServers = embeddedKafka.getBrokersAsString();
        AtomicInteger projectionCalls = new AtomicInteger();
        StreamsBuilder builder = new StreamsBuilder();
        JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class);
        eventSerde.deserializer().addTrustedPackages("com.retail.common");
        eventSerde.deserializer().ignoreTypeHeaders();
        ProjectionFailureRouter.route(builder.stream("projection-router-input",
                Consumed.with(Serdes.String(), eventSerde)), event -> {
                    projectionCalls.incrementAndGet();
                    if ("invalid".equals(event.eventType())) {
                        throw new InvalidProjectionEventException("Invalid event payload");
                    }
                }, "projection-router-dlt");

        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG,
                "projection-router-integration-" + UUID.randomUUID());
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsProperties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                ProjectionDeserializationExceptionHandler.class);
        streamsProperties.put(ProjectionDeserializationExceptionHandler.DLT_TOPIC_CONFIG,
                "projection-router-deserialization-dlt");
        KafkaStreams streams = new KafkaStreams(builder.build(), streamsProperties);

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProperties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "projection-router-test-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProperties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ProjectionFailure.class);
        consumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.retail.common");
        consumerProperties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        Properties rawProducerProperties = new Properties();
        rawProducerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        rawProducerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        rawProducerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        Properties deserializationConsumerProperties = new Properties();
        deserializationConsumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        deserializationConsumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG,
                "projection-router-deserialization-test-" + UUID.randomUUID());
        deserializationConsumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        deserializationConsumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        deserializationConsumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        deserializationConsumerProperties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DeserializationFailure.class);
        deserializationConsumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.retail.common");
        deserializationConsumerProperties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        try (KafkaProducer<String, DomainEventMessage> producer = new KafkaProducer<>(producerProperties);
                KafkaProducer<String, byte[]> rawProducer = new KafkaProducer<>(rawProducerProperties);
                KafkaConsumer<String, ProjectionFailure> consumer = new KafkaConsumer<>(consumerProperties);
                KafkaConsumer<String, DeserializationFailure> deserializationConsumer =
                        new KafkaConsumer<>(deserializationConsumerProperties)) {
            consumer.subscribe(java.util.List.of("projection-router-dlt"));
            consumer.poll(Duration.ofMillis(100));
            deserializationConsumer.subscribe(java.util.List.of("projection-router-deserialization-dlt"));
            deserializationConsumer.poll(Duration.ofMillis(100));
            streams.start();

            DomainEventMessage invalid = event("invalid");
            DomainEventMessage valid = event("valid");
            byte[] malformedValue = "{not-json".getBytes(StandardCharsets.UTF_8);
            rawProducer.send(new ProducerRecord<>("projection-router-input", "Customer:bad", malformedValue))
                    .get(10, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>("projection-router-input", "Customer:1", invalid))
                    .get(10, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>("projection-router-input", "Customer:2", valid))
                    .get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, ProjectionFailure> failureRecord =
                    pollFirstRecord(consumer, Duration.ofSeconds(15));
            ProjectionFailure failure = failureRecord == null ? null : failureRecord.value();
            DeserializationFailure deserializationFailure =
                    pollFirstValue(deserializationConsumer, Duration.ofSeconds(15));

            assertNotNull(failure, "Expected the invalid event to reach the DLT");
            assertNull(failureRecord.headers().lastHeader("__TypeId__"),
                    "Projection DLT records should not require Spring Java type headers");
            assertEquals(invalid.eventId(), failure.event().eventId());
            assertEquals(InvalidProjectionEventException.class.getName(), failure.errorType());
            assertEquals("Invalid event payload", failure.errorMessage());
            assertNotNull(failure.failedAt());
            assertNotNull(deserializationFailure, "Expected the malformed bytes to reach the deserialization DLT");
            assertEquals("projection-router-input", deserializationFailure.sourceTopic());
            assertEquals(0, deserializationFailure.sourcePartition());
            assertTrue(deserializationFailure.sourceOffset() >= 0);
            assertEquals("Customer:bad", new String(
                    java.util.Base64.getDecoder().decode(deserializationFailure.keyBase64()), StandardCharsets.UTF_8));
            assertArrayEquals(malformedValue,
                    java.util.Base64.getDecoder().decode(deserializationFailure.valueBase64()));
            assertNotNull(deserializationFailure.failedAt());
            assertTrue(awaitProjectionCalls(projectionCalls, 2, Duration.ofSeconds(5)),
                    "Expected valid and invalid events to reach the projector");
        } finally {
            streams.close();
            eventSerde.close();
        }
    }

    private <T> T pollFirstValue(KafkaConsumer<String, T> consumer, Duration timeout) {
        ConsumerRecord<String, T> record = pollFirstRecord(consumer, timeout);
        return record == null ? null : record.value();
    }

    private <T> ConsumerRecord<String, T> pollFirstRecord(KafkaConsumer<String, T> consumer, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            var records = consumer.poll(Duration.ofMillis(250));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        return null;
    }

    private boolean awaitProjectionCalls(AtomicInteger projectionCalls, int expected, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (projectionCalls.get() == expected) {
                return true;
            }
            Thread.sleep(25);
        }
        return projectionCalls.get() == expected;
    }

    private DomainEventMessage event(String eventType) {
        return new DomainEventMessage(UUID.randomUUID(), "Customer", 1L, 1, eventType,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, Map.of("customerId", 1L));
    }

    @org.springframework.context.annotation.Configuration
    static class EmbeddedKafkaTestConfiguration {
    }
}
