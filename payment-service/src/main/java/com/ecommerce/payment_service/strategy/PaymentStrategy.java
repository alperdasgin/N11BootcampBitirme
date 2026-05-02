package com.ecommerce.payment_service.strategy;

import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;

/**
 * Strategy Pattern — Ödeme sağlayıcı arayüzü.
 * Yeni bir ödeme yöntemi eklemek için bu interface'i implemente et
 * (örn. StripePaymentStrategy, PayPalPaymentStrategy).
 */
public interface PaymentStrategy {
    PaymentResponse pay(PaymentRequest request);
}
