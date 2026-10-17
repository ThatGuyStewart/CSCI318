package com.retail.order.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.common.DomainEventMessage;
import com.retail.order.domain.Address;
import com.retail.order.domain.OrderItem;
import com.retail.order.domain.OrderProjectionCheckpoint;
import com.retail.order.domain.OrderStatus;
import com.retail.order.domain.OrderSummaryView;
import com.retail.order.dto.AddressDto;
import com.retail.order.dto.OrderItemEventPayload;
import com.retail.order.repository.OrderProjectionCheckpointRepository;
import com.retail.order.repository.OrderSummaryViewRepository;

@Service
public class OrderProjectionService {

    private static final String ORDER_AGGREGATE_TYPE = "Order";
    private static final List<String> ORDER_SNAPSHOT_EVENTS = List.of(
            "OrderPlacedEvent", "BasketUsedForOrderEvent", "OrderStatusChangedEvent",
            "OrderCancelledEvent", "OrderCancelFailedEvent");

    private final OrderSummaryViewRepository summaryRepository;
    private final OrderProjectionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;

    public OrderProjectionService(OrderSummaryViewRepository summaryRepository,
            OrderProjectionCheckpointRepository checkpointRepository, ObjectMapper objectMapper) {
        this.summaryRepository = summaryRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean apply(DomainEventMessage event) {
        Objects.requireNonNull(event, "Order event is required");
        if (!ORDER_AGGREGATE_TYPE.equals(event.aggregateType())) {
            throw new IllegalArgumentException("Unexpected aggregate type for Order projection");
        }
        Long orderId = Objects.requireNonNull(event.aggregateId(), "Order event aggregate ID is required");
        if (event.aggregateVersion() < 1) {
            throw new IllegalArgumentException("Order event must have a positive aggregate version");
        }

        OrderProjectionCheckpoint checkpoint = checkpointRepository.findById(orderId).orElse(null);
        if (checkpoint != null && event.aggregateVersion() <= checkpoint.getAggregateVersion()) {
            return false;
        }
        if (!ORDER_SNAPSHOT_EVENTS.contains(event.eventType())) {
            throw new IllegalArgumentException("Unsupported Order event type: " + event.eventType());
        }

        OrderSummaryView existing = summaryRepository.findById(orderId).orElse(null);
        summaryRepository.save(Objects.requireNonNull(toSummaryView(event, existing)));
        checkpointRepository.save(new OrderProjectionCheckpoint(orderId, event.aggregateVersion()));
        return true;
    }

    private OrderSummaryView toSummaryView(DomainEventMessage event, OrderSummaryView existing) {
        JsonNode payload = objectMapper.valueToTree(event.payload());
        if (payload == null || !payload.isObject()
                || !payload.path("orderId").isIntegralNumber()
                || payload.path("orderId").longValue() != event.aggregateId()
                || !payload.path("customerId").isIntegralNumber()
                || !payload.path("total").isNumber()
                || !payload.path("status").isTextual()
                || !payload.path("items").isArray()) {
            throw new IllegalArgumentException("Order event payload is missing required projection fields");
        }

        OrderStatus status;
        try {
            status = OrderStatus.valueOf(payload.path("status").asText());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Order event contains an unknown status", exception);
        }

        AddressDto addressDto;
        try {
            addressDto = objectMapper.treeToValue(payload.get("address"), AddressDto.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Order event contains an invalid address", exception);
        }
        if (addressDto == null) {
            throw new IllegalArgumentException("Order event is missing its address");
        }

        List<OrderItem> items = new ArrayList<>();
        for (JsonNode itemNode : payload.path("items")) {
            try {
                OrderItemEventPayload item = objectMapper.treeToValue(itemNode, OrderItemEventPayload.class);
                if (item.productId() == null || item.quantity() == null || item.quantity() <= 0
                        || item.price() == null || item.subtotal() == null) {
                    throw new IllegalArgumentException("Order event contains an incomplete item");
                }
                items.add(new OrderItem(item.productId(), item.name(), item.price(), item.quantity(), item.subtotal()));
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalArgumentException("Order event contains an invalid item", exception);
            }
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order event must contain at least one item");
        }

        LocalDateTime createdAt = existing != null ? existing.getCreatedAt()
                : LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        return new OrderSummaryView(event.aggregateId(), payload.path("customerId").longValue(),
                payload.path("total").doubleValue(), status, createdAt, toAddress(addressDto), items);
    }

    private Address toAddress(AddressDto address) {
        return new Address(address.getUnitNumber(), address.getStreetNumber(), address.getStreet(),
                address.getSuburb(), address.getCity(), address.getPostcode(), address.getState(),
                address.getCountry());
    }
}
