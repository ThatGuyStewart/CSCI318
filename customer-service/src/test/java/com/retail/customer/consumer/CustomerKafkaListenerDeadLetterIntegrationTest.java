package com.retail.customer.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.customer.CustomerServiceApplication;

@SpringBootTest(classes = CustomerServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "retail.events.streams.enabled=false",
                "retail.events.consumer.enabled=true",
                "retail.events.consumer.group-id=customer-listener-dlt-test",
                "retail.events.consumer.dlt-topic=customer.consumer.dlt",
                "retail.events.product-topic=product.events",
                "retail.events.order-topic=order.events"
        })
@EmbeddedKafka(partitions = 1, topics = {"product.events", "order.events", "customer.consumer.dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerKafkaListenerDeadLetterIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Test
    void publishesMalformedAndUnsupportedSchemaRecordsToConsumerDeadLetterTopic() throws Exception {
        listenerRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, 1));

        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "customer-listener-dlt-reader-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        byte[] malformedJson = "{not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        DomainEventMessage unsupportedEvent = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
                "ProductUpdatedEvent", Instant.now(), null, null,
                Map.of("productId", 7L, "name", "New product", "price", 12.5), 2);
        byte[] unsupportedJson = objectMapper.writeValueAsBytes(unsupportedEvent);

        try (KafkaConsumer<String, byte[]> deadLetterConsumer = new KafkaConsumer<>(consumerProperties);
                KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties)) {
            deadLetterConsumer.subscribe(List.of("customer.consumer.dlt"));
            deadLetterConsumer.poll(Duration.ofMillis(100));

            producer.send(new ProducerRecord<>("product.events", "Product:bad", malformedJson))
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>("product.events", "Product:7", unsupportedJson))
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);

            List<ConsumerRecord<String, byte[]>> records = pollRecords(deadLetterConsumer, 2,
                    Duration.ofSeconds(20));

            assertEquals(2, records.size());
            assertTrue(records.stream().anyMatch(record -> java.util.Arrays.equals(record.value(), malformedJson)),
                    "Expected the original malformed bytes in the DLT");
            JsonNode unsupportedRecord = records.stream()
                    .filter(record -> !java.util.Arrays.equals(record.value(), malformedJson))
                    .map(record -> readJson(record.value()))
                    .filter(json -> json.path("schemaVersion").asInt() == 2)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected the unsupported schema record in the DLT"));
            assertEquals(unsupportedEvent.eventId().toString(), unsupportedRecord.path("eventId").asText());
        }
    }

    private List<ConsumerRecord<String, byte[]>> pollRecords(KafkaConsumer<String, byte[]> consumer, int expectedCount,
            Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<ConsumerRecord<String, byte[]>> records = new ArrayList<>();
        while (System.nanoTime() < deadline && records.size() < expectedCount) {
            consumer.poll(Duration.ofMillis(250)).forEach(records::add);
        }
        return records;
    }

    private JsonNode readJson(byte[] value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new AssertionError("Expected DLT record to contain JSON", exception);
        }
    }
}
