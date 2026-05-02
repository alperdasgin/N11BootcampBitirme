package com.ecommerce.stock_service.dto;

import lombok.*;
import java.util.List;

public class StockEventPayloads {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockReserveRequestedEvent {
        private Long orderId;
        private String username;
        private List<Item> items;

        @Data @NoArgsConstructor @AllArgsConstructor
        public static class Item {
            private Long productId;
            private Integer quantity;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockReservedEvent {
        private Long orderId;
        private String username;
        private String message;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockRejectedEvent {
        private Long orderId;
        private String username;
        private String reason;
    }
}