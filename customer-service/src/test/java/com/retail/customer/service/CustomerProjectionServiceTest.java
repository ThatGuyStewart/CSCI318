package com.retail.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.CustomerProjectionCheckpoint;
import com.retail.customer.domain.CustomerView;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerProjectionCheckpointRepository;
import com.retail.customer.repository.CustomerViewRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerProjectionServiceTest {

    @Mock
    private CustomerViewRepository customerViewRepository;

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private CustomerProjectionCheckpointRepository checkpointRepository;

    @Test
    void customerUpdateRecreatesMissingViewAndPreservesAuthoritativeBasketTotal() {
        CustomerProjectionService projectionService = new CustomerProjectionService(customerViewRepository,
                basketRepository, checkpointRepository, new ObjectMapper());
        Basket basket = new Basket(42L);
        basket.addItem(7L, "Item", 12.5, 2);
        when(checkpointRepository.findById(42L)).thenReturn(Optional.empty());
        when(customerViewRepository.findById(42L)).thenReturn(Optional.empty());
        when(basketRepository.findById(42L)).thenReturn(Optional.of(basket));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", 42L);
        payload.put("name", "Updated Customer");
        payload.put("email", "updated@example.com");
        payload.put("phone", "0400000042");
        payload.put("contactMethod", "Email");
        payload.put("address", null);
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Customer", 42L, 3,
                "CustomerUpdatedEvent", Instant.now(), null, null, payload);

        assertTrue(projectionService.apply(event));

        ArgumentCaptor<CustomerView> viewCaptor = ArgumentCaptor.forClass(CustomerView.class);
        verify(customerViewRepository).save(viewCaptor.capture());
        assertEquals("Updated Customer", viewCaptor.getValue().getName());
        assertEquals(25.0, viewCaptor.getValue().getBasketTotal());
        verify(checkpointRepository).save(any(CustomerProjectionCheckpoint.class));
    }
}
