package com.retail.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.order.client.CustomerClient;
import com.retail.order.client.NotificationClient;
import com.retail.order.client.ProductClient;
import com.retail.order.domain.OrderStatus;
import com.retail.order.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerClient customerClient;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private NotificationClient notificationClient;

    private AddressDto sampleAddress;
    private CustomerDto sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleAddress = new AddressDto(1, 10, "George St", "Sydney", "Sydney", 2000, "NSW", "Australia");
        sampleCustomer = new CustomerDto(1L, "John Doe", "john@example.com", "0412345678", "Email", sampleAddress);

        Mockito.when(customerClient.getCustomerById(eq(1L))).thenReturn(Optional.of(sampleCustomer));
        Mockito.when(customerClient.getCustomerByEmail(eq("john@example.com"))).thenReturn(Optional.of(sampleCustomer));
        Mockito.when(customerClient.getCustomerByPhone(eq("0412345678"))).thenReturn(Optional.of(sampleCustomer));

        Mockito.when(productClient.getProductById(eq(50L))).thenReturn(
                Optional.of(new ProductDto(50L, "Coffee Beans", "Grocery", 25.0, "Arabica beans"))
        );
    }

    @Test
    void testCreateOrderWithExplicitItems_HappyPath() throws Exception {
        OrderItemDto item = new OrderItemDto(50L, "Coffee Beans", 25.0, 2, 50.0);
        OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

        String responseJson = mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.status").value("Placed"))
                .andExpect(jsonPath("$.total").value(50.0))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

        // Get by ID
        mockMvc.perform(get("/order/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()));

        // Check notification client called
        Mockito.verify(notificationClient, Mockito.atLeastOnce())
                .sendNotificationToCustomer(eq(1L), any());
    }

        @Test
        void testCreateOrderWithUnresolvedProduct_ReturnsNotFound() throws Exception {
                OrderItemDto item = new OrderItemDto(999L, "Unverified Product", 10.0, 1, 10.0);
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

                mockMvc.perform(post("/order")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

    @Test
    void testCreateOrderFromCustomerBasket_HappyPath() throws Exception {
        BasketItemDto basketItem = new BasketItemDto(50L, "Coffee Beans", 25.0, 3, 75.0);
        BasketDto basket = new BasketDto(1L, List.of(basketItem), 75.0);
        Mockito.when(customerClient.getCustomerBasket(eq(1L))).thenReturn(Optional.of(basket));

        // Create order with no items -> fetches from basket
        OrderCreateRequest request = new OrderCreateRequest(1L, null, null);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.total").value(75.0))
                .andExpect(jsonPath("$.items", hasSize(1)));

        // Verify basket cleared
        Mockito.verify(customerClient).clearCustomerBasket(eq(1L));
    }

    @Test
    void testCreateOrder_ValidationFail_EmptyBasket() throws Exception {
        Mockito.when(customerClient.getCustomerBasket(eq(1L))).thenReturn(Optional.of(new BasketDto(1L, List.of(), 0.0)));

        OrderCreateRequest request = new OrderCreateRequest(1L, null, null);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void testCancelOrder_HappyAndNonCancellable() throws Exception {
        OrderItemDto item = new OrderItemDto(50L, "Coffee Beans", 25.0, 1, 25.0);
        OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

        String responseJson = mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

        // Cancel order while Placed -> should become Cancelled
        mockMvc.perform(post("/order/" + created.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));

        // Try cancelling again (already Cancelled) -> order status stays Cancelled
        mockMvc.perform(post("/order/" + created.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelled"));
    }

    @Test
    void testQueryOrdersByCustomerAndProduct() throws Exception {
        OrderItemDto item = new OrderItemDto(50L, "Coffee Beans", 25.0, 1, 25.0);
        OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // By Customer ID
        mockMvc.perform(get("/order/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // By Customer Email
        mockMvc.perform(get("/order/customer/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // By Customer Phone
        mockMvc.perform(get("/order/customer/phone/0412345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // By Product ID
        mockMvc.perform(get("/order/product/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testUpdateOrderStatus() throws Exception {
        OrderItemDto item = new OrderItemDto(50L, "Coffee Beans", 25.0, 1, 25.0);
        OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

        String responseJson = mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

        OrderStatusUpdateRequest update = new OrderStatusUpdateRequest(OrderStatus.InTransit);

        mockMvc.perform(put("/order/" + created.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("InTransit"));
    }
}
