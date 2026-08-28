package com.retail.customer.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.retail.common.ServicePropertyKeys;
import com.retail.common.ServiceUriBuilder;
import com.retail.customer.dto.ProductDto;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final String productBaseUri;

    public ProductClient(RestClient restClient,
                         @Value("${" + ServicePropertyKeys.PRODUCT_SERVICE_URL + ":http://localhost:8082}") String productServiceUrl,
                         @Value("${" + ServicePropertyKeys.PRODUCT_SERVICE_PATH + ":/product/}") String productPath) {
        this.restClient = restClient;
        this.productBaseUri = ServiceUriBuilder.resourceBaseUri(productServiceUrl, productPath,
            ServicePropertyKeys.PRODUCT_SERVICE_URL, ServicePropertyKeys.PRODUCT_SERVICE_PATH);
    }

    public Optional<ProductDto> getProductById(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri(productBaseUri + "{productId}", productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
