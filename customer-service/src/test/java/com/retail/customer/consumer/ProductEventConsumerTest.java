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

import com.retail.common.DomainEventMessage;
import com.retail.customer.domain.Basket;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.ProcessedProductEventRepository;
import com.retail.customer.repository.CustomerViewRepository;

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

        new ProductEventConsumer(basketRepository, processedProductEventRepository, customerViewRepository).onProductEvent(event);

        ArgumentCaptor<Basket> savedBasket = ArgumentCaptor.forClass(Basket.class);
        verify(processedProductEventRepository).save(any());
        verify(basketRepository).save(savedBasket.capture());
        assertThat(savedBasket.getValue().getItems().getFirst().getName()).isEqualTo("New name");
        assertThat(savedBasket.getValue().getItems().getFirst().getPrice()).isEqualTo(12.5);
        assertThat(savedBasket.getValue().getTotal()).isEqualTo(25.0);
        verify(basketRepository, never()).save(otherBasket);
        verify(basketRepository, never()).findAll();
    }

    @Test
    void ignoresAnAlreadyProcessedEvent() {
        DomainEventMessage event = productUpdatedEvent(UUID.randomUUID());
        when(processedProductEventRepository.existsById(event.eventId())).thenReturn(true);

        new ProductEventConsumer(basketRepository, processedProductEventRepository, customerViewRepository).onProductEvent(event);

        verify(processedProductEventRepository, never()).save(any());
        verify(basketRepository, never()).findByItemsProductId(7L);
        verify(basketRepository, never()).save(any());
    }

    private DomainEventMessage productUpdatedEvent(UUID eventId) {
        return new DomainEventMessage(eventId, "Product", 7L, 2, "ProductUpdatedEvent", Instant.now(), null, null,
                Map.of("productId", 7L, "name", "New name", "price", 12.5));
    }
}