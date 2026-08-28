package com.retail.notification.client;

import com.retail.common.ServiceUriBuilder;
import com.retail.common.ServicePropertyKeys;
import com.retail.notification.config.CustomerServiceProperties;
import com.retail.notification.dto.CustomerDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CustomerClient {

    private final RestClient restClient;
    @NonNull
    private final String customerCollectionUri;
    private final String customerBaseUri;

    public CustomerClient(RestClient restClient, CustomerServiceProperties customerServiceProperties) {
        this.restClient = restClient;
        String customerServiceUrl = Objects.requireNonNull(customerServiceProperties.getUrl(),
            ServicePropertyKeys.CUSTOMER_SERVICE_URL + " must not be null");
        String customerPath = Objects.requireNonNull(customerServiceProperties.getPath(),
            ServicePropertyKeys.CUSTOMER_SERVICE_PATH + " must not be null");
        this.customerCollectionUri = ServiceUriBuilder.resourceUri(customerServiceUrl, customerPath,
            ServicePropertyKeys.CUSTOMER_SERVICE_URL, ServicePropertyKeys.CUSTOMER_SERVICE_PATH);
        this.customerBaseUri = customerCollectionUri + "/";
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

    public List<CustomerDto> getAllCustomers() {
        try {
            List<CustomerDto> customers = restClient.get()
                .uri(customerCollectionUri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CustomerDto>>() {});
            return customers != null ? customers : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
