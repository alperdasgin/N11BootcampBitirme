package com.ecommerce.order_service.saga;

import com.ecommerce.order_service.client.PaymentClient;
import com.ecommerce.order_service.dto.PaymentRequestDto;
import com.ecommerce.order_service.dto.PaymentResponseDto;
import com.ecommerce.order_service.entity.*;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaListener Unit Testleri")
class OrderSagaListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentCardStore paymentCardStore;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderSagaListener sagaListener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sagaListener, "stockExchange", "stock.events.exchange");
        ReflectionTestUtils.setField(sagaListener, "stockReleaseKey", "order.stock.rejected");
    }

    // ─────────────────────────────────────────────────────────────
    // onStockReserved — Başarılı ödeme senaryosu
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Stok ayrıldı + Ödeme başarılı → Sipariş COMPLETED olur")
    void onStockReserved_withSuccessfulPayment_shouldCompleteOrder() {
        // ARRANGE
        Order order = buildOrderWithCard(1L, OrderStatus.CREATED);
        PaymentCardStore.CardInfo cardInfo = buildCard();

        when(orderRepository.findWithItemsAndDetails(1L)).thenReturn(Optional.of(order));
        when(paymentCardStore.take(1L)).thenReturn(cardInfo);
        when(paymentClient.processPayment(any(PaymentRequestDto.class)))
                .thenReturn(PaymentResponseDto.builder()
                        .success(true)
                        .transactionId("TXN-12345")
                        .orderId(1L)
                        .build());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        OrderSagaListener.StockReservedEvent event =
                new OrderSagaListener.StockReservedEvent(1L, "alper", "Stok ayrıldı");
        sagaListener.onStockReserved(event);

        // ASSERT: Siparişin COMPLETED yapıldığını doğrula
        verify(orderRepository, atLeastOnce()).save(argThat(o ->
                o.getStatus() == OrderStatus.COMPLETED
        ));
        // Stok serbest bırakma mesajı gönderilmemeli, sadece bildirim mesajı gönderilmeli
        verify(rabbitTemplate, never()).convertAndSend(
                eq("stock.events.exchange"), anyString(), any(Object.class));
        // Notification servisine tamamlandı mesajı gönderilmeli
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("order.exchange"), eq("order.completed"), any(Object.class));
    }

    @Test
    @DisplayName("Stok ayrıldı + Ödeme başarısız → Sipariş CANCELLED, stok geri bırakılır")
    void onStockReserved_withFailedPayment_shouldCancelOrderAndReleaseStock() {
        // ARRANGE
        Order order = buildOrderWithCard(1L, OrderStatus.CREATED);
        PaymentCardStore.CardInfo cardInfo = buildCard();

        when(orderRepository.findWithItemsAndDetails(1L)).thenReturn(Optional.of(order));
        when(paymentCardStore.take(1L)).thenReturn(cardInfo);
        when(paymentClient.processPayment(any(PaymentRequestDto.class)))
                .thenReturn(PaymentResponseDto.builder()
                        .success(false)
                        .message("Bakiye yetersiz")
                        .orderId(1L)
                        .build());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        OrderSagaListener.StockReservedEvent event =
                new OrderSagaListener.StockReservedEvent(1L, "alper", "Stok ayrıldı");
        sagaListener.onStockReserved(event);

        // ASSERT: Sipariş CANCELLED yapıldı
        verify(orderRepository, atLeastOnce()).save(argThat(o ->
                o.getStatus() == OrderStatus.CANCELLED
        ));
        // Stok serbest bırakma mesajı GÖNDERİLMELİ (doğru routing key ile)
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("stock.events.exchange"),
                eq("order.stock.release.requested"),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("Stok ayrıldı + Kart bilgisi RAM'de yok → Sipariş COMPLETED (kart bilgisi olmadan geçiş)")
    void onStockReserved_withNoCardInfo_shouldCompleteOrderWithoutPayment() {
        // ARRANGE: Kart bilgisi RAM'de bulunamıyor (take() null döner)
        Order order = buildOrderWithCard(1L, OrderStatus.CREATED);
        when(orderRepository.findWithItemsAndDetails(1L)).thenReturn(Optional.of(order));
        when(paymentCardStore.take(1L)).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        OrderSagaListener.StockReservedEvent event =
                new OrderSagaListener.StockReservedEvent(1L, "alper", "Stok ayrıldı");
        sagaListener.onStockReserved(event);

        // ASSERT: Ödeme servisine gidilmemeli, sipariş COMPLETED yapılmalı
        verify(paymentClient, never()).processPayment(any());
        verify(orderRepository, atLeastOnce()).save(argThat(o ->
                o.getStatus() == OrderStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("Sipariş zaten CANCELLED ise StockReserved eventi yok sayılır")
    void onStockReserved_whenOrderAlreadyCancelled_shouldSkipProcessing() {
        // ARRANGE: Sipariş zaten iptal edilmiş
        Order order = buildOrderWithCard(1L, OrderStatus.CANCELLED);
        when(orderRepository.findWithItemsAndDetails(1L)).thenReturn(Optional.of(order));

        // ACT
        OrderSagaListener.StockReservedEvent event =
                new OrderSagaListener.StockReservedEvent(1L, "alper", "Stok ayrıldı");
        sagaListener.onStockReserved(event);

        // ASSERT: Hiçbir ödeme veya kayıt işlemi yapılmamalı
        verify(paymentClient, never()).processPayment(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sipariş zaten COMPLETED ise StockReserved eventi yok sayılır (idempotency)")
    void onStockReserved_whenOrderAlreadyCompleted_shouldSkipProcessing() {
        // ARRANGE
        Order order = buildOrderWithCard(1L, OrderStatus.COMPLETED);
        when(orderRepository.findWithItemsAndDetails(1L)).thenReturn(Optional.of(order));

        // ACT
        OrderSagaListener.StockReservedEvent event =
                new OrderSagaListener.StockReservedEvent(1L, "alper", "Stok ayrıldı");
        sagaListener.onStockReserved(event);

        // ASSERT
        verify(paymentClient, never()).processPayment(any());
        verify(orderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // onStockRejected — Yetersiz stok senaryosu
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Stok yetersiz → Sipariş CANCELLED, kart bilgisi temizlenir")
    void onStockRejected_shouldCancelOrderAndCleanupCard() {
        // ARRANGE
        Order order = buildOrderWithCard(1L, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        OrderSagaListener.StockRejectedEvent event =
                new OrderSagaListener.StockRejectedEvent(1L, "alper", "Yetersiz stok", "Stok yok");
        sagaListener.onStockRejected(event);

        // ASSERT: Sipariş CANCELLED yapılmalı
        verify(orderRepository, times(1)).save(argThat(o ->
                o.getStatus() == OrderStatus.CANCELLED
        ));
        // Kart bilgisi temizlenmeli
        verify(paymentCardStore, times(1)).take(1L);
        // Ödeme servisine GİDİLMEMELİ
        verify(paymentClient, never()).processPayment(any());
    }

    @Test
    @DisplayName("Var olmayan sipariş için StockRejected eventi hata fırlatır")
    void onStockRejected_whenOrderNotFound_shouldThrowException() {
        // ARRANGE
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // ASSERT
        OrderSagaListener.StockRejectedEvent event =
                new OrderSagaListener.StockRejectedEvent(99L, "alper", "Stok yok", null);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sagaListener.onStockRejected(event));
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metotlar
    // ─────────────────────────────────────────────────────────────

    private Order buildOrderWithCard(Long id, OrderStatus status) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(10L)
                .productName("Wireless Mouse")
                .price(299.99)
                .quantity(1)
                .build();

        OrderDetails details = OrderDetails.builder()
                .firstName("Alper")
                .lastName("Daşgın")
                .city("İstanbul")
                .country("Turkey")
                .phone("05551234567")
                .email("alper@test.com")
                .streetAddress("Test Sokak No:1")
                .build();

        Order order = Order.builder()
                .id(id)
                .username("alper")
                .totalPrice(299.99)
                .status(status)
                .items(List.of(item))
                .orderDetails(details)
                .build();

        item.setOrder(order);
        details.setOrder(order);
        return order;
    }

    private PaymentCardStore.CardInfo buildCard() {
        PaymentCardStore.CardInfo card = new PaymentCardStore.CardInfo();
        card.setCardHolderName("Alper Daşgın");
        card.setCardNumber("5528790000000008");
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");
        return card;
    }
}
