package com.retail.common;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@AutoConfiguration
@ConditionalOnClass({CommonErrorHandler.class, KafkaTemplate.class})
@ConditionalOnProperty(name = "retail.events.consumer.enabled", havingValue = "true")
@SuppressWarnings("null")
public class KafkaListenerDeadLetterAutoConfiguration {

    @Bean
    KafkaTemplate<String, Object> consumerDeadLetterKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();
        Serializer<Object> valueSerializer = (topic, value) -> {
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof String string) {
                return string.getBytes(StandardCharsets.UTF_8);
            }
            return jsonSerializer.serialize(topic, value);
        };
        var producerFactory = new DefaultKafkaProducerFactory<String, Object>(producerProperties,
                new StringSerializer(), valueSerializer);
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    CommonErrorHandler kafkaListenerCommonErrorHandler(KafkaTemplate<String, Object> consumerDeadLetterKafkaTemplate,
            @Value("${retail.events.consumer.dlt-topic}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                consumerDeadLetterKafkaTemplate, (record, exception) -> new TopicPartition(deadLetterTopic, -1));
        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1_000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        errorHandler.addNotRetryableExceptions(UnsupportedEventSchemaVersionException.class);
        return errorHandler;
    }
}
