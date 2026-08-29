package com.retail.common;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnProperty(name = "retail.events.publishing.enabled", havingValue = "true")
@ConditionalOnProperty(name = "retail.events.outbox.enabled", havingValue = "true")
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan(basePackageClasses = OutboxEvent.class)
@EnableJpaRepositories(basePackageClasses = OutboxEventRepository.class)
@EnableScheduling
public class TransactionalOutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    DomainEventPublisher transactionalOutboxDomainEventPublisher(OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        return new TransactionalOutboxDomainEventPublisher(outboxEventRepository, objectMapper);
    }

    @Bean
    OutboxEventDispatcher outboxEventDispatcher(OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, DomainEventMessage> kafkaTemplate, ObjectMapper objectMapper) {
        return new OutboxEventDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
    }
}