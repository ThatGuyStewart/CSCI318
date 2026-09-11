package com.retail.notification.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.notification.client.CustomerClient;
import com.retail.notification.domain.DeliveryType;
import com.retail.notification.domain.Notification;
import com.retail.notification.domain.NotificationDomainEvent;
import com.retail.notification.domain.NotificationView;
import com.retail.notification.dto.AddressDto;
import com.retail.notification.dto.CustomerDto;
import com.retail.notification.dto.NotificationAreaBroadcastRequest;
import com.retail.notification.dto.NotificationBroadcastRequest;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.dto.NotificationResponse;
import com.retail.notification.exception.BadRequestException;
import com.retail.notification.exception.ResourceNotFoundException;
import com.retail.notification.repository.NotificationDomainEventRepository;
import com.retail.notification.repository.NotificationRepository;
import com.retail.notification.repository.NotificationViewRepository;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationViewRepository notificationViewRepository;
    private final NotificationDomainEventRepository notificationDomainEventRepository;
    private final CustomerClient customerClient;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final String eventTopic;

    public NotificationService(NotificationRepository notificationRepository,
            NotificationViewRepository notificationViewRepository,
            NotificationDomainEventRepository notificationDomainEventRepository, CustomerClient customerClient,
            ObjectMapper objectMapper, DomainEventPublisher domainEventPublisher,
            @Value("${retail.events.topic}") String eventTopic) {
        this.notificationRepository = notificationRepository;
        this.notificationViewRepository = notificationViewRepository;
        this.notificationDomainEventRepository = notificationDomainEventRepository;
        this.customerClient = customerClient;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.eventTopic = eventTopic;
    }

    public NotificationResponse createNotificationForCustomerId(Long customerId, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customerId, deliveryType, request.getMessage(), now());
        Notification saved = saveNotification(notification);

        return toNotificationResponse(saved);
    }

    public NotificationResponse createNotificationByEmail(String email, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customer.getCustomerId(), deliveryType, request.getMessage(), now());
        Notification saved = saveNotification(notification);

        return toNotificationResponse(saved);
    }

    public NotificationResponse createNotificationByPhone(String phone, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + phone));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customer.getCustomerId(), deliveryType, request.getMessage(), now());
        Notification saved = saveNotification(notification);

        return toNotificationResponse(saved);
    }

    public List<NotificationResponse> broadcastNotification(NotificationBroadcastRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        List<CustomerDto> customers = customerClient.getAllCustomers();
        List<NotificationResponse> responses = new ArrayList<>();

        for (CustomerDto customer : customers) {
            DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
            Notification notification = new Notification(customer.getCustomerId(), deliveryType, request.getMessage(), now());
            Notification saved = saveNotification(notification);
            responses.add(toNotificationResponse(saved));
        }

        return responses;
    }

    public List<NotificationResponse> broadcastAreaNotification(NotificationAreaBroadcastRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        List<CustomerDto> customers = customerClient.getAllCustomers();
        List<NotificationResponse> responses = new ArrayList<>();

        for (CustomerDto customer : customers) {
            if (matchesArea(customer, request)) {
                DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
                Notification notification = new Notification(customer.getCustomerId(), deliveryType, request.getMessage(),
                        now());
                Notification saved = saveNotification(notification);
                responses.add(toNotificationResponse(saved));
            }
        }

        return responses;
    }

    private boolean matchesArea(CustomerDto customer, NotificationAreaBroadcastRequest request) {
        AddressDto addr = customer.getAddress();
        return addr != null
                && (request.getPostcode() == null
                        || (addr.getPostcode() != null && addr.getPostcode().equals(request.getPostcode())))
                && (request.getState() == null
                        || (addr.getState() != null && addr.getState().equalsIgnoreCase(request.getState())))
                && (request.getCountry() == null
                        || (addr.getCountry() != null && addr.getCountry().equalsIgnoreCase(request.getCountry())));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Long notificationId = Objects.requireNonNull(id, "Notification id is required");
        NotificationView notification = notificationViewRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        return toNotificationResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerId(Long customerId) {
        return findNotificationsByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerId(Long customerId, LocalDate date, LocalDate from,
            LocalDate to) {
        return findNotificationsByCustomerId(customerId, date, from, to);
    }

    private List<NotificationResponse> findNotificationsByCustomerId(Long customerId) {
        Long resolvedCustomerId = Objects.requireNonNull(customerId, "Customer id is required");
        return notificationViewRepository.findByCustomerId(resolvedCustomerId).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    private List<NotificationResponse> findNotificationsByCustomerId(Long customerId, LocalDate date, LocalDate from,
            LocalDate to) {
        Long resolvedCustomerId = Objects.requireNonNull(customerId, "Customer id is required");
        validateDateFilters(date, from, to);
        List<NotificationView> notifications;
        if (date != null) {
            notifications = notificationViewRepository.findByCustomerIdAndSentBetween(resolvedCustomerId,
                    date.atStartOfDay(), date.atTime(LocalTime.MAX));
        } else if (from != null) {
            notifications = notificationViewRepository.findByCustomerIdAndSentBetween(resolvedCustomerId,
                    from.atStartOfDay(), to.atTime(LocalTime.MAX));
        } else {
            notifications = notificationViewRepository.findByCustomerId(resolvedCustomerId);
        }
        return notifications.stream().map(this::toNotificationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerEmail(String email) {
        return findNotificationsByCustomerEmail(email);
    }

    private List<NotificationResponse> findNotificationsByCustomerEmail(String email) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getCustomerId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerEmail(String email, LocalDate date, LocalDate from,
            LocalDate to) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getCustomerId(), date, from, to);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerPhone(String phone) {
        return findNotificationsByCustomerPhone(phone);
    }

    private List<NotificationResponse> findNotificationsByCustomerPhone(String phone) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getCustomerId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerPhone(String phone, LocalDate date, LocalDate from,
            LocalDate to) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getCustomerId(), date, from, to);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(LocalDate date, LocalDate from, LocalDate to) {
        validateDateFilters(date, from, to);
        List<NotificationView> notifications;
        if (date != null) {
            notifications = notificationViewRepository.findBySentBetween(date.atStartOfDay(),
                    date.atTime(LocalTime.MAX));
        } else if (from != null) {
            notifications = notificationViewRepository.findBySentBetween(from.atStartOfDay(), to.atTime(LocalTime.MAX));
        } else {
            notifications = notificationViewRepository.findAll();
        }
        return notifications.stream().map(this::toNotificationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return notificationViewRepository.findBySentBetween(start, end).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByDateRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return notificationViewRepository.findBySentBetween(start, end).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    private void validateDateFilters(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && (from != null || to != null)) {
            throw new BadRequestException("date cannot be combined with from or to");
        }
        if ((from == null) != (to == null)) {
            throw new BadRequestException("from and to must be provided together");
        }
        validateDateRange(from, to);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be on or before to");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    private DeliveryType parseDeliveryType(String contactMethod) {
        if ("Phone".equalsIgnoreCase(contactMethod)) {
            return DeliveryType.Phone;
        }
        return DeliveryType.Email;
    }

    private Notification saveNotification(Notification notification) {
        Notification saved = notificationRepository
                .save(Objects.requireNonNull(notification, "Notification is required"));
        appendNotificationCreatedEvent(saved);
        notificationViewRepository.save(new NotificationView(saved.getNotificationId(), saved.getCustomerId(), saved.getType(),
                saved.getMessage(), saved.getSent()));
        return saved;
    }

    private void appendNotificationCreatedEvent(Notification notification) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", notification.getNotificationId());
        payload.put("customerId", notification.getCustomerId());
        payload.put("type", notification.getType());
        payload.put("message", notification.getMessage());
        payload.put("sentAt", notification.getSent());
        try {
            NotificationDomainEvent event = new NotificationDomainEvent(UUID.randomUUID(), "Notification",
                    notification.getNotificationId(), 1, "NotificationCreatedEvent", Instant.now(), null, null,
                    objectMapper.writeValueAsString(payload));
            notificationDomainEventRepository.save(event);
            domainEventPublisher.publish(eventTopic,
                    new DomainEventMessage(event.getEventId(), event.getAggregateType(),
                            event.getAggregateId(), event.getAggregateVersion(), event.getEventType(),
                            event.getOccurredAt(),
                            event.getCorrelationId(), event.getCausationId(), payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize notification event payload", exception);
        }
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getCustomerId(),
                notification.getType(),
                notification.getMessage(),
                notification.getSent());
    }

    public NotificationResponse toNotificationResponse(NotificationView notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getCustomerId(), notification.getType(),
                notification.getMessage(), notification.getSent());
    }
}
