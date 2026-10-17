package com.retail.common;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

public class KafkaStreamsHealthIndicator implements HealthIndicator {

    private final StreamsBuilderFactoryBean streamsFactory;

    public KafkaStreamsHealthIndicator(StreamsBuilderFactoryBean streamsFactory) {
        this.streamsFactory = streamsFactory;
    }

    @Override
    public Health health() {
        KafkaStreams streams = streamsFactory.getKafkaStreams();
        if (streams == null) {
            return Health.down().withDetail("state", "NOT_STARTED").build();
        }

        KafkaStreams.State state = streams.state();
        if (state == KafkaStreams.State.RUNNING) {
            return Health.up().withDetail("state", state.name()).build();
        }
        return Health.down().withDetail("state", state.name()).build();
    }
}
