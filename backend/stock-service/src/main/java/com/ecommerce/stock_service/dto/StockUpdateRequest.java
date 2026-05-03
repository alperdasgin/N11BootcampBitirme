package com.ecommerce.stock_service.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockUpdateRequest {
    private List<StockItem> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class StockItem {
        private Long productId;
        private Integer quantity;
    }
}