package com.ecommerce.order_service.saga;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PaymentCardStore {

    @Data
    public static class CardInfo {
        private String cardHolderName;
        private String cardNumber;
        private String expireMonth;
        private String expireYear;
        private String cvc;
    }

    private final ConcurrentMap<Long, CardInfo> store = new ConcurrentHashMap<>();

    public void put(Long orderId, CardInfo card) {
        if (orderId != null && card != null) store.put(orderId, card);
    }

    public CardInfo take(Long orderId) {
        return orderId != null ? store.remove(orderId) : null;
    }
}