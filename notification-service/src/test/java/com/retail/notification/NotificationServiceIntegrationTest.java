package com.retail.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.notification.client.CustomerClient;
import com.retail.notification.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerClient customerClient;

    private CustomerDto cust1;
    private CustomerDto cust2;

    @BeforeEach
    void setUp() {
        AddressDto addr1 = new AddressDto(null, 10, "George St", "Sydney", "Sydney", 2000, "NSW", "Australia");
        AddressDto addr2 = new AddressDto(null, 20, "Bourke St", "Melbourne", "Melbourne", 3000, "VIC", "Australia");

        cust1 = new CustomerDto(1L, "Alice", "alice@test.com", "0400111222", "Email", addr1);
        cust2 = new CustomerDto(2L, "Bob", "bob@test.com", "0400333444", "Phone", addr2);

        Mockito.when(customerClient.getCustomerById(eq(1L))).thenReturn(Optional.of(cust1));
        Mockito.when(customerClient.getCustomerById(eq(2L))).thenReturn(Optional.of(cust2));
        Mockito.when(customerClient.getCustomerByEmail(eq("alice@test.com"))).thenReturn(Optional.of(cust1));
        Mockito.when(customerClient.getCustomerByPhone(eq("0400333444"))).thenReturn(Optional.of(cust2));
        Mockito.when(customerClient.getAllCustomers()).thenReturn(List.of(cust1, cust2));
    }

    @Test
    void testCreateAndGetByCustomerId_HappyPath() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("Welcome to the retail platform!");

        String responseJson = mockMvc.perform(post("/notification/customer/id/1")
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
        mockMvc.perform(get("/notification/customer/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
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
    void testDateRangeQueries() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("Dated Notification");
        mockMvc.perform(post("/notification/customer/id/1")
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

        mockMvc.perform(post("/notification/customer/id/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testValidationFailure_Returns400() throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest("");

        mockMvc.perform(post("/notification/customer/id/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
