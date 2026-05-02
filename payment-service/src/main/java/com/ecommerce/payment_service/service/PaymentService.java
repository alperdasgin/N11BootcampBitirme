package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.entity.Payment;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    List<Payment> getPaymentsByOrder(Long orderId);
}
