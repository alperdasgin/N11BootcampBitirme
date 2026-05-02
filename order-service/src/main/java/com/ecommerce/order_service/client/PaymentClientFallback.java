package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.PaymentRequestDto;
import com.ecommerce.order_service.dto.PaymentResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Circuit Breaker Fallback — Payment Service erişilemez olduğunda devreye girer.
 */
@Component
@Slf4j
public class PaymentClientFallback implements PaymentClient {

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        log.error("[CircuitBreaker] Payment Service erişilemiyor! orderId={}", request.getOrderId());
        return PaymentResponseDto.builder()
                .success(false)
                .orderId(request.getOrderId())
                .message("Ödeme servisi şu an kullanılamıyor. Lütfen tekrar deneyin.")
                .build();
    }
}
