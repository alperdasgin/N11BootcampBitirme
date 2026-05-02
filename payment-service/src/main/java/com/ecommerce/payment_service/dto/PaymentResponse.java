package com.ecommerce.payment_service.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private boolean success;
    private String transactionId;
    private String message;
    private Long orderId;
}