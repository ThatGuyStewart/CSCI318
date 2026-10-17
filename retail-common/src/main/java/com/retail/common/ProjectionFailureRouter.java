package com.retail.common;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;

public final class ProjectionFailureRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionFailureRouter.class);

    private ProjectionFailureRouter() {
    }

    public static void route(KStream<String, DomainEventMessage> events,
            Consumer<DomainEventMessage> projector, String deadLetterTopic) {
        JsonSerde<ProjectionFailure> failureSerde = new JsonSerde<>(ProjectionFailure.class).noTypeInfo();
        failureSerde.deserializer().addTrustedPackages("com.retail.common");
        failureSerde.deserializer().ignoreTypeHeaders();
        events.flatMapValues(event -> {
            try {
                if (event == null) {
                    throw new InvalidProjectionEventException("Projection event value must not be null");
                }
                if (!Integer.valueOf(DomainEventMessage.CURRENT_SCHEMA_VERSION).equals(event.schemaVersion())) {
                    throw new InvalidProjectionEventException("Unsupported event schema version: "
                            + event.schemaVersion());
                }
                projector.accept(event);
                return List.<ProjectionFailure>of();
            } catch (InvalidProjectionEventException exception) {
                LOGGER.error("Invalid event {} for aggregate {}:{} routed to {}",
                        event == null ? null : event.eventId(),
                        event == null ? null : event.aggregateType(),
                        event == null ? null : event.aggregateId(),
                        deadLetterTopic, exception);
                return List.of(new ProjectionFailure(event, exception.getClass().getName(),
                        exception.getMessage(), Instant.now()));
            }
        }).to(deadLetterTopic, Produced.with(Serdes.String(), failureSerde));
    }
}
