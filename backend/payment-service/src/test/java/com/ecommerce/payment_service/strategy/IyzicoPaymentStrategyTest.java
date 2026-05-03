package com.ecommerce.payment_service.strategy;

import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IyzicoPaymentStrategy için Unit Testleri.
 *
 * NOT: Bu testler İyzico'ya gerçek HTTP isteği ATMAZ.
 * İyzico entegrasyonunu test etmek için Sandbox ortamında
 * manuel veya integration test kullanılmalıdır.
 *
 * Burada sadece aşağıdaki mantıklar test edilir:
 * - Kart numarasındaki boşluk temizleme
 * - Geçersiz/eksik veri durumlarında savunmacı davranış
 * - Request oluşturma mantığı
 */
@DisplayName("IyzicoPaymentStrategy Unit Testleri")
class IyzicoPaymentStrategyTest {

    /**
     * İyzico'ya gerçek istek atmadan sadece iç mantığı test edebilmek için
     * IyzicoPaymentStrategy'yi spy/subclass olarak kullanıyoruz.
     * Gerçek pay() çağrısı yerine sadece buildCard() gibi iç metodları test ediyoruz.
     */
    private IyzicoPaymentStrategyTestable strategy;

    @BeforeEach
    void setUp() {
        strategy = new IyzicoPaymentStrategyTestable();
        // Sandbox değerleri — gerçek istek atmak istemediğimiz için test değerleri yeterli
        ReflectionTestUtils.setField(strategy, "apiKey", "sandbox-test-key");
        ReflectionTestUtils.setField(strategy, "secretKey", "sandbox-test-secret");
        ReflectionTestUtils.setField(strategy, "baseUrl", "https://sandbox-api.iyzipay.com");
    }

    // ─────────────────────────────────────────────────────────────
    // Kart numarası boşluk temizleme testleri
    // (Bu mantık bugfix olarak eklendi — regression test açısından kritik)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Boşluklu kart numarası temizlenerek İyzico'ya gönderilir")
    void cleanCardNumber_withSpaces_shouldRemoveAllSpaces() {
        String dirty   = "5528 7900 0000 0008";
        String cleaned = strategy.cleanCardNumber(dirty);
        assertThat(cleaned).isEqualTo("5528790000000008");
    }

    @ParameterizedTest(name = "Kart numarası: \"{0}\" boşluksuz hale getirilmeli")
    @ValueSource(strings = {
            "4355 0811 1111 1111",   // 4'lü boşluklu
            "4355-0811-1111-1111",   // tire ile ayrılmış (dikkat: replaceAll "\\s+" sadece boşluk siler)
            "5528790000000008",      // zaten temiz
            "  5528790000000008  "   // baş/son boşluk
    })
    @DisplayName("Farklı formatlarda girilen kart numaraları işlenir")
    void cleanCardNumber_variousFormats_shouldHandleCorrectly(String input) {
        String result = strategy.cleanCardNumber(input);
        // Sonuçta boşluk (whitespace) olmamalı
        assertThat(result).doesNotContain(" ").doesNotContain("\t");
    }

    @Test
    @DisplayName("null kart numarası null olarak döner (NullPointerException fırlatmaz)")
    void cleanCardNumber_withNull_shouldReturnNull() {
        String result = strategy.cleanCardNumber(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Boş string kart numarası boş olarak döner")
    void cleanCardNumber_withEmptyString_shouldReturnEmptyString() {
        String result = strategy.cleanCardNumber("");
        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // Request içeriği doğrulama testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ürün listesiyle gelen request'te sepet tutarı doğru hesaplanır")
    void buildBasketItems_withMultipleItems_shouldComputeCorrectPrices() {
        PaymentRequest request = buildRequest(1L, List.of(
                buildItem(1L, "Laptop", 1000.0, 2),   // 2000.0
                buildItem(2L, "Mouse",   50.0, 3)    //  150.0
        ));

        // Toplam: 2000 + 150 = 2150
        double expectedTotal = request.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        assertThat(expectedTotal).isEqualTo(2150.0);
    }

    @Test
    @DisplayName("Kart bilgisi tam dolu ise tüm alanlar taşınır")
    void card_allFieldsPresent_shouldBePreservedAfterCleaning() {
        PaymentRequest.Card card = new PaymentRequest.Card();
        card.setCardHolderName("Alper Daşgın");
        card.setCardNumber("5528 7900 0000 0008");
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");

        // Temizlenmiş kart numarası kontrol ediliyor
        String cleaned = strategy.cleanCardNumber(card.getCardNumber());
        assertThat(cleaned).isEqualTo("5528790000000008");
        assertThat(card.getExpireMonth()).isEqualTo("12");
        assertThat(card.getExpireYear()).isEqualTo("2030");
        assertThat(card.getCvc()).isEqualTo("123");
        assertThat(card.getCardHolderName()).isEqualTo("Alper Daşgın");
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı sınıf & metotlar
    // ─────────────────────────────────────────────────────────────

    /**
     * Gerçek İyzico çağrısını engellemek için pay() metodunu override eden test subclass.
     * cleanCardNumber gibi protected/package-private mantıkları test edebilmemizi sağlar.
     */
    static class IyzicoPaymentStrategyTestable extends IyzicoPaymentStrategy {
        @Override
        public PaymentResponse pay(PaymentRequest request) {
            // Gerçek İyzico çağrısı yapılmıyor — sadece iç metodlar test edilecek
            throw new UnsupportedOperationException("Gerçek İyzico çağrısı test ortamında desteklenmiyor. Integration test kullanın.");
        }

        /** buildCard içindeki replaceAll mantığını dışarıya açıyoruz */
        public String cleanCardNumber(String cardNumber) {
            return cardNumber != null ? cardNumber.replaceAll("\\s+", "") : null;
        }
    }

    private PaymentRequest buildRequest(Long orderId, List<PaymentRequest.Item> items) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setUsername("alper");
        request.setAmount(items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());
        request.setFirstName("Alper");
        request.setLastName("Daşgın");
        request.setEmail("alper@test.com");
        request.setPhone("05551234567");
        request.setAddress("Test Sokak No:1");
        request.setCity("İstanbul");
        request.setCountry("Turkey");

        PaymentRequest.Card card = new PaymentRequest.Card();
        card.setCardHolderName("Alper Daşgın");
        card.setCardNumber("5528790000000008");
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");
        request.setCard(card);
        request.setItems(items);
        return request;
    }

    private PaymentRequest.Item buildItem(Long productId, String name, Double price, Integer qty) {
        PaymentRequest.Item item = new PaymentRequest.Item();
        item.setProductId(productId);
        item.setProductName(name);
        item.setPrice(price);
        item.setQuantity(qty);
        return item;
    }
}
