package com.retail.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.retail.product.domain.Product;
import com.retail.product.domain.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @NonNull
    <S extends Product> S save(@NonNull S entity);

    List<Product> findByCategory(ProductCategory category);
    List<Product> findByNameContainingIgnoreCase(String name);
}
