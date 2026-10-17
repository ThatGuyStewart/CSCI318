package com.retail.common;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.streams.KafkaStreamsMicrometerListener;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, StreamsBuilderFactoryBean.class})
@ConditionalOnProperty(name = "retail.events.streams.enabled", havingValue = "true", matchIfMissing = true)
public class StreamingObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kafkaStreamsHealthIndicator")
    KafkaStreamsHealthIndicator kafkaStreamsHealthIndicator(
            ObjectProvider<StreamsBuilderFactoryBean> streamsFactoryProvider) {
        StreamsBuilderFactoryBean streamsFactory = streamsFactoryProvider.getIfAvailable();
        if (streamsFactory == null) {
            throw new IllegalStateException("Kafka Streams is enabled but no StreamsBuilderFactoryBean is configured");
        }
        return new KafkaStreamsHealthIndicator(streamsFactory);
    }

    @Bean
    StreamsBuilderFactoryBeanConfigurer kafkaStreamsMetricsConfigurer(MeterRegistry meterRegistry) {
        return streamsFactory -> streamsFactory
                .addListener(new KafkaStreamsMicrometerListener(meterRegistry));
    }

    @Bean
    @ConditionalOnBean(OutboxEventRepository.class)
    @ConditionalOnProperty(name = "retail.events.outbox.enabled", havingValue = "true", matchIfMissing = true)
    MeterBinder pendingOutboxEventsMetric(OutboxEventRepository outboxEventRepository) {
        return registry -> Gauge.builder("retail.events.outbox.pending", outboxEventRepository,
                OutboxEventRepository::countByPublishedFalse)
                .description("Number of un-published transactional outbox events")
                .register(registry);
    }
}
