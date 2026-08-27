package com.retail.customer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.customer.client.ProductClient;
import com.retail.customer.domain.Basket;
import com.retail.customer.domain.BasketItem;
import com.retail.customer.dto.BasketAddRequest;
import com.retail.customer.dto.BasketItemResponse;
import com.retail.customer.dto.BasketRemoveRequest;
import com.retail.customer.dto.BasketResponse;
import com.retail.customer.dto.ProductDto;
import com.retail.customer.exception.BadRequestException;
import com.retail.customer.exception.ResourceNotFoundException;
import com.retail.customer.repository.BasketRepository;
import com.retail.customer.repository.CustomerRepository;

@Service
@Transactional
public class BasketService {

    private final BasketRepository basketRepository;
    private final CustomerRepository customerRepository;
    private final ProductClient productClient;

    public BasketService(BasketRepository basketRepository, CustomerRepository customerRepository, ProductClient productClient) {
        this.basketRepository = basketRepository;
        this.customerRepository = customerRepository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public BasketResponse getBasketByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        Basket basket = basketRepository.findById(customerId)
                .orElseGet(() -> basketRepository.save(new Basket(customerId)));

        return toBasketResponse(basket);
    }

    public BasketResponse addItemToBasket(Long customerId, BasketAddRequest request) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        if (request.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Basket basket = basketRepository.findById(customerId)
                .orElseGet(() -> new Basket(customerId));

        // Fetch product info from Product Service
        Optional<ProductDto> productOpt = productClient.getProductById(request.getProductId());
        String productName = "Product " + request.getProductId();
        Double productPrice = 10.0; // fallback if product service offline/standalone

        if (productOpt.isPresent()) {
            productName = productOpt.get().getName();
            productPrice = productOpt.get().getPrice();
        }

        basket.addItem(request.getProductId(), productName, productPrice, request.getQuantity());
        Basket saved = basketRepository.save(basket);

        return toBasketResponse(saved);
    }

    public BasketResponse removeItemFromBasket(Long customerId, BasketRemoveRequest request) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        if (request.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Basket basket = basketRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket not found for customer: " + customerId));

        basket.removeItem(request.getProductId(), request.getQuantity());
        Basket saved = basketRepository.save(basket);

        return toBasketResponse(saved);
    }

    public void clearBasket(Long customerId) {
        basketRepository.findById(customerId).ifPresent(basket -> {
            basket.clear();
            basketRepository.save(basket);
        });
    }

    public BasketResponse toBasketResponse(Basket basket) {
        List<BasketItemResponse> itemResponses = basket.getItems()
                .stream()
                .map(this::toBasketItemResponse)
                .toList(); // unmodifiable list by contract

        return new BasketResponse(basket.getCustomerId(), itemResponses, basket.getTotal());
    }

    public BasketItemResponse toBasketItemResponse(BasketItem item) {
        return new BasketItemResponse(
                item.getProductId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
