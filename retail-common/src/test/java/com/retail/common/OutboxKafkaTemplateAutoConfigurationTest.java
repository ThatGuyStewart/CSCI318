package com.retail.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

class OutboxKafkaTemplateAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxKafkaTemplateAutoConfiguration.class))
            .withPropertyValues(
                    "retail.events.publishing.enabled=true",
                    "retail.events.outbox.enabled=true",
                    "spring.kafka.bootstrap-servers=localhost:9092",
                    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer");

    @Test
    void createsConfiguredTemplateWhenBootKafkaAutoConfigurationIsUnavailable() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(KafkaTemplate.class)
                .hasBean("outboxKafkaTemplate"));
    }

    @Test
    void providesDedicatedOutboxTemplateEvenWhenSpringBootProvidesKafkaTemplate() {
        contextRunner.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .hasBean("outboxKafkaTemplate")
                        .hasBean("kafkaTemplate"));
    }
}
