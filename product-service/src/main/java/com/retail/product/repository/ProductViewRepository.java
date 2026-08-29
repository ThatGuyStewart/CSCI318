package com.retail.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.product.domain.ProductCategory;
import com.retail.product.domain.ProductView;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    List<ProductView> findByCategory(ProductCategory category);
    List<ProductView> findByNameContainingIgnoreCase(String name);
}