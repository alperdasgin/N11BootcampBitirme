package com.ecommerce.payment_service.controller;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Payment", description = "Ödeme işlemleri")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    @Operation(summary = "Ödeme yap (Iyzico)")
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Siparişe ait ödemeleri getir")
    public ResponseEntity<List<Payment>> getPaymentsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(orderId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service çalışıyor");
    }
}