package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategy paymentStrategy;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Ödeme işlemi başladı. orderId={}, amount={}", request.getOrderId(), request.getAmount());
        try {
            PaymentResponse response = paymentStrategy.pay(request);

            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .username(request.getUsername())
                    .amount(request.getAmount())
                    .status(response.isSuccess() ? "SUCCESS" : "FAILED")
                    .transactionId(response.getTransactionId())
                    .errorMessage(response.isSuccess() ? null : response.getMessage())
                    .build();
            paymentRepository.save(payment);

            log.info("Ödeme sonucu: success={}, transactionId={}", response.isSuccess(), response.getTransactionId());
            return response;

        } catch (Exception e) {
            log.error("Ödeme hatası: {}", e.getMessage());
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .username(request.getUsername())
                    .amount(request.getAmount())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build();
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .success(false)
                    .message("Ödeme hatası: " + e.getMessage())
                    .orderId(request.getOrderId())
                    .build();
        }
    }

    @Override
    public List<Payment> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
