package com.retail.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.order.client.CustomerClient;
import com.retail.order.client.ProductClient;
import com.retail.order.domain.OrderStatus;
import com.retail.order.dto.AddressDto;
import com.retail.order.dto.BasketDto;
import com.retail.order.dto.BasketItemDto;
import com.retail.order.dto.CustomerDto;
import com.retail.order.dto.OrderCreateRequest;
import com.retail.order.dto.OrderItemDto;
import com.retail.order.dto.OrderResponse;
import com.retail.order.dto.OrderStatusUpdateRequest;
import com.retail.order.dto.ProductDto;
import com.retail.order.exception.ServiceUnavailableException;
import com.retail.order.repository.OrderSummaryViewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({ "null", "unused" })
class OrderServiceIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CustomerClient customerClient;

        @MockitoBean
        private ProductClient productClient;

        @MockitoBean
        private DomainEventPublisher domainEventPublisher;

        private AddressDto sampleAddress;
        private CustomerDto sampleCustomer;

        @Autowired
        private OrderSummaryViewRepository orderSummaryViewRepository;

        @BeforeEach
        @SuppressWarnings("unused")
        void setUp() {
                sampleAddress = new AddressDto(1, 10, "George St", "Sydney", "Sydney", 2000, "NSW", "Australia");
                sampleCustomer = new CustomerDto(1L, "John Doe", "john@example.com", "0412345678", "Email",
                                sampleAddress);

                when(customerClient.getCustomerById(1L)).thenReturn(Optional.of(sampleCustomer));
                when(customerClient.getCustomerByEmail("john@example.com")).thenReturn(Optional.of(sampleCustomer));
                when(customerClient.getCustomerByPhone("0412345678")).thenReturn(Optional.of(sampleCustomer));

                when(productClient.getProductById(50L)).thenReturn(
                                Optional.of(new ProductDto(50L, "Coffee Beans", "Grocery", 25.0, "Arabica beans")));
        }

        @Test
        void testCreateOrderWithExplicitItems_HappyPath() throws Exception {
                OrderItemDto item = new OrderItemDto(50L, "Coffee Beans", 25.0, 2, 50.0);
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of(item));

                String responseJson = mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.orderId").isNumber())
                                .andExpect(jsonPath("$.customerId").value(1))
                                .andExpect(jsonPath("$.status").value("Placed"))
                                .andExpect(jsonPath("$.total").value(50.0))
                                .andExpect(jsonPath("$.items", hasSize(1)))
                                .andReturn().getResponse().getContentAsString();

                OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

                // Get by ID
                mockMvc.perform(get("/order/" + created.getOrderId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(created.getOrderId()));

                org.junit.jupiter.api.Assertions.assertTrue(orderSummaryViewRepository.existsById(created.getOrderId()));
                verify(domainEventPublisher).publish(eq("order.events"), any(DomainEventMessage.class));
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
        void eventHistoryForUnknownOrderReturnsNotFound() throws Exception {
                mockMvc.perform(get("/order/99999/event"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void testCreateOrderFromCustomerBasket_HappyPath() throws Exception {
                BasketItemDto basketItem = new BasketItemDto(50L, "Coffee Beans", 25.0, 3, 75.0);
                BasketDto basket = new BasketDto(1L, List.of(basketItem), 75.0);
                when(customerClient.getCustomerBasket(1L)).thenReturn(Optional.of(basket));

                // Create order with no items -> fetches from basket
                OrderCreateRequest request = new OrderCreateRequest(1L, null, null);

                String responseJson = mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.customerId").value(1))
                                .andExpect(jsonPath("$.total").value(75.0))
                                .andExpect(jsonPath("$.items", hasSize(1)))
                                .andReturn().getResponse().getContentAsString();
                OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

                mockMvc.perform(get("/order/" + created.getOrderId() + "/event"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].eventType").value("OrderPlacedEvent"))
                                .andExpect(jsonPath("$[0].aggregateVersion").value(1))
                                .andExpect(jsonPath("$[1].eventType").value("BasketUsedForOrderEvent"))
                                .andExpect(jsonPath("$[1].aggregateVersion").value(2));

                // Basket clearing is performed asynchronously by Customer Service's order-event
                // consumer.
        }

        @Test
        void testCreateOrderWithExplicitEmptyItems_ReturnsBadRequest() throws Exception {
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress, List.of());

                mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        void testCreateOrder_ValidationFail_EmptyBasket() throws Exception {
                when(customerClient.getCustomerBasket(1L)).thenReturn(Optional.of(new BasketDto(1L, List.of(), 0.0)));

                OrderCreateRequest request = new OrderCreateRequest(1L, null, null);

                mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        void createOrderRejectsNonPositiveItemQuantity() throws Exception {
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress,
                                List.of(new OrderItemDto(50L, null, null, 0, null)));

                mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        void createOrderRejectsMissingRequestAndCustomerAddress() throws Exception {
                CustomerDto customerWithoutAddress = new CustomerDto(1L, "John Doe", "john@example.com", "0412345678",
                                "Email", null);
                when(customerClient.getCustomerById(1L)).thenReturn(Optional.of(customerWithoutAddress));
                OrderCreateRequest request = new OrderCreateRequest(1L, null,
                                List.of(new OrderItemDto(50L, null, null, 1, null)));

                mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        void createOrderReturnsServiceUnavailableWhenProductServiceFails() throws Exception {
                when(productClient.getProductById(50L))
                                .thenThrow(new ServiceUnavailableException("Product service is unavailable",
                                                new RuntimeException()));
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress,
                                List.of(new OrderItemDto(50L, null, null, 1, null)));

                mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
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
                mockMvc.perform(post("/order/" + created.getOrderId() + "/cancel"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("Cancelled"));

                // Try cancelling again (already Cancelled) -> order status stays Cancelled
                mockMvc.perform(post("/order/" + created.getOrderId() + "/cancel"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("Cancelled"));

                mockMvc.perform(get("/order/" + created.getOrderId() + "/event"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(3)))
                                .andExpect(jsonPath("$[0].eventType").value("OrderPlacedEvent"))
                                .andExpect(jsonPath("$[0].aggregateVersion").value(1))
                                .andExpect(jsonPath("$[1].eventType").value("OrderCancelledEvent"))
                                .andExpect(jsonPath("$[1].aggregateVersion").value(2))
                                .andExpect(jsonPath("$[2].eventType").value("OrderCancelFailedEvent"))
                                .andExpect(jsonPath("$[2].aggregateVersion").value(3))
                                .andExpect(jsonPath("$[2].payload.status").value("Cancelled"));
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

                mockMvc.perform(put("/order/" + created.getOrderId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("InTransit"));
        }

        @Test
        void orderCommandsAppendImmutablePersistedEventsAndExposeCollectionRoute() throws Exception {
                OrderCreateRequest request = new OrderCreateRequest(1L, sampleAddress,
                                List.of(new OrderItemDto(50L, "Coffee Beans", 25.0, 1, 25.0)));
                String responseJson = mockMvc.perform(post("/order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

                mockMvc.perform(post("/order/" + created.getOrderId() + "/cancel"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("Cancelled"));
                mockMvc.perform(put("/order/" + created.getOrderId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new OrderStatusUpdateRequest(OrderStatus.InTransit))))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/order/" + created.getOrderId() + "/event"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(3)))
                                .andExpect(jsonPath("$[0].eventType").value("OrderPlacedEvent"))
                                .andExpect(jsonPath("$[0].aggregateVersion").value(1))
                                .andExpect(jsonPath("$[0].payload.status").value("Placed"))
                                .andExpect(jsonPath("$[0].payload.items[0].productId").value(50))
                                .andExpect(jsonPath("$[0].payload.items[0].subtotal").value(25.0))
                                .andExpect(jsonPath("$[1].eventType").value("OrderCancelledEvent"))
                                .andExpect(jsonPath("$[1].aggregateVersion").value(2))
                                .andExpect(jsonPath("$[1].payload.status").value("Cancelled"))
                                .andExpect(jsonPath("$[2].eventType").value("OrderStatusChangedEvent"))
                                .andExpect(jsonPath("$[0].timestamp").doesNotExist());

                mockMvc.perform(get("/order/event").param("date", LocalDate.now().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        }
}
