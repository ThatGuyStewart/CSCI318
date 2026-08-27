package com.retail.customer.client;

import com.retail.customer.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class ProductClient {

    private final RestClient restClient;
    private final String productServiceUrl;

    public ProductClient(RestClient restClient, @Value("${product.service.url:http://localhost:8082}") String productServiceUrl) {
        this.restClient = restClient;
        this.productServiceUrl = productServiceUrl;
    }

    public Optional<ProductDto> getProductById(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri(productServiceUrl + "/product/" + productId)
                    .retrieve()
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
