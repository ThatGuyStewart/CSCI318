package com.retail.order.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.retail.common.ServicePropertyKeys;
import com.retail.common.ServiceUriBuilder;
import com.retail.order.dto.BasketDto;
import com.retail.order.dto.CustomerDto;

@Component
public class CustomerClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerClient.class);

    private final RestClient restClient;
    private final String customerBaseUri;

    public CustomerClient(RestClient restClient,
                          @Value("${" + ServicePropertyKeys.CUSTOMER_SERVICE_URL + ":http://localhost:8081}") String customerServiceUrl,
                          @Value("${" + ServicePropertyKeys.CUSTOMER_SERVICE_PATH + ":/customer/}") String customerPath) {
        this.restClient = restClient;
        this.customerBaseUri = ServiceUriBuilder.resourceBaseUri(customerServiceUrl, customerPath,
            ServicePropertyKeys.CUSTOMER_SERVICE_URL, ServicePropertyKeys.CUSTOMER_SERVICE_PATH);
    }

    public Optional<CustomerDto> getCustomerById(Long customerId) {
        try {
            CustomerDto customer = restClient.get()
                    .uri(customerBaseUri + "{customerId}", customerId)
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
                    .uri(customerBaseUri + "email/{email}", email)
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
                    .uri(customerBaseUri + "phone/{phone}", phone)
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
                    .uri(customerBaseUri + "{customerId}/basket", customerId)
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
                    .uri(customerBaseUri + "{customerId}/basket/clear", customerId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            logger.warn("Failed to clear basket for customer {}", customerId, exception);
        }
    }

}
