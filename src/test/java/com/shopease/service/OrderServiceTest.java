package com.shopease.service;

import com.shopease.dto.request.OrderRequest;
import com.shopease.dto.response.OrderResponse;
import com.shopease.entity.*;
import com.shopease.exception.CartEmptyException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.exception.UnauthorizedAccessException;
import com.shopease.mapper.OrderMapper;
import com.shopease.repository.CartRepository;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.service.impl.OrderServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Product product;
    private Order order;
    private OrderRequest orderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("49.99"))
                .stockQuantity(10)
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();
        cartItem.setCart(cart);

        order = Order.builder()
                .id(1L)
                .orderNumber("ORD-12345678")
                .user(user)
                .items(new ArrayList<>())
                .subtotal(new BigDecimal("99.98"))
                .shippingCost(BigDecimal.ZERO)
                .tax(new BigDecimal("8.00"))
                .totalPrice(new BigDecimal("107.98"))
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();

        orderRequest = OrderRequest.builder()
                .shippingAddress("123 Main St")
                .shippingCity("New York")
                .shippingState("NY")
                .shippingZipCode("10001")
                .shippingCountry("USA")
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-12345678")
                .userId(1L)
                .totalPrice(new BigDecimal("107.98"))
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should create order from cart")
    void shouldCreateOrderFromCart() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        OrderResponse response = orderService.createOrder(orderRequest);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-12345678");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when cart is empty")
    void shouldThrowExceptionWhenCartIsEmpty() {
        cart.getItems().clear();

        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    @DisplayName("Should get order by ID")
    void shouldGetOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(1L)).thenReturn(true);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse response = orderService.getOrderById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when accessing another user's order")
    void shouldThrowExceptionWhenAccessingAnotherUsersOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(1L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    @DisplayName("Should cancel order")
    void shouldCancelOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(1L)).thenReturn(true);
        when(productRepository.findById(any())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        OrderResponse response = orderService.cancelOrder(1L);

        assertThat(response).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
