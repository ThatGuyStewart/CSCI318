package com.retail.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.retail.common.ServicePropertyKeys;
import com.retail.common.ServiceUriBuilder;
import com.retail.order.dto.NotificationCreateRequest;

@Component
public class NotificationClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final String notificationCustomerBaseUri;

    public NotificationClient(RestClient restClient,
                              @Value("${" + ServicePropertyKeys.NOTIFICATION_SERVICE_URL + ":http://localhost:8084}") String notificationServiceUrl,
                              @Value("${" + ServicePropertyKeys.NOTIFICATION_SERVICE_CUSTOMER_PATH + ":/notification/customer/}") String notificationCustomerPath) {
        this.restClient = restClient;
        this.notificationCustomerBaseUri = ServiceUriBuilder.resourceBaseUri(notificationServiceUrl,
            notificationCustomerPath, ServicePropertyKeys.NOTIFICATION_SERVICE_URL,
            ServicePropertyKeys.NOTIFICATION_SERVICE_CUSTOMER_PATH);
    }

    public void sendNotificationToCustomer(Long customerId, String message) {
        try {
            restClient.post()
                    .uri(notificationCustomerBaseUri + "{customerId}", customerId)
                    .body(new NotificationCreateRequest(message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            logger.warn("Failed to send notification to customer {}", customerId, exception);
        }
    }

}
