package com.ecommerce.order_service.saga;

import com.ecommerce.order_service.client.PaymentClient;
import com.ecommerce.order_service.dto.PaymentRequestDto;
import com.ecommerce.order_service.dto.PaymentResponseDto;
import com.ecommerce.order_service.dto.StockReserveRequestedEvent;
import com.ecommerce.order_service.entity.*;
import com.ecommerce.order_service.repository.OrderRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Saga Choreography Listener
 *
 * Akış:
 *  1. Order oluşturulur → StockReserveRequested yayınlanır
 *  2. Stock Service stoku kontrol eder:
 *     - Stok yeterliyse → StockReserved yayınlanır (bu listener yakalar)
 *     - Stok yetersizse → StockRejected yayınlanır (bu listener yakalar)
 *  3. StockReserved alındığında → Payment Service çağrılır
 *     - Ödeme başarılıysa → Order COMPLETED
 *     - Ödeme başarısızsa → StockReleaseRequested yayınlanır + Order CANCELLED
 *  4. StockRejected alındığında → Order CANCELLED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final PaymentCardStore paymentCardStore;
    private final PaymentClient paymentClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${stock.rabbit.exchange}") private String stockExchange;
    @Value("${stock.rabbit.rejectedRoutingKey}") private String stockReleaseKey;

    // ── Adım 2a: Stok rezerve edildi → Ödeme başlat ─────────────────────────
    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockReservedQueue}")
    public void onStockReserved(StockReservedEvent event) {
        log.info("[SAGA] StockReserved alındı. orderId={}", event.getOrderId());

        Order order = orderRepository.findWithItemsAndDetails(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + event.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("[SAGA] Sipariş zaten terminal durumda, atlanıyor. orderId={}", event.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.STOCK_DEDUCTED);
        orderRepository.save(order);

        // Kart bilgisini RAM'den al
        PaymentCardStore.CardInfo card = paymentCardStore.take(order.getId());

        if (card == null) {
            log.warn("[SAGA] Kart bilgisi bulunamadı. orderId={} — Ödeme atlanıyor, COMPLETED yapılıyor.", order.getId());
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            return;
        }

        // Payment Service'e istek gönder (FeignClient + Circuit Breaker)
        PaymentRequestDto paymentRequest = buildPaymentRequest(order, card);
        log.info("[SAGA] Payment Service'e istek gönderiliyor. orderId={}", order.getId());

        PaymentResponseDto paymentResponse = paymentClient.processPayment(paymentRequest);

        if (paymentResponse.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            log.info("[SAGA] Ödeme başarılı, sipariş COMPLETED. orderId={}, transactionId={}",
                    order.getId(), paymentResponse.getTransactionId());
                    
            // E-Posta atılması için Notification Service'e mesaj gönder
            try {
                java.util.Map<String, Object> eventData = new java.util.HashMap<>();
                eventData.put("orderId", order.getId());
                eventData.put("email", order.getOrderDetails() != null ? order.getOrderDetails().getEmail() : "");
                eventData.put("firstName", order.getOrderDetails() != null ? order.getOrderDetails().getFirstName() : order.getUsername());
                eventData.put("totalPrice", order.getTotalPrice());
                rabbitTemplate.convertAndSend("order.exchange", "order.completed", eventData);
                log.info("[SAGA] OrderCompletedEvent yayınlandı. orderId={}", order.getId());
            } catch (Exception e) {
                log.error("Bildirim servisine mesaj gönderilirken hata oluştu: {}", e.getMessage());
            }
            
        } else {
            // Ödeme başarısız → stok geri bırak + iptal
            log.warn("[SAGA] Ödeme başarısız. orderId={}, reason={}", order.getId(), paymentResponse.getMessage());
            releaseStock(order);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }

    // ── Adım 2b: Stok yetersiz → İptal ──────────────────────────────────────
    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockRejectedQueue}")
    public void onStockRejected(StockRejectedEvent event) {
        log.warn("[SAGA] StockRejected alındı. orderId={}, reason={}", event.getOrderId(), event.getReason());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + event.getOrderId()));

        paymentCardStore.take(order.getId()); // Kart bilgisini temizle
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[SAGA] Sipariş CANCELLED (yetersiz stok). orderId={}", order.getId());
    }

    // ── Stok serbest bırak ────────────────────────────────────────────────────
    private void releaseStock(Order order) {
        try {
            StockReserveRequestedEvent releaseEvent = new StockReserveRequestedEvent(
                    order.getId(), order.getUsername(),
                    order.getItems().stream()
                            .map(i -> new StockReserveRequestedEvent.Item(i.getProductId(), i.getQuantity()))
                            .collect(Collectors.toList())
            );
            rabbitTemplate.convertAndSend(stockExchange, "order.stock.release.requested", releaseEvent);
            log.info("[SAGA] Stok serbest bırakma eventi yayınlandı. orderId={}", order.getId());
        } catch (Exception e) {
            log.error("[SAGA] Stok serbest bırakma hatası: {}", e.getMessage());
        }
    }

    // ── Payment Request oluştur ───────────────────────────────────────────────
    private PaymentRequestDto buildPaymentRequest(Order order, PaymentCardStore.CardInfo card) {
        OrderDetails details = order.getOrderDetails();

        return PaymentRequestDto.builder()
                .orderId(order.getId())
                .username(order.getUsername())
                .amount(order.getTotalPrice())
                .firstName(details != null ? details.getFirstName() : "")
                .lastName(details != null ? details.getLastName() : "")
                .email(details != null ? details.getEmail() : "")
                .phone(details != null ? details.getPhone() : "")
                .address(details != null ? details.getStreetAddress() : "")
                .city(details != null ? details.getCity() : "")
                .country(details != null ? details.getCountry() : "")
                .card(PaymentRequestDto.Card.builder()
                        .cardHolderName(card.getCardHolderName())
                        .cardNumber(card.getCardNumber())
                        .expireMonth(card.getExpireMonth())
                        .expireYear(card.getExpireYear())
                        .cvc(card.getCvc())
                        .build())
                .items(order.getItems().stream()
                        .map(i -> PaymentRequestDto.Item.builder()
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .price(i.getPrice())
                                .quantity(i.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Inner DTO sınıfları ───────────────────────────────────────────────────

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
        private String reason;   // StockEventPayloads ile eşleşiyor
        private String message;  // backward compat
    }
}
