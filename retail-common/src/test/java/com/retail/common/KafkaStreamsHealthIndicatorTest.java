package com.retail.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

class KafkaStreamsHealthIndicatorTest {

    @Test
    void reportsUpOnlyWhenKafkaStreamsIsRunning() {
        StreamsBuilderFactoryBean streamsFactory = mock(StreamsBuilderFactoryBean.class);
        KafkaStreams streams = mock(KafkaStreams.class);
        when(streamsFactory.getKafkaStreams()).thenReturn(streams);
        when(streams.state()).thenReturn(KafkaStreams.State.RUNNING);

        var health = new KafkaStreamsHealthIndicator(streamsFactory).health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("RUNNING", health.getDetails().get("state"));
    }

    @Test
    void reportsDownBeforeKafkaStreamsStarts() {
        StreamsBuilderFactoryBean streamsFactory = mock(StreamsBuilderFactoryBean.class);
        when(streamsFactory.getKafkaStreams()).thenReturn(null);

        var health = new KafkaStreamsHealthIndicator(streamsFactory).health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("NOT_STARTED", health.getDetails().get("state"));
    }
}
