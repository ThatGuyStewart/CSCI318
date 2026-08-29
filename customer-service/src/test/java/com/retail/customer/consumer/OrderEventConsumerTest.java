package com.retail.customer.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerViewRepository;
import com.retail.customer.repository.ProcessedOrderEventRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderEventConsumerTest {

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private CustomerViewRepository customerViewRepository;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @Test
    void clearsBasketForAPlacedOrderAndRecordsTheEvent() {
        DomainEventMessage event = placedOrderEvent(UUID.randomUUID());
        Basket basket = new Basket(5L);
        basket.addItem(9L, "Drill", 10.0, 2);
        when(processedOrderEventRepository.existsById(event.eventId())).thenReturn(false);
        when(basketRepository.findById(5L)).thenReturn(java.util.Optional.of(basket));
        when(basketRepository.save(any(Basket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new OrderEventConsumer(basketRepository, customerViewRepository, processedOrderEventRepository).onOrderEvent(event);

        verify(processedOrderEventRepository).save(any());
        verify(basketRepository).save(basket);
        assertThat(basket.getItems()).isEmpty();
        assertThat(basket.getTotal()).isZero();
    }

    @Test
    void ignoresDuplicateAndUnrelatedEvents() {
        DomainEventMessage duplicate = placedOrderEvent(UUID.randomUUID());
        when(processedOrderEventRepository.existsById(duplicate.eventId())).thenReturn(true);
        OrderEventConsumer consumer = new OrderEventConsumer(basketRepository, customerViewRepository,
                processedOrderEventRepository);

        consumer.onOrderEvent(duplicate);
        consumer.onOrderEvent(new DomainEventMessage(UUID.randomUUID(), "Order", 1L, 1,
                "OrderCancelledEvent", Instant.now(), null, null, Map.of("customerId", 5L)));

        verify(processedOrderEventRepository, never()).save(any());
        verify(basketRepository, never()).findById(any());
    }

    private DomainEventMessage placedOrderEvent(UUID eventId) {
        return new DomainEventMessage(eventId, "Order", 1L, 1, "OrderPlacedEvent", Instant.now(), null, null,
                Map.of("customerId", 5L));
    }
}