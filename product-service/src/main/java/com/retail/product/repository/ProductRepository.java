package com.retail.product.repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.retail.product.domain.Product;
import com.retail.product.domain.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @NonNull
    <S extends Product> S save(@NonNull S entity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    java.util.Optional<Product> findByIdForUpdate(Long id);

    List<Product> findByCategory(ProductCategory category);
    List<Product> findByNameContainingIgnoreCase(String name);
}
