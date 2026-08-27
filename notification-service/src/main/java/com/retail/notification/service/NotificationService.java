package com.retail.notification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.notification.client.CustomerClient;
import com.retail.notification.domain.DeliveryType;
import com.retail.notification.domain.Notification;
import com.retail.notification.dto.AddressDto;
import com.retail.notification.dto.CustomerDto;
import com.retail.notification.dto.NotificationAreaBroadcastRequest;
import com.retail.notification.dto.NotificationBroadcastRequest;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.dto.NotificationResponse;
import com.retail.notification.exception.BadRequestException;
import com.retail.notification.exception.ResourceNotFoundException;
import com.retail.notification.repository.NotificationRepository;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerClient customerClient;

    public NotificationService(NotificationRepository notificationRepository, CustomerClient customerClient) {
        this.notificationRepository = notificationRepository;
        this.customerClient = customerClient;
    }

    public NotificationResponse createNotificationForCustomerId(Long customerId, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customerId, deliveryType, request.getMessage(), now());
        Notification saved = notificationRepository.save(notification);

        return toNotificationResponse(saved);
    }

    public NotificationResponse createNotificationByEmail(String email, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customer.getId(), deliveryType, request.getMessage(), now());
        Notification saved = notificationRepository.save(notification);

        return toNotificationResponse(saved);
    }

    public NotificationResponse createNotificationByPhone(String phone, NotificationCreateRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BadRequestException("Message is required");
        }

        CustomerDto customer = customerClient.getCustomerByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone: " + phone));

        DeliveryType deliveryType = parseDeliveryType(customer.getContactMethod());
        Notification notification = new Notification(customer.getId(), deliveryType, request.getMessage(), now());
        Notification saved = notificationRepository.save(notification);

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
            Notification notification = new Notification(customer.getId(), deliveryType, request.getMessage(), now());
            Notification saved = notificationRepository.save(notification);
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
                Notification notification = new Notification(customer.getId(), deliveryType, request.getMessage(), now());
                Notification saved = notificationRepository.save(notification);
                responses.add(toNotificationResponse(saved));
            }
        }

        return responses;
    }

    private boolean matchesArea(CustomerDto customer, NotificationAreaBroadcastRequest request) {
        AddressDto addr = customer.getAddress();
        return addr != null
                && (request.getPostcode() == null || (addr.getPostcode() != null && addr.getPostcode().equals(request.getPostcode())))
                && (request.getState() == null || (addr.getState() != null && addr.getState().equalsIgnoreCase(request.getState())))
                && (request.getCountry() == null || (addr.getCountry() != null && addr.getCountry().equalsIgnoreCase(request.getCountry())));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Long notificationId = Objects.requireNonNull(id, "Notification id is required");
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        return toNotificationResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerId(Long customerId) {
        return findNotificationsByCustomerId(customerId);
    }

    private List<NotificationResponse> findNotificationsByCustomerId(Long customerId) {
        Long resolvedCustomerId = Objects.requireNonNull(customerId, "Customer id is required");
        return notificationRepository.findByCustomerId(resolvedCustomerId).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerEmail(String email) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomerPhone(String phone) {
        Optional<CustomerDto> customerOpt = customerClient.getCustomerByPhone(phone);
        if (customerOpt.isEmpty()) {
            return List.of();
        }
        return findNotificationsByCustomerId(customerOpt.get().getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return notificationRepository.findBySentBetween(start, end).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByDateRange(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return notificationRepository.findBySentBetween(start, end).stream()
                .map(this::toNotificationResponse)
                .toList();
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

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCustomerId(),
                notification.getType(),
                notification.getMessage(),
                notification.getSent()
        );
    }
}
