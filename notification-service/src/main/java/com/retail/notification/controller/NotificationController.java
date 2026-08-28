package com.retail.notification.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.retail.notification.dto.NotificationAreaBroadcastRequest;
import com.retail.notification.dto.NotificationBroadcastRequest;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.dto.NotificationResponse;
import com.retail.notification.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------
    // Notification Endpoints (Feature N1)
    // -------------------------------------------------------------

    @PostMapping("/customer/{customerId}")
    public ResponseEntity<NotificationResponse> createByCustomerId(@PathVariable("customerId") Long customerId,
                                                                   @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotificationForCustomerId(customerId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationResponse>> getByCustomerId(@PathVariable("customerId") Long customerId) {
        List<NotificationResponse> responses = notificationService.getNotificationsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/customer/email/{email}")
    public ResponseEntity<NotificationResponse> createByEmail(@PathVariable("email") String email,
                                                             @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotificationByEmail(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/customer/email/{email}")
    public ResponseEntity<List<NotificationResponse>> getByEmail(@PathVariable("email") String email) {
        List<NotificationResponse> responses = notificationService.getNotificationsByCustomerEmail(email);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/customer/phone/{phone}")
    public ResponseEntity<NotificationResponse> createByPhone(@PathVariable("phone") String phone,
                                                             @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotificationByPhone(phone, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/customer/phone/{phone}")
    public ResponseEntity<List<NotificationResponse>> getByPhone(@PathVariable("phone") String phone) {
        List<NotificationResponse> responses = notificationService.getNotificationsByCustomerPhone(phone);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/broadcast")
    public ResponseEntity<List<NotificationResponse>> broadcast(@Valid @RequestBody NotificationBroadcastRequest request) {
        List<NotificationResponse> responses = notificationService.broadcastNotification(request);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @PostMapping("/broadcast/area")
    public ResponseEntity<List<NotificationResponse>> broadcastArea(@Valid @RequestBody NotificationAreaBroadcastRequest request) {
        List<NotificationResponse> responses = notificationService.broadcastAreaNotification(request);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<NotificationResponse>> getByDate(@PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<NotificationResponse> responses = notificationService.getNotificationsByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<NotificationResponse>> getByDateRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<NotificationResponse> responses = notificationService.getNotificationsByDateRange(from, to);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable("id") Long id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }
}
