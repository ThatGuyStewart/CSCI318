package com.retail.customer;

import java.util.Optional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.client.ProductClient;
import com.retail.customer.domain.ContactMethod;
import com.retail.customer.dto.AddressDto;
import com.retail.customer.dto.AddressUpdateRequest;
import com.retail.customer.dto.BasketAddRequest;
import com.retail.customer.dto.BasketRemoveRequest;
import com.retail.customer.dto.CustomerCreateRequest;
import com.retail.customer.dto.CustomerResponse;
import com.retail.customer.dto.CustomerUpdateRequest;
import com.retail.customer.dto.ProductDto;
import com.retail.customer.repository.CustomerViewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"null", "unused"})
class CustomerServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private CustomerViewRepository customerViewRepository;

        @MockitoBean
    private ProductClient productClient;

        @MockitoBean
        private DomainEventPublisher domainEventPublisher;

    private AddressDto sampleAddress;

    @BeforeEach
        @SuppressWarnings("unused")
    void setUp() {
        sampleAddress = new AddressDto(12, 100, "George St", "Sydney CBD", "Sydney", 2000, "NSW", "Australia");
                when(productClient.getProductById(101L)).thenReturn(
                Optional.of(new ProductDto(101L, "Laptop", "Electronics", 1200.0, "High end laptop"))
        );
    }

    @Test
    void testCreateAndGetCustomer_HappyPath() throws Exception {
        CustomerCreateRequest createRequest = new CustomerCreateRequest(
                "John Doe", "john.doe@example.com", "0412345678", ContactMethod.Email, sampleAddress
        );

        String responseJson = mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.contactMethod").value("Email"))
                .andExpect(jsonPath("$.address.city").value("Sydney"))
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(responseJson, CustomerResponse.class);

        // Get by ID
        mockMvc.perform(get("/customer/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("John Doe"));

        // List all
        String customersJson = mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(customersJson.contains("\n"));

        org.junit.jupiter.api.Assertions.assertTrue(customerViewRepository.existsById(created.getId()));
    }

    @Test
    void testCreateCustomer_ValidationError_Returns400() throws Exception {
        // Missing name, email, phone
        CustomerCreateRequest invalidRequest = new CustomerCreateRequest(
                "", "", "", null, null
        );

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void testGetCustomerNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/customer/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testUpdateCustomer_HappyAndNotFound() throws Exception {
        CustomerCreateRequest createRequest = new CustomerCreateRequest(
                "Alice Smith", "alice.smith@example.com", "0498765432", ContactMethod.Phone, sampleAddress
        );

        String responseJson = mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(responseJson, CustomerResponse.class);

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                "Alice Johnson", null, null, ContactMethod.Email, null
        );

        mockMvc.perform(put("/customer/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.contactMethod").value("Email"));

        // Update address
        AddressUpdateRequest addressUpdate = new AddressUpdateRequest(
                5, 200, "Pitt St", "Sydney CBD", "Sydney", 2000, "NSW", "Australia"
        );
        mockMvc.perform(put("/customer/" + created.getId() + "/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Pitt St"))
                .andExpect(jsonPath("$.unitNumber").value(5));
    }

    @Test
    void testDeleteCustomer_HappyAndNotFound() throws Exception {
        CustomerCreateRequest createRequest = new CustomerCreateRequest(
                "Bob Brown", "bob.brown@example.com", "0411223344", ContactMethod.Email, sampleAddress
        );

        String responseJson = mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(responseJson, CustomerResponse.class);

        // Delete
        mockMvc.perform(delete("/customer/" + created.getId()))
                .andExpect(status().isNoContent());

        // Get after delete should be 404
        mockMvc.perform(get("/customer/" + created.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCommandsAppendImmutablePersistedEvents() throws Exception {
        CustomerCreateRequest createRequest = new CustomerCreateRequest(
                "Event Customer", "event.customer@example.com", "0400000000", ContactMethod.Email, sampleAddress);
        String responseJson = mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        CustomerResponse created = objectMapper.readValue(responseJson, CustomerResponse.class);

        mockMvc.perform(put("/customer/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerUpdateRequest(
                                "Updated Event Customer", null, null, null, null))))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/customer/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customer/" + created.getId() + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].eventType").value("CustomerCreatedEvent"))
                .andExpect(jsonPath("$[0].aggregateVersion").value(1))
                .andExpect(jsonPath("$[0].payload.name").value("Event Customer"))
                .andExpect(jsonPath("$[1].eventType").value("CustomerUpdatedEvent"))
                .andExpect(jsonPath("$[1].aggregateVersion").value(2))
                .andExpect(jsonPath("$[1].payload.name").value("Updated Event Customer"))
                .andExpect(jsonPath("$[2].eventType").value("CustomerDeletedEvent"))
                .andExpect(jsonPath("$[2].aggregateVersion").value(3))
                .andExpect(jsonPath("$[2].payload.name").value("Updated Event Customer"))
                .andExpect(jsonPath("$[0].eventId").isNotEmpty())
                .andExpect(jsonPath("$[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$[0].timestamp").doesNotExist());

        mockMvc.perform(get("/customer/email/event.customer@example.com/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        verify(domainEventPublisher, atLeastOnce()).publish(
                org.mockito.ArgumentMatchers.eq("customer.events"),
                org.mockito.ArgumentMatchers.any(DomainEventMessage.class));
    }

        @Test
        void eventHistoryForUnknownCustomerReturnsNotFound() throws Exception {
                mockMvc.perform(get("/customer/99999/event"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

    @Test
    void testBasketOperations_HappyAndEdgePaths() throws Exception {
        CustomerCreateRequest createRequest = new CustomerCreateRequest(
                "Charlie Green", "charlie.green@example.com", "0455667788", ContactMethod.Email, sampleAddress
        );

        String responseJson = mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(responseJson, CustomerResponse.class);

        // Initial Basket should be empty
        mockMvc.perform(get("/customer/" + created.getId() + "/basket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(created.getId()))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0.0));

        mockMvc.perform(get("/customer/" + created.getId() + "//basket/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(created.getId()));

        // Add 2 Laptops
        BasketAddRequest addRequest = new BasketAddRequest(101L, 2);
        mockMvc.perform(post("/customer/" + created.getId() + "/basket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(101))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(1200.0))
                .andExpect(jsonPath("$.total").value(2400.0));

        // Remove 1 Laptop
        BasketRemoveRequest removeRequest = new BasketRemoveRequest(101L, 1);
        mockMvc.perform(delete("/customer/" + created.getId() + "/basket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.total").value(1200.0));

        // Remove remaining quantity -> item should be removed completely
        mockMvc.perform(delete("/customer/" + created.getId() + "/basket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0.0));

        mockMvc.perform(get("/customer/" + created.getId() + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].eventType").value("BasketItemAddedEvent"))
                .andExpect(jsonPath("$[1].aggregateVersion").value(2))
                .andExpect(jsonPath("$[1].payload.items[0].quantity").value(2))
                .andExpect(jsonPath("$[2].eventType").value("BasketItemRemovedEvent"))
                .andExpect(jsonPath("$[3].eventType").value("BasketItemRemovedEvent"))
                .andExpect(jsonPath("$[3].payload.items", hasSize(0)))
                .andExpect(jsonPath("$[3].payload.total").value(0.0));

        verify(domainEventPublisher, atLeastOnce()).publish(
                org.mockito.ArgumentMatchers.eq("customer.events"),
                org.mockito.ArgumentMatchers.any(DomainEventMessage.class));
    }

    @Test
    void testLocationQueriesAndEventEndpoints() throws Exception {
        CustomerCreateRequest first = new CustomerCreateRequest(
                "Dana West", "dana.west@example.com", "0411111111", ContactMethod.Email,
                new AddressDto(2, 20, "Queen St", "Sydney", "Sydney", 2000, "NSW", "Australia")
        );
        CustomerCreateRequest second = new CustomerCreateRequest(
                "Eli North", "eli.north@example.com", "0422222222", ContactMethod.Phone,
                new AddressDto(5, 15, "Collins St", "Melbourne", "Melbourne", 3000, "VIC", "Australia")
        );

        mockMvc.perform(post("/customer").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/customer").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/customer/state/NSW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/customer/country/Australia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        mockMvc.perform(get("/customer/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/customer/email/dana.west@example.com/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
