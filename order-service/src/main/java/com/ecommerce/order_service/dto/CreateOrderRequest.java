package com.ecommerce.order_service.dto;

import lombok.*;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String username;
    private List<OrderItemDto> items;
    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String country;
    private String phone;
    private String email;
    private Card card;

    @Data
    public static class OrderItemDto {
        private Long productId;
        private String productName;
        private Double price;
        private Integer quantity;
    }

    @Data
    public static class Card {
        private String cardHolderName;
        private String cardNumber;
        private String expireMonth;
        private String expireYear;
        private String cvc;
    }
}