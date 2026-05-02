package com.ecommerce.cart_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartResponse {
    private Long id;
    private String username;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private Integer totalItems;
}