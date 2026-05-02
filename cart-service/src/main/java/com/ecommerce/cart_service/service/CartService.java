package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.AddToCartRequest;
import com.ecommerce.cart_service.dto.CartResponse;

public interface CartService {

    CartResponse addToCart(String username, AddToCartRequest request);

    CartResponse getCart(String username);

    CartResponse removeFromCart(String username, Long productId);

    CartResponse updateQuantity(String username, Long productId, Integer quantity);

    void clearCart(String username);
}
