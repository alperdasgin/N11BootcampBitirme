package com.ecommerce.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncEvent {

    public enum Type { CREATED, UPDATED, DELETED }

    private Long productId;
    private String productName;
    private Integer stock;
    private Type type;
}
