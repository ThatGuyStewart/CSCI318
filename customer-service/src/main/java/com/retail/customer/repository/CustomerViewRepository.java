package com.retail.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.retail.customer.domain.CustomerView;

public interface CustomerViewRepository extends JpaRepository<CustomerView, Long> {
    Optional<CustomerView> findByEmail(String email);
    Optional<CustomerView> findByPhone(String phone);
    List<CustomerView> findByAddress_StateIgnoreCase(String state);
    List<CustomerView> findByAddress_CountryIgnoreCase(String country);
    List<CustomerView> findByAddress_Postcode(Integer postcode);
}