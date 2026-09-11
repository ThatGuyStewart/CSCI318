package com.retail.customer.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "baskets")
public class Basket {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "basket_items", joinColumns = @JoinColumn(name = "customer_id"))
    private List<BasketItem> items = new ArrayList<>();

    private Double total = 0.0;

    public Basket() {
    }

    public Basket(Long customerId) {
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.total = 0.0;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<BasketItem> getItems() {
        return items;
    }

    public void setItems(List<BasketItem> items) {
        this.items = items;
        recalculateTotal();
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public void addItem(Long productId, String name, Double price, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        Optional<BasketItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            BasketItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            if (name != null) item.setName(name);
            if (price != null) item.setPrice(price);
            item.recalculateSubtotal();
        } else {
            items.add(new BasketItem(productId, name, price, quantity));
        }
        recalculateTotal();
    }

    public void removeItem(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return;
        }
        Optional<BasketItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            BasketItem item = existing.get();
            int newQuantity = item.getQuantity() - quantity;
            if (newQuantity < 1) {
                items.remove(item);
            } else {
                item.setQuantity(newQuantity);
                item.recalculateSubtotal();
            }
            recalculateTotal();
        }
    }

    public void clear() {
        items.clear();
        total = 0.0;
    }

    public void recalculateTotal() {
        this.total = items.stream()
                .mapToDouble(item -> {
                    item.recalculateSubtotal();
                    return item.getSubtotal();
                })
                .sum();
    }
}
