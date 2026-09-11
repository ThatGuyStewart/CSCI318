package com.retail.customer.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.common.DomainEventPublisher;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.Customer;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerViewRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.ProcessedProductEventRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductEventConsumerTest {

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private ProcessedProductEventRepository processedProductEventRepository;

    @Mock
    private CustomerViewRepository customerViewRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerDomainEventRepository customerDomainEventRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Test
    void updatesMatchingBasketItemsAndRecordsTheEvent() {
        Basket matchingBasket = new Basket(1L);
        matchingBasket.addItem(7L, "Old name", 10.0, 2);
        Basket otherBasket = new Basket(2L);
        otherBasket.addItem(8L, "Other", 5.0, 3);
        DomainEventMessage event = productUpdatedEvent(UUID.randomUUID());
        when(basketRepository.findByItemsProductId(7L)).thenReturn(List.of(matchingBasket));
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(false);

        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));
        when(customerDomainEventRepository.findMaxAggregateVersion("Customer", 1L)).thenReturn(0L);

        consumer().onProductEvent(event);

        ArgumentCaptor<Basket> savedBasket = ArgumentCaptor.forClass(Basket.class);
        verify(processedProductEventRepository).save(any());
        verify(basketRepository).save(savedBasket.capture());
        assertThat(savedBasket.getValue().getItems().getFirst().getName()).isEqualTo("New name");
        assertThat(savedBasket.getValue().getItems().getFirst().getPrice()).isEqualTo(12.5);
        assertThat(savedBasket.getValue().getTotal()).isEqualTo(25.0);
        verify(basketRepository, never()).save(otherBasket);
        verify(basketRepository, never()).findAll();
        verify(domainEventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.events"), any(DomainEventMessage.class));
    }

    @Test
    void ignoresAnAlreadyProcessedEvent() {
        DomainEventMessage event = productUpdatedEvent(UUID.randomUUID());
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(true);

        consumer().onProductEvent(event);

        verify(processedProductEventRepository, never()).save(any());
        verify(basketRepository, never()).findByItemsProductId(7L);
        verify(basketRepository, never()).save(any());
    }

    @Test
    void appliesOnlyPresentProductFieldsAndPreservesTheOtherBasketFields() {
        Basket matchingBasket = new Basket(1L);
        matchingBasket.addItem(7L, "Existing name", 10.0, 2);
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
                "ProductUpdatedEvent", Instant.now(), null, null,
                Map.of("productId", 7L, "price", 12.5));
        when(basketRepository.findByItemsProductId(7L)).thenReturn(List.of(matchingBasket));
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(false);
        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));
        when(customerDomainEventRepository.findMaxAggregateVersion("Customer", 1L)).thenReturn(0L);

        consumer().onProductEvent(event);

        assertThat(matchingBasket.getItems().getFirst().getName()).isEqualTo("Existing name");
        assertThat(matchingBasket.getItems().getFirst().getPrice()).isEqualTo(12.5);
        assertThat(matchingBasket.getTotal()).isEqualTo(25.0);
        verify(domainEventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.events"), any(DomainEventMessage.class));
    }

    @Test
    void doesNotRecalculateWhenProductUpdateContainsNoChangedFields() {
        Basket matchingBasket = new Basket(1L);
        matchingBasket.addItem(7L, "Existing name", 10.0, 2);
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
                "ProductUpdatedEvent", Instant.now(), null, null, Map.of("productId", 7L));
        when(basketRepository.findByItemsProductId(7L)).thenReturn(List.of(matchingBasket));
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(false);

        consumer().onProductEvent(event);

        verify(basketRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any(), any());
    }

    @Test
    void publishesRecalculationWhenBasketPayloadContainsNullableFields() {
        Basket matchingBasket = new Basket(1L);
        matchingBasket.addItem(7L, null, 10.0, 2);
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 2,
            "ProductUpdatedEvent", Instant.now(), null, null, Map.of("productId", 7L, "price", 12.5));
        when(basketRepository.findByItemsProductId(7L)).thenReturn(List.of(matchingBasket));
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(false);
        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));
        when(customerDomainEventRepository.findMaxAggregateVersion("Customer", 1L)).thenReturn(0L);

        consumer().onProductEvent(event);

        verify(domainEventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.events"), any(DomainEventMessage.class));
    }

    @Test
    void removesDeletedProductFromMatchingBasketsAndPublishesRecalculation() {
        Basket matchingBasket = new Basket(1L);
        matchingBasket.addItem(7L, "Old name", 10.0, 2);
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Product", 7L, 3,
                "ProductDeletedEvent", Instant.now(), null, null, Map.of("productId", 7L));
        when(basketRepository.findByItemsProductId(7L)).thenReturn(List.of(matchingBasket));
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(false);
        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        when(customerRepository.findById(1L)).thenReturn(java.util.Optional.of(customer));
        when(customerDomainEventRepository.findMaxAggregateVersion("Customer", 1L)).thenReturn(0L);

        consumer().onProductEvent(event);

        assertThat(matchingBasket.getItems()).isEmpty();
        assertThat(matchingBasket.getTotal()).isEqualTo(0.0);
        verify(domainEventPublisher).publish(org.mockito.ArgumentMatchers.eq("customer.events"), any(DomainEventMessage.class));
    }

    private DomainEventMessage productUpdatedEvent(UUID eventId) {
        return new DomainEventMessage(eventId, "Product", 7L, 2, "ProductUpdatedEvent", Instant.now(), null, null,
                Map.of("productId", 7L, "name", "New name", "price", 12.5));
    }

    private ProductEventConsumer consumer() {
        return new ProductEventConsumer(basketRepository, processedProductEventRepository, customerViewRepository,
                customerRepository, customerDomainEventRepository, new ObjectMapper(), domainEventPublisher,
                "customer.events");
    }
}