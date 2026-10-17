package com.retail.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_views")
public class ProductView {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(nullable = false)
    private Double price;

    private String description;

    @Column(name = "aggregate_version")
    private Long aggregateVersion;

    public ProductView() {
    }

    public ProductView(Long productId, String name, ProductCategory category, Double price, String description) {
        this(productId, name, category, price, description, 0);
    }

    public ProductView(Long productId, String name, ProductCategory category, Double price, String description,
            long aggregateVersion) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.aggregateVersion = aggregateVersion;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public Double getPrice() { return price; }
    public String getDescription() { return description; }
    public long getAggregateVersion() { return aggregateVersion != null ? aggregateVersion : 0; }
}