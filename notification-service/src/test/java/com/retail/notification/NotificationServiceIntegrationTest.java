package com.retail.notification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.notification.client.CustomerClient;
import com.retail.notification.domain.Notification;
import com.retail.notification.domain.NotificationDomainEvent;
import com.retail.notification.dto.AddressDto;
import com.retail.notification.dto.CustomerDto;
import com.retail.notification.dto.NotificationAreaBroadcastRequest;
import com.retail.notification.dto.NotificationBroadcastRequest;
import com.retail.notification.dto.NotificationCreateRequest;
import com.retail.notification.dto.NotificationResponse;
import com.retail.notification.repository.NotificationDomainEventRepository;
import com.retail.notification.repository.NotificationRepository;
import com.retail.notification.repository.NotificationViewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"null", "unused"})
class NotificationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private NotificationDomainEventRepository notificationDomainEventRepository;

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private NotificationViewRepository notificationViewRepository;

        @MockitoBean
    private CustomerClient customerClient;

        @MockitoBean
        private DomainEventPublisher domainEventPublisher;

    private CustomerDto cust1;
    private CustomerDto cust2;

    @BeforeEach
        @SuppressWarnings("unused")
    void setUp() {
        AddressDto addr1 = new AddressDto(null, 10, "George St", "Sydney", "Sydney", 2000, "NSW", "Australia");
        AddressDto addr2 = new AddressDto(null, 20, "Bourke St", "Melbourne", "Melbourne", 3000, "VIC", "Australia");

        cust1 = new CustomerDto(1L, "Alice", "alice@test.com", "0400111222", "Email", addr1);
        cust2 = new CustomerDto(2L, "Bob", "bob@test.com", "0400333444", "Phone", addr2);

        when(customerClient.getCustomerById(1L)).thenReturn(Optional.of(cust1));
        when(customerClient.getCustomerById(2L)).thenReturn(Optional.of(cust2));
        when(customerClient.getCustomerByEmail("alice@test.com")).thenReturn(Optional.of(cust1));
        when(customerClient.getCustomerByPhone("0400333444")).thenReturn(Optional.of(cust2));
        when(customerClient.getAllCustomers()).thenReturn(List.of(cust1, cust2));
    }

    @Test
    void testCreateAndGetByCustomerId_HappyPath() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("Welcome to the retail platform!");

        String responseJson = mockMvc.perform(post("/notification/customer/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.type").value("Email"))
                .andExpect(jsonPath("$.message").value("Welcome to the retail platform!"))
                .andReturn().getResponse().getContentAsString();

        NotificationResponse created = objectMapper.readValue(responseJson, NotificationResponse.class);

        // Get by ID
        mockMvc.perform(get("/notification/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()));

        // Get by customer ID
        mockMvc.perform(get("/notification/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        org.junit.jupiter.api.Assertions.assertTrue(notificationViewRepository.existsById(created.getId()));

        verify(domainEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq("notification.events"),
                org.mockito.ArgumentMatchers.any(DomainEventMessage.class));
    }

    @Test
    void testCreateByEmailAndPhone_HappyPath() throws Exception {
        NotificationCreateRequest requestEmail = new NotificationCreateRequest("Special offer via Email");
        mockMvc.perform(post("/notification/customer/email/alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.type").value("Email"));

        NotificationCreateRequest requestPhone = new NotificationCreateRequest("Special offer via SMS");
        mockMvc.perform(post("/notification/customer/phone/0400333444")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPhone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.type").value("Phone"));
    }

    @Test
    void testBroadcastNotifications() throws Exception {
        NotificationBroadcastRequest broadcast = new NotificationBroadcastRequest("Storewide discount this weekend!");

        mockMvc.perform(post("/notification/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcast)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testAreaBroadcastNotifications() throws Exception {
        NotificationAreaBroadcastRequest areaBroadcast = new NotificationAreaBroadcastRequest(
                "NSW Flash Sale!", null, "NSW", null
        );

        mockMvc.perform(post("/notification/broadcast/area")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(areaBroadcast)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }

    @Test
    void notificationCommandsAppendImmutablePersistedEvents() throws Exception {
        long eventsBefore = notificationDomainEventRepository.count();

        mockMvc.perform(post("/notification/customer/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationCreateRequest("Original message"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/notification/customer/email/alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationCreateRequest("Email message"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/notification/customer/phone/0400333444")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationCreateRequest("Phone message"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/notification/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationBroadcastRequest("Broadcast message"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/notification/broadcast/area")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NotificationAreaBroadcastRequest("Area message", null, "NSW", null))))
                .andExpect(status().isCreated());

        List<NotificationDomainEvent> events = notificationDomainEventRepository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(eventsBefore + 6, events.size());
        NotificationDomainEvent first = events.stream()
                .filter(event -> event.getPayload().contains("Original message"))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Notification", first.getAggregateType());
        org.junit.jupiter.api.Assertions.assertEquals("NotificationCreatedEvent", first.getEventType());
        org.junit.jupiter.api.Assertions.assertEquals(1, first.getAggregateVersion());
        org.junit.jupiter.api.Assertions.assertNotNull(first.getEventId());
        org.junit.jupiter.api.Assertions.assertNotNull(first.getOccurredAt());
        org.junit.jupiter.api.Assertions.assertTrue(first.getPayload().contains("Original message"));
        org.junit.jupiter.api.Assertions.assertNull(first.getCorrelationId());
        org.junit.jupiter.api.Assertions.assertNull(first.getCausationId());

        JsonNode payload = objectMapper.readTree(first.getPayload());
        Notification notification = notificationRepository.findById(payload.get("notificationId").asLong()).orElseThrow();
        notification.setMessage("Updated message");
        notificationRepository.save(notification);
        org.junit.jupiter.api.Assertions.assertEquals("Original message", objectMapper.readTree(
                notificationDomainEventRepository.findById(first.getEventId()).orElseThrow().getPayload())
                .get("message").asText());
    }

    @Test
    void testDateRangeQueries() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("Dated Notification");
        mockMvc.perform(post("/notification/customer/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LocalDate today = LocalDate.now();

        // Get by date
        mockMvc.perform(get("/notification/date/" + today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Get by date range
        mockMvc.perform(get("/notification/date-range")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCustomerNotFound_Returns404() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("Hello");

        mockMvc.perform(post("/notification/customer/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testValidationFailure_Returns400() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("");

        mockMvc.perform(post("/notification/customer/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
