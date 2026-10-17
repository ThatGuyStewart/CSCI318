package com.retail.common;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@AutoConfigureAfter(KafkaAutoConfiguration.class)
@ConditionalOnClass({KafkaProperties.class, KafkaTemplate.class})
@ConditionalOnProperty(name = "retail.events.publishing.enabled", havingValue = "true")
@ConditionalOnProperty(name = "retail.events.outbox.enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
public class OutboxKafkaTemplateAutoConfiguration {

    @Bean
    KafkaTemplate<Object, Object> outboxKafkaTemplate(KafkaProperties kafkaProperties,
            ObjectProvider<SslBundles> sslBundles) {
        var producerFactory = new DefaultKafkaProducerFactory<Object, Object>(
                kafkaProperties.buildProducerProperties(sslBundles.getIfAvailable()));
        return new KafkaTemplate<>(producerFactory);
    }
}
