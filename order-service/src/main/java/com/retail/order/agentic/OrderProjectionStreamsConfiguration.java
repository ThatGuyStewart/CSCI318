package com.retail.order.agentic;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.retail.common.DomainEventMessage;
import com.retail.common.ProjectionFailureRouter;
import com.retail.order.service.OrderProjectionService;

@Configuration
@EnableKafkaStreams
@ConditionalOnProperty(name = "retail.events.streams.enabled", havingValue = "true")
public class OrderProjectionStreamsConfiguration {

    @Bean
    KStream<String, DomainEventMessage> orderProjectionStream(StreamsBuilder streamsBuilder,
            OrderProjectionService projectionService,
            @Value("${retail.events.topic}") String orderEventsTopic,
            @Value("${retail.events.projection.dlt-topic}") String deadLetterTopic) {
        JsonSerde<DomainEventMessage> eventSerde = new JsonSerde<>(DomainEventMessage.class);
        eventSerde.deserializer().addTrustedPackages("com.retail.common");
        eventSerde.deserializer().ignoreTypeHeaders();
        KStream<String, DomainEventMessage> events = streamsBuilder.stream(orderEventsTopic,
                Consumed.with(Serdes.String(), eventSerde));
        ProjectionFailureRouter.route(events, projectionService::apply, deadLetterTopic);
        return events;
    }
}
