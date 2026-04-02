package com.shopease.service;

import com.shopease.dto.request.OrderRequest;
import com.shopease.dto.response.OrderResponse;
import com.shopease.dto.response.PagedResponse;
import com.shopease.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByOrderNumber(String orderNumber);

    List<OrderResponse> getCurrentUserOrders();

    PagedResponse<OrderResponse> getUserOrders(int page, int size);

    PagedResponse<OrderResponse> getAllOrders(int page, int size);

    PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, int page, int size);

    OrderResponse updateOrderStatus(Long id, OrderStatus status);

    OrderResponse cancelOrder(Long id);
}
