package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Order", description = "Sipariş işlemleri")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Yeni sipariş oluştur")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/all")
    @Operation(summary = "Tüm siparişleri listele")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sipariş detayı")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Kullanıcının siparişleri")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable String username) {
        return ResponseEntity.ok(orderService.findOrdersByUsername(username));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service çalışıyor");
    }
}