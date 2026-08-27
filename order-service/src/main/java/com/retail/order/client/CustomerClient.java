package com.retail.order.client;

import com.retail.order.dto.BasketDto;
import com.retail.order.dto.CustomerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CustomerClient {

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerClient(RestClient restClient, @Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    public Optional<CustomerDto> getCustomerById(Long customerId) {
        try {
            CustomerDto customer = restClient.get()
                    .uri(customerServiceUrl + "/customer/id/" + customerId)
                    .retrieve()
                    .body(CustomerDto.class);
            return Optional.ofNullable(customer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<CustomerDto> getCustomerByEmail(String email) {
        try {
            CustomerDto customer = restClient.get()
                    .uri(customerServiceUrl + "/customer/email/" + email)
                    .retrieve()
                    .body(CustomerDto.class);
            return Optional.ofNullable(customer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<CustomerDto> getCustomerByPhone(String phone) {
        try {
            CustomerDto customer = restClient.get()
                    .uri(customerServiceUrl + "/customer/phone/" + phone)
                    .retrieve()
                    .body(CustomerDto.class);
            return Optional.ofNullable(customer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<BasketDto> getCustomerBasket(Long customerId) {
        try {
            BasketDto basket = restClient.get()
                    .uri(customerServiceUrl + "/customer/id/" + customerId + "/basket")
                    .retrieve()
                    .body(BasketDto.class);
            return Optional.ofNullable(basket);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void clearCustomerBasket(Long customerId) {
        try {
            restClient.delete()
                    .uri(customerServiceUrl + "/customer/id/" + customerId + "/basket/clear")
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
        }
    }
}
