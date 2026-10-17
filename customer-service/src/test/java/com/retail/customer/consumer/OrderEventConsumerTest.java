package com.retail.customer.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.common.DomainEventMessage;
import com.retail.common.UnsupportedEventSchemaVersionException;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.Customer;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerDomainEventRepository;
import com.retail.customer.repository.CustomerRepository;
import com.retail.customer.repository.ProcessedOrderEventRepository;
import com.retail.customer.service.CustomerEventRecorder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderEventConsumerTest {

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerDomainEventRepository customerDomainEventRepository;

    @Mock
    private CustomerEventRecorder customerEventRecorder;

    @Test
    void clearsBasketForAnOrderUsingTheBasketAndRecordsTheEvent() {
        DomainEventMessage event = placedOrderEvent(UUID.randomUUID());
        Basket basket = new Basket(5L);
        basket.addItem(9L, "Drill", 10.0, 2);
        when(processedOrderEventRepository.existsById(event.eventId())).thenReturn(false);
        when(basketRepository.findById(5L)).thenReturn(java.util.Optional.of(basket));
        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Customer customer = new Customer();
        customer.setCustomerId(5L);
        customer.initializeAggregateVersion(0);
        when(customerRepository.findByIdForUpdate(5L)).thenReturn(java.util.Optional.of(customer));

        new OrderEventConsumer(basketRepository, processedOrderEventRepository, customerRepository,
                customerDomainEventRepository, customerEventRecorder).onOrderEvent(event);

        verify(processedOrderEventRepository).save(any());
        verify(basketRepository).save(basket);
        verify(customerEventRecorder).record(org.mockito.ArgumentMatchers.eq(customer),
                org.mockito.ArgumentMatchers.eq("BasketClearedAfterOrderEvent"), any(), eq(event.eventId()));
        assertThat(basket.getItems()).isEmpty();
        assertThat(basket.getTotal()).isZero();
    }

    @Test
    void ignoresDuplicateAndUnrelatedEvents() {
        DomainEventMessage duplicate = placedOrderEvent(UUID.randomUUID());
        when(processedOrderEventRepository.existsById(duplicate.eventId())).thenReturn(true);
        OrderEventConsumer consumer = new OrderEventConsumer(basketRepository, processedOrderEventRepository,
                customerRepository, customerDomainEventRepository, customerEventRecorder);

        consumer.onOrderEvent(duplicate);
        consumer.onOrderEvent(new DomainEventMessage(UUID.randomUUID(), "Order", 1L, 1,
                "OrderPlacedEvent", Instant.now(), null, null, Map.of("customerId", 5L)));

        verify(processedOrderEventRepository, never()).save(any());
        verify(basketRepository, never()).findById(any());
    }

    @Test
    void rejectsUnsupportedEventSchemaVersions() {
        DomainEventMessage event = new DomainEventMessage(UUID.randomUUID(), "Order", 1L, 1,
                "BasketUsedForOrderEvent", Instant.now(), null, null, Map.of("customerId", 5L), 2);
        OrderEventConsumer consumer = new OrderEventConsumer(basketRepository, processedOrderEventRepository,
                customerRepository, customerDomainEventRepository, customerEventRecorder);

        assertThrows(UnsupportedEventSchemaVersionException.class, () -> consumer.onOrderEvent(event));
    }

    private DomainEventMessage placedOrderEvent(UUID eventId) {
        return new DomainEventMessage(eventId, "Order", 1L, 1, "BasketUsedForOrderEvent", Instant.now(), null, null,
                Map.of("customerId", 5L));
    }
}