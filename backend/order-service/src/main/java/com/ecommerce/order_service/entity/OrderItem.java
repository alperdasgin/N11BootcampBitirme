package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    private Order order;
}