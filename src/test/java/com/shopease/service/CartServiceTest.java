package com.shopease.service;

import com.shopease.dto.request.CartItemRequest;
import com.shopease.dto.request.UpdateCartItemRequest;
import com.shopease.dto.response.CartResponse;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Product;
import com.shopease.entity.User;
import com.shopease.exception.InsufficientStockException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.CartMapper;
import com.shopease.repository.CartRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.service.impl.CartServiceImpl;
import com.shopease.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("49.99"))
                .stockQuantity(10)
                .active(true)
                .build();

        cartResponse = CartResponse.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .totalItems(0)
                .totalPrice(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should get cart for current user")
    void shouldGetCartForCurrentUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse response = cartService.getCart();

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should add item to cart")
    void shouldAddItemToCart() {
        CartItemRequest request = new CartItemRequest(1L, 2);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse response = cartService.addToCart(request);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        CartItemRequest request = new CartItemRequest(999L, 1);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowExceptionWhenInsufficientStock() {
        CartItemRequest request = new CartItemRequest(1L, 100);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Should remove item from cart")
    void shouldRemoveItemFromCart() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();
        cart.getItems().add(cartItem);

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);

        CartResponse response = cartService.removeFromCart(1L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should clear cart")
    void shouldClearCart() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart();

        verify(cartRepository).save(cart);
        assertThat(cart.getItems()).isEmpty();
    }
}
