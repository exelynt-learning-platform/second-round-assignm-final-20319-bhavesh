package com.shopease.service.impl;

import com.shopease.dto.request.OrderRequest;
import com.shopease.dto.response.OrderResponse;
import com.shopease.dto.response.PagedResponse;
import com.shopease.entity.*;
import com.shopease.exception.*;
import com.shopease.mapper.OrderMapper;
import com.shopease.repository.CartRepository;
import com.shopease.repository.OrderRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.service.OrderService;
import com.shopease.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("9.99");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final SecurityUtils securityUtils;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartRepository cartRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            OrderMapper orderMapper,
                            SecurityUtils securityUtils) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        if (cart.isEmpty()) {
            throw new CartEmptyException();
        }

        validateAndReserveStock(cart);

        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingZipCode(request.getShippingZipCode())
                .shippingCountry(request.getShippingCountry())
                .shippingPhone(request.getShippingPhone())
                .notes(request.getNotes())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);
        }

        BigDecimal subtotal = cart.getTotalPrice();
        BigDecimal shippingCost = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 
                ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setTax(tax);
        order.setTotalPrice(subtotal.add(shippingCost).add(tax));

        Order savedOrder = orderRepository.save(order);

        cart.clear();
        cartRepository.save(cart);

        logger.info("Order created: {} for user {}", savedOrder.getOrderNumber(), user.getEmail());
        
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        validateOrderAccess(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        validateOrderAccess(order);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getCurrentUserOrders() {
        Long userId = securityUtils.getCurrentUserId();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(int page, int size) {
        Long userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        
        Page<OrderResponse> responsePage = orderPage.map(orderMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findAll(pageable);
        
        Page<OrderResponse> responsePage = orderPage.map(orderMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByOrderStatus(status, pageable);
        
        Page<OrderResponse> responsePage = orderPage.map(orderMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = findOrderById(id);
        order.setOrderStatus(status);
        
        Order updatedOrder = orderRepository.save(order);
        logger.info("Order {} status updated to {}", order.getOrderNumber(), status);
        
        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findOrderById(id);
        validateOrderAccess(order);

        if (!order.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + order.getOrderStatus());
        }

        restoreStock(order);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} cancelled", order.getOrderNumber());
        
        return orderMapper.toResponse(savedOrder);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    private void validateOrderAccess(Order order) {
        if (!securityUtils.canAccessResource(order.getUser().getId())) {
            throw new UnauthorizedAccessException("order", order.getId());
        }
    }

    private void validateAndReserveStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProduct().getId()));

            if (!product.hasStock(item.getQuantity())) {
                throw new InsufficientStockException(
                        product.getName(),
                        item.getQuantity(),
                        product.getStockQuantity()
                );
            }

            product.decreaseStock(item.getQuantity());
            productRepository.save(product);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElse(null);
            
            if (product != null) {
                product.increaseStock(item.getQuantity());
                productRepository.save(product);
            }
        }
    }
}
