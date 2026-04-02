package com.shopease.service;

import com.shopease.dto.request.CartItemRequest;
import com.shopease.dto.request.UpdateCartItemRequest;
import com.shopease.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addToCart(CartItemRequest request);

    CartResponse updateCartItem(Long productId, UpdateCartItemRequest request);

    CartResponse removeFromCart(Long productId);

    void clearCart();

    int getCartItemCount();
}
