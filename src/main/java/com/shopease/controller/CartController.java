package com.shopease.controller;

import com.shopease.dto.request.CartItemRequest;
import com.shopease.dto.request.UpdateCartItemRequest;
import com.shopease.dto.response.ApiResponse;
import com.shopease.dto.response.CartResponse;
import com.shopease.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get cart", description = "Returns the current user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse response = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item", description = "Updates the quantity of an item in the cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes a product from the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(@PathVariable Long productId) {
        CartResponse response = cartService.removeFromCart(productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count", description = "Returns the total number of items in the cart")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount() {
        int count = cartService.getCartItemCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
