package com.retail.product.agentic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.StreamsBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.retail.common.DomainEventMessage;
import com.retail.product.service.ProductProjectionService;

class ProductProjectionStreamsConfigurationTest {

    @Test
    void productTopicEventsAreAppliedToProjectionService() {
        ProductProjectionService projectionService = mock(ProductProjectionService.class);
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        new ProductProjectionStreamsConfiguration().productProjectionStream(streamsBuilder, projectionService,
                "product.events", "product.projection.dlt");

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "product-projection-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver testDriver = new TopologyTestDriver(streamsBuilder.build(), properties)) {
            JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class);
            eventSerde.deserializer().addTrustedPackages("com.retail.common");
            eventSerde.deserializer().ignoreTypeHeaders();
            TestInputTopic<String, DomainEventMessage> inputTopic = testDriver.createInputTopic("product.events",
                    Serdes.String().serializer(), eventSerde.serializer());
            DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 12L, 3,
                    "ProductUpdatedEvent", Instant.parse("2026-01-01T00:00:00Z"), null, null,
                    Map.of("productId", 12L, "name", "Updated Product", "category", "Tools", "price", 15.0));

            inputTopic.pipeInput("Product:12", event);

            ArgumentCaptor<DomainEventMessage> capturedEvent = ArgumentCaptor.forClass(DomainEventMessage.class);
            verify(projectionService).apply(capturedEvent.capture());
            assertEquals(event.eventId(), capturedEvent.getValue().eventId());
            assertEquals(event.aggregateVersion(), capturedEvent.getValue().aggregateVersion());
            assertEquals("Updated Product", capturedEvent.getValue().payload().get("name"));
            assertEquals("Tools", capturedEvent.getValue().payload().get("category"));
        }
    }
}
