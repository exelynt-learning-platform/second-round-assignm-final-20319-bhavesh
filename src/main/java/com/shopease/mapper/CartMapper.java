package com.shopease.mapper;

import com.shopease.dto.response.CartItemResponse;
import com.shopease.dto.response.CartResponse;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }
        
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(cart.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()))
                .totalItems(cart.getTotalItems())
                .totalPrice(cart.getTotalPrice())
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }
        
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .availableStock(item.getProduct().getStockQuantity())
                .build();
    }
}
