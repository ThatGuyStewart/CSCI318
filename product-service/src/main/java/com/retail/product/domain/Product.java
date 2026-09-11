package com.retail.product.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    private Long aggregateVersion;

    public Product() {
    }

    public Product(String name, ProductCategory category, Double price, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long nextAggregateVersion() {
        aggregateVersion = getAggregateVersion() + 1;
        return aggregateVersion;
    }

    public long getAggregateVersion() {
        return aggregateVersion != null ? aggregateVersion : 0;
    }

    public boolean hasAggregateVersion() {
        return aggregateVersion != null;
    }

    public void initializeAggregateVersion(long aggregateVersion) {
        if (this.aggregateVersion == null) {
            this.aggregateVersion = aggregateVersion;
        }
    }
}
