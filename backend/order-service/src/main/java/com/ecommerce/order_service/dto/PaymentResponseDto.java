package com.ecommerce.order_service.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponseDto {
    private boolean success;
    private String transactionId;
    private String message;
    private Long orderId;
}
