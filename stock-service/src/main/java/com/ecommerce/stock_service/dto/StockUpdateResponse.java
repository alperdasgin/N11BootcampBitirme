package com.ecommerce.stock_service.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StockUpdateResponse {
    private boolean success;
    private String message;

    public static StockUpdateResponse ok(String msg) { return new StockUpdateResponse(true, msg); }
    public static StockUpdateResponse fail(String msg) { return new StockUpdateResponse(false, msg); }
}