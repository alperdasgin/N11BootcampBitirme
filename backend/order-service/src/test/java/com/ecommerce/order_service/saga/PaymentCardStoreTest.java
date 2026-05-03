package com.ecommerce.order_service.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentCardStore Unit Testleri")
class PaymentCardStoreTest {

    private PaymentCardStore store;

    @BeforeEach
    void setUp() {
        store = new PaymentCardStore();
    }

    @Test
    @DisplayName("Kart bilgisi kaydedilir ve doğru şekilde alınır")
    void put_andTake_shouldWorkCorrectly() {
        // ARRANGE
        PaymentCardStore.CardInfo card = buildCard("5528790000000008");

        // ACT
        store.put(1L, card);
        PaymentCardStore.CardInfo retrieved = store.take(1L);

        // ASSERT
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getCardNumber()).isEqualTo("5528790000000008");
        assertThat(retrieved.getExpireYear()).isEqualTo("2030");
    }

    @Test
    @DisplayName("take() çağrısı kart bilgisini store'dan siler (güvenlik)")
    void take_shouldRemoveCardFromStore() {
        // ARRANGE
        store.put(1L, buildCard("5528790000000008"));

        // ACT: İlk take başarılı olmalı
        PaymentCardStore.CardInfo first = store.take(1L);
        // İkinci take null olmalı (çünkü silindi)
        PaymentCardStore.CardInfo second = store.take(1L);

        // ASSERT
        assertThat(first).isNotNull();
        assertThat(second).isNull();
    }

    @Test
    @DisplayName("Kayıt olmayan ID için take() null döner")
    void take_withNonExistentId_shouldReturnNull() {
        // ACT & ASSERT
        assertThat(store.take(999L)).isNull();
    }

    @Test
    @DisplayName("null orderId ile put() çağrısı güvenli şekilde yok sayılır")
    void put_withNullOrderId_shouldBeIgnored() {
        // ACT: null ile put — hata fırlatmamalı
        store.put(null, buildCard("5528790000000008"));

        // ASSERT: null ile take da null dönmeli
        assertThat(store.take(null)).isNull();
    }

    @Test
    @DisplayName("null kart bilgisi ile put() çağrısı güvenli şekilde yok sayılır")
    void put_withNullCard_shouldBeIgnored() {
        // ACT: Null kart ile put — hata fırlatmamalı
        store.put(1L, null);

        // ASSERT: Daha sonra take() null dönmeli
        assertThat(store.take(1L)).isNull();
    }

    @Test
    @DisplayName("Farklı siparişlerin kart bilgileri birbirini etkilemez")
    void multipleOrders_shouldHaveIsolatedCardInfo() {
        // ARRANGE
        store.put(1L, buildCard("5528790000000008"));
        store.put(2L, buildCard("4766620000000001"));

        // ACT
        PaymentCardStore.CardInfo card1 = store.take(1L);
        PaymentCardStore.CardInfo card2 = store.take(2L);

        // ASSERT
        assertThat(card1.getCardNumber()).isEqualTo("5528790000000008");
        assertThat(card2.getCardNumber()).isEqualTo("4766620000000001");
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metot
    // ─────────────────────────────────────────────────────────────

    private PaymentCardStore.CardInfo buildCard(String cardNumber) {
        PaymentCardStore.CardInfo card = new PaymentCardStore.CardInfo();
        card.setCardHolderName("Test Kullanıcı");
        card.setCardNumber(cardNumber);
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");
        return card;
    }
}
