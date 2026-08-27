package com.retail.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.customer.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    List<Customer> findByAddress_StateIgnoreCase(String state);
    List<Customer> findByAddress_CountryIgnoreCase(String country);
    List<Customer> findByAddress_Postcode(Integer postcode);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
