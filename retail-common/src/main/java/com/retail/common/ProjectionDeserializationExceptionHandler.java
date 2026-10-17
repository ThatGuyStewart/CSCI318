package com.retail.common;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler.DeserializationHandlerResponse;
import org.apache.kafka.streams.errors.ErrorHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ProjectionDeserializationExceptionHandler implements DeserializationExceptionHandler {

    public static final String DLT_TOPIC_CONFIG = "retail.events.deserialization.dlt-topic";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionDeserializationExceptionHandler.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private Properties producerProperties;
    private String deadLetterTopic;

    @Override
    public void configure(Map<String, ?> configs) {
        Object topicConfig = configs.get(DLT_TOPIC_CONFIG);
        Object bootstrapServers = configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (!(topicConfig instanceof String topic) || topic.isBlank()) {
            throw new IllegalArgumentException("A deserialization dead-letter topic must be configured");
        }
        if (bootstrapServers == null) {
            throw new IllegalArgumentException("Kafka bootstrap servers must be configured");
        }

        deadLetterTopic = topic;
        producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    }

    @Override
    public DeserializationHandlerResponse handle(ErrorHandlerContext context, ConsumerRecord<byte[], byte[]> record,
            Exception exception) {
        Objects.requireNonNull(record, "Failed Kafka record is required");
        if (producerProperties == null || deadLetterTopic == null) {
            throw new IllegalStateException("Deserialization dead-letter handler has not been configured");
        }

        try {
            DeserializationFailure failure = new DeserializationFailure(
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    encode(record.key()),
                    encode(record.value()),
                    exception.getClass().getName(),
                    "Kafka record value could not be deserialized",
                    Instant.now());
            byte[] serializedFailure = objectMapper.writeValueAsBytes(failure);
            try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<byte[], byte[]>(producerProperties)) {
                producer.send(new ProducerRecord<>(deadLetterTopic, record.key(), serializedFailure))
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            LOGGER.error("Malformed Kafka record from {} partition {} offset {} routed to {} ({})",
                    record.topic(), record.partition(), record.offset(), deadLetterTopic,
                    exception.getClass().getName());
            return DeserializationHandlerResponse.CONTINUE;
        } catch (JsonProcessingException | InterruptedException | ExecutionException
                | java.util.concurrent.TimeoutException | RuntimeException sendFailure) {
            if (sendFailure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Failed to route malformed Kafka record from {} partition {} offset {} to {}",
                    record.topic(), record.partition(), record.offset(), deadLetterTopic, sendFailure);
            return DeserializationHandlerResponse.FAIL;
        }
    }

    private String encode(byte[] value) {
        return value == null ? null : Base64.getEncoder().encodeToString(value);
    }
}
