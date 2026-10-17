package com.retail.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.notification.domain.NotificationDomainEvent;
import com.retail.notification.repository.NotificationDomainEventRepository;
import com.retail.notification.repository.NotificationProjectionCheckpointRepository;
import com.retail.notification.repository.NotificationViewRepository;

@Service
public class NotificationProjectionRebuilder {

    private static final String NOTIFICATION_AGGREGATE_TYPE = "Notification";

    private final NotificationDomainEventRepository eventRepository;
    private final NotificationViewRepository viewRepository;
    private final NotificationProjectionCheckpointRepository checkpointRepository;
    private final NotificationProjectionService projectionService;
    private final ObjectMapper objectMapper;

    public NotificationProjectionRebuilder(NotificationDomainEventRepository eventRepository,
            NotificationViewRepository viewRepository,
            NotificationProjectionCheckpointRepository checkpointRepository,
            NotificationProjectionService projectionService, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.viewRepository = viewRepository;
        this.checkpointRepository = checkpointRepository;
        this.projectionService = projectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int rebuild() {
        List<NotificationDomainEvent> events = eventRepository
                .findByAggregateTypeOrderByAggregateIdAscAggregateVersionAsc(NOTIFICATION_AGGREGATE_TYPE);
        viewRepository.deleteAllInBatch();
        checkpointRepository.deleteAllInBatch();
        for (NotificationDomainEvent event : events) {
            projectionService.apply(toMessage(event));
        }
        return events.size();
    }

    private DomainEventMessage toMessage(NotificationDomainEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() { });
            return new DomainEventMessage(event.getEventId(), event.getAggregateType(), event.getAggregateId(),
                    event.getAggregateVersion(), event.getEventType(), event.getOccurredAt(), event.getCorrelationId(),
                    event.getCausationId(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize persisted Notification event "
                    + event.getEventId(), exception);
        }
    }
}
