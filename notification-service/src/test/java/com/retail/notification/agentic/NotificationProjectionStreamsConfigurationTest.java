package com.retail.notification.agentic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.retail.common.DomainEventMessage;
import com.retail.notification.service.NotificationProjectionService;

class NotificationProjectionStreamsConfigurationTest {

    @Test
    void notificationTopicEventsAreAppliedToProjectionService() {
        NotificationProjectionService projectionService = mock(NotificationProjectionService.class);
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        new NotificationProjectionStreamsConfiguration().notificationProjectionStream(streamsBuilder, projectionService,
                "notification.events", "notification.projection.dlt");

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "notification-projection-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver testDriver = new TopologyTestDriver(streamsBuilder.build(), properties);
                JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class)) {
            eventSerde.deserializer().addTrustedPackages("com.retail.common");
            eventSerde.deserializer().ignoreTypeHeaders();
            TestInputTopic<String, DomainEventMessage> inputTopic = testDriver.createInputTopic(
                    "notification.events", Serdes.String().serializer(), eventSerde.serializer());
            DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Notification", 12L, 1,
                    "NotificationCreatedEvent", Instant.parse("2026-01-01T00:00:00Z"), null, null,
                    Map.of("notificationId", 12L, "customerId", 20L, "type", "Email",
                            "message", "Order placed", "sentAt", "2026-01-01T00:00:00"));

            inputTopic.pipeInput("Notification:12", event);

            ArgumentCaptor<DomainEventMessage> capturedEvent = ArgumentCaptor.forClass(DomainEventMessage.class);
            verify(projectionService).apply(capturedEvent.capture());
            assertEquals(event.eventId(), capturedEvent.getValue().eventId());
            assertEquals(event.aggregateVersion(), capturedEvent.getValue().aggregateVersion());
            assertEquals("Order placed", capturedEvent.getValue().payload().get("message"));
        }
    }
}
