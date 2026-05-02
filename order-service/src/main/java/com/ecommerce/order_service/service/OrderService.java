package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.CreateOrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> findAllOrders();

    OrderResponse getOrderById(Long id);

    List<OrderResponse> findOrdersByUsername(String username);

    void updateOrderStatus(Long orderId, String status);
}
