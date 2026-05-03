package com.ecommerce.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_details")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    private Order order;

    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String country;
    private String phone;
    private String email;
}