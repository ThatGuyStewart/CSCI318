package com.retail.product.dto;

import com.retail.product.domain.ProductCategory;
import jakarta.validation.constraints.Positive;

public class ProductUpdateRequest {
    private String name;
    private ProductCategory category;
    @Positive
    private Double price;
    private String description;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, ProductCategory category, Double price, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
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
}
