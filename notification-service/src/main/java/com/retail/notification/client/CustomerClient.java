package com.retail.notification.client;

import com.retail.notification.config.CustomerServiceProperties;
import com.retail.notification.dto.CustomerDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class CustomerClient {

    private final RestClient restClient;
    private final String customerServiceUrl;

    public CustomerClient(RestClient restClient, CustomerServiceProperties customerServiceProperties) {
        this.restClient = restClient;
        this.customerServiceUrl = customerServiceProperties.getUrl();
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

    public List<CustomerDto> getAllCustomers() {
        try {
            List<CustomerDto> customers = restClient.get()
                    .uri(customerServiceUrl + "/customer")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CustomerDto>>() {});
            return customers != null ? customers : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
