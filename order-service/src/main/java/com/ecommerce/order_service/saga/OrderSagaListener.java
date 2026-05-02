package com.ecommerce.order_service.saga;

import com.ecommerce.order_service.entity.*;
import com.ecommerce.order_service.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderRepository orderRepository;

    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockReservedQueue}")
    public void onStockReserved(StockReservedEvent event) {
        log.info("[SAGA] StockReserved alındı. orderId={}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) return;

        // Ödeme servisi entegrasyonu burada yapılacak
        // Şimdilik direkt COMPLETED yapıyoruz
        order.setStatus(OrderStatus.STOCK_DEDUCTED);
        orderRepository.save(order);
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        log.info("[SAGA] Sipariş COMPLETED. orderId={}", order.getId());
    }

    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockRejectedQueue}")
    public void onStockRejected(StockRejectedEvent event) {
        log.warn("[SAGA] StockRejected alındı. orderId={}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı"));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[SAGA] Sipariş CANCELLED. orderId={}", order.getId());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockReservedEvent {
        private Long orderId;
        private String username;
        private String message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockRejectedEvent {
        private Long orderId;
        private String username;
        private String message;
    }
}