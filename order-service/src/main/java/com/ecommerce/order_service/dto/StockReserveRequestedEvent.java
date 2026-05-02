package com.ecommerce.order_service.dto;

import lombok.*;
import java.io.Serializable;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockReserveRequestedEvent implements Serializable {
    private Long orderId;
    private String username;
    private List<Item> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Item implements Serializable {
        private Long productId;
        private Integer quantity;
    }
}