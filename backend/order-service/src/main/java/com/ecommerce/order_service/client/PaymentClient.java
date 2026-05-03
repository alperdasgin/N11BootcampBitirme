package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.PaymentRequestDto;
import com.ecommerce.order_service.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * FeignClient — Order Service → Payment Service arası HTTP iletişimi.
 * Circuit Breaker fallback: PaymentClientFallback
 */
@FeignClient(name = "payment-service", fallback = PaymentClientFallback.class)
public interface PaymentClient {

    @PostMapping("/api/payments/pay")
    PaymentResponseDto processPayment(@RequestBody PaymentRequestDto request);
}
