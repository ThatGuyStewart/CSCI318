package com.retail.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.retail.order.dto.NotificationCreateRequest;

@Component
public class NotificationClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final String notificationServiceUrl;

    public NotificationClient(RestClient restClient, @Value("${notification.service.url:http://localhost:8084}") String notificationServiceUrl) {
        this.restClient = restClient;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void sendNotificationToCustomer(Long customerId, String message) {
        try {
            restClient.post()
                    .uri(notificationServiceUrl + "/notification/customer/id/" + customerId)
                    .body(new NotificationCreateRequest(message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            logger.warn("Failed to send notification to customer {}", customerId, exception);
        }
    }
}
