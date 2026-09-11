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

    public ProductView() {
    }

    public ProductView(Long productId, String name, ProductCategory category, Double price, String description) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public Double getPrice() { return price; }
    public String getDescription() { return description; }
}