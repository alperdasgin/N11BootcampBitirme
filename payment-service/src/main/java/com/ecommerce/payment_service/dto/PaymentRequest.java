package com.ecommerce.payment_service.dto;

import lombok.*;
import java.util.List;

@Data
public class PaymentRequest {
    private Long orderId;
    private String username;
    private Double amount;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private Card card;
    private List<Item> items;

    @Data
    public static class Card {
        private String cardHolderName;
        private String cardNumber;
        private String expireMonth;
        private String expireYear;
        private String cvc;
    }

    @Data
    public static class Item {
        private Long productId;
        private String productName;
        private Double price;
        private Integer quantity;
    }
}