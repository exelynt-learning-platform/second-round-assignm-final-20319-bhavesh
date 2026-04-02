package com.shopease.service;

import com.shopease.config.StripeConfig;
import com.shopease.dto.request.PaymentRequest;
import com.shopease.dto.response.PaymentResponse;
import com.shopease.entity.*;
import com.shopease.exception.PaymentException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.repository.OrderRepository;
import com.shopease.service.impl.PaymentServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Order order;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("49.99"))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .product(product)
                .productName("Test Product")
                .unitPrice(new BigDecimal("49.99"))
                .quantity(2)
                .subtotal(new BigDecimal("99.98"))
                .build();

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
        order.addItem(orderItem);

        paymentRequest = PaymentRequest.builder()
                .orderId(1L)
                .build();
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentRequest request = PaymentRequest.builder().orderId(999L).build();

        assertThatThrownBy(() -> paymentService.createCheckoutSession(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when order already paid")
    void shouldThrowExceptionWhenOrderAlreadyPaid() {
        order.setPaymentStatus(PaymentStatus.COMPLETED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createCheckoutSession(paymentRequest))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    @DisplayName("Should throw exception when order is cancelled")
    void shouldThrowExceptionWhenOrderCancelled() {
        order.setOrderStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createCheckoutSession(paymentRequest))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("Should get payment status")
    void shouldGetPaymentStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(securityUtils.canAccessResource(anyLong())).thenReturn(true);

        PaymentResponse response = paymentService.getPaymentStatus(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
