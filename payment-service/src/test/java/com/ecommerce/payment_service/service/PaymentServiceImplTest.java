package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.strategy.PaymentStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Testleri")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // ─────────────────────────────────────────────────────────────
    // processPayment — Başarılı ödeme senaryosu
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Başarılı ödeme → SUCCESS statüsüyle veritabanına kaydedilir")
    void processPayment_withSuccess_shouldSavePaymentAsSuccess() {
        // ARRANGE
        PaymentRequest request = buildRequest(1L, 299.99);
        PaymentResponse strategyResponse = PaymentResponse.builder()
                .success(true)
                .transactionId("TXN-12345")
                .message("Ödeme başarılı")
                .orderId(1L)
                .build();

        when(paymentStrategy.pay(any(PaymentRequest.class))).thenReturn(strategyResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        PaymentResponse response = paymentService.processPayment(request);

        // ASSERT — Dönen response doğruluğu
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTransactionId()).isEqualTo("TXN-12345");
        assertThat(response.getOrderId()).isEqualTo(1L);

        // ASSERT — Veritabanına kaydedilen Payment doğruluğu
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo("SUCCESS");
        assertThat(savedPayment.getTransactionId()).isEqualTo("TXN-12345");
        assertThat(savedPayment.getErrorMessage()).isNull();
        assertThat(savedPayment.getOrderId()).isEqualTo(1L);
        assertThat(savedPayment.getAmount()).isEqualTo(299.99);
    }

    @Test
    @DisplayName("Başarısız ödeme → FAILED statüsüyle hata mesajı veritabanına kaydedilir")
    void processPayment_withFailure_shouldSavePaymentAsFailed() {
        // ARRANGE
        PaymentRequest request = buildRequest(2L, 1500.0);
        PaymentResponse strategyResponse = PaymentResponse.builder()
                .success(false)
                .transactionId(null)
                .message("Bakiye yetersiz")
                .orderId(2L)
                .build();

        when(paymentStrategy.pay(any(PaymentRequest.class))).thenReturn(strategyResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        PaymentResponse response = paymentService.processPayment(request);

        // ASSERT — Dönen response
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Bakiye yetersiz");

        // ASSERT — Veritabanına FAILED olarak kaydedilmeli
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment savedPayment = captor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo("FAILED");
        assertThat(savedPayment.getErrorMessage()).isEqualTo("Bakiye yetersiz");
        assertThat(savedPayment.getTransactionId()).isNull();
    }

    // ─────────────────────────────────────────────────────────────
    // Exception yönetimi
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PaymentStrategy exception fırlatırsa → FAILED kaydedilir, exception yukarı çıkmaz")
    void processPayment_whenStrategyThrowsException_shouldHandleGracefully() {
        // ARRANGE
        PaymentRequest request = buildRequest(3L, 500.0);
        when(paymentStrategy.pay(any())).thenThrow(new RuntimeException("İyzico servisine bağlanılamadı"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT — Exception dışarı fırlatılmamalı
        PaymentResponse response = paymentService.processPayment(request);

        // ASSERT — Hata yönetimi doğru çalışmalı
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Ödeme hatası");

        // ASSERT — Yine de FAILED olarak veritabanına kaydedilmeli
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());

        Payment savedPayment = captor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo("FAILED");
        assertThat(savedPayment.getErrorMessage()).contains("İyzico servisine bağlanılamadı");
    }

    @Test
    @DisplayName("Exception durumunda orderId doğru şekilde response'a eklenir")
    void processPayment_whenStrategyThrowsException_shouldReturnCorrectOrderId() {
        // ARRANGE
        PaymentRequest request = buildRequest(5L, 100.0);
        when(paymentStrategy.pay(any())).thenThrow(new RuntimeException("Timeout"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        PaymentResponse response = paymentService.processPayment(request);

        // ASSERT
        assertThat(response.getOrderId()).isEqualTo(5L);
    }

    // ─────────────────────────────────────────────────────────────
    // getPaymentsByOrder
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Siparişe ait ödemeler doğru şekilde listelenir")
    void getPaymentsByOrder_shouldReturnPaymentsForOrder() {
        // ARRANGE
        Payment p1 = Payment.builder().id(1L).orderId(10L).status("SUCCESS").amount(300.0).build();
        Payment p2 = Payment.builder().id(2L).orderId(10L).status("FAILED").amount(300.0).build();

        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(p1, p2));

        // ACT
        List<Payment> payments = paymentService.getPaymentsByOrder(10L);

        // ASSERT
        assertThat(payments).hasSize(2);
        assertThat(payments.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(payments.get(1).getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Ödeme kaydı olmayan sipariş için boş liste döner")
    void getPaymentsByOrder_withNoPayments_shouldReturnEmptyList() {
        // ARRANGE
        when(paymentRepository.findByOrderId(99L)).thenReturn(List.of());

        // ACT
        List<Payment> payments = paymentService.getPaymentsByOrder(99L);

        // ASSERT
        assertThat(payments).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metotlar
    // ─────────────────────────────────────────────────────────────

    private PaymentRequest buildRequest(Long orderId, Double amount) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setUsername("alper");
        request.setAmount(amount);
        request.setFirstName("Alper");
        request.setLastName("Daşgın");
        request.setEmail("alper@test.com");
        request.setPhone("05551234567");
        request.setAddress("Test Sokak No:1");
        request.setCity("İstanbul");
        request.setCountry("Turkey");

        PaymentRequest.Card card = new PaymentRequest.Card();
        card.setCardHolderName("Alper Daşgın");
        card.setCardNumber("5528790000000008");
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");
        request.setCard(card);

        PaymentRequest.Item item = new PaymentRequest.Item();
        item.setProductId(1L);
        item.setProductName("Wireless Mouse");
        item.setPrice(amount);
        item.setQuantity(1);
        request.setItems(List.of(item));

        return request;
    }
}
