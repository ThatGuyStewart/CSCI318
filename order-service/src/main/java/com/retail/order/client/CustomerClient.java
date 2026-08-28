package com.retail.order.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.retail.order.dto.BasketDto;
import com.retail.order.dto.CustomerDto;

@Component
public class CustomerClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerClient.class);

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerClient(RestClient restClient, @Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    public Optional<CustomerDto> getCustomerById(Long customerId) {
        try {
            CustomerDto customer = restClient.get()
                    .uri(customerServiceUrl + "/customer/" + customerId)
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
                    .uri(customerServiceUrl + "/customer/" + customerId + "/basket")
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
                    .uri(customerServiceUrl + "/customer/" + customerId + "/basket/clear")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            logger.warn("Failed to clear basket for customer {}", customerId, exception);
        }
    }
}
