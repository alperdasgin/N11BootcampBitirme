package com.ecommerce.order_service.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String username;
    private Double totalPrice;
    private String status;
    private List<OrderItemResponse> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private Double price;
        private Integer quantity;
    }
}