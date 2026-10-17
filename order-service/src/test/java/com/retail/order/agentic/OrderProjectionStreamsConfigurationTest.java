package com.retail.order.agentic;

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
import com.retail.order.service.OrderProjectionService;

class OrderProjectionStreamsConfigurationTest {

    @Test
    void orderTopicEventsAreAppliedToProjectionService() {
        OrderProjectionService projectionService = mock(OrderProjectionService.class);
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        new OrderProjectionStreamsConfiguration().orderProjectionStream(streamsBuilder, projectionService,
                "order.events", "order.projection.dlt");

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-projection-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver testDriver = new TopologyTestDriver(streamsBuilder.build(), properties);
                JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class)) {
            eventSerde.deserializer().addTrustedPackages("com.retail.common");
            eventSerde.deserializer().ignoreTypeHeaders();
            TestInputTopic<String, DomainEventMessage> inputTopic = testDriver.createInputTopic("order.events",
                    Serdes.String().serializer(), eventSerde.serializer());
            DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Order", 12L, 3,
                    "OrderStatusChangedEvent", Instant.parse("2026-01-01T00:00:00Z"), null, null,
                    Map.of("orderId", 12L, "status", "InTransit"));

            inputTopic.pipeInput("Order:12", event);

            ArgumentCaptor<DomainEventMessage> capturedEvent = ArgumentCaptor.forClass(DomainEventMessage.class);
            verify(projectionService).apply(capturedEvent.capture());
            assertEquals(event.eventId(), capturedEvent.getValue().eventId());
            assertEquals(event.aggregateVersion(), capturedEvent.getValue().aggregateVersion());
            assertEquals("InTransit", capturedEvent.getValue().payload().get("status"));
        }
    }
}
