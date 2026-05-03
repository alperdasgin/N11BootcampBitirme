package com.ecommerce.stock_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_stock")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductStock {
    @Id
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    public void reserve(int q) {
        if (availableQuantity < q) throw new IllegalStateException("Yetersiz stok");
        availableQuantity -= q;
        reservedQuantity += q;
    }

    public void release(int q) {
        if (reservedQuantity < q) throw new IllegalStateException("Yetersiz rezerve stok");
        reservedQuantity -= q;
        availableQuantity += q;
    }

    public void commit(int q) {
        if (reservedQuantity < q) throw new IllegalStateException("Yetersiz rezerve stok");
        reservedQuantity -= q;
    }
}