package com.retail.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
public class DomainEventPublisherAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "retail.events.publishing.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "retail.events.outbox.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher kafkaDomainEventPublisher(KafkaTemplate<String, DomainEventMessage> kafkaTemplate) {
        return new KafkaDomainEventPublisher(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "retail.events.publishing.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher noOpDomainEventPublisher() {
        return (topic, event) -> { };
    }
}