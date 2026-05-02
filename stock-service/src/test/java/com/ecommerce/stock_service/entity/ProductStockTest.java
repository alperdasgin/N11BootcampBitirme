package com.ecommerce.stock_service.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductStock Domain Mantığı Testleri")
class ProductStockTest {

    // ─────────────────────────────────────────────────────────────
    // reserve() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Yeterli stok varsa reserve() başarılı olur ve miktarlar güncellenir")
    void reserve_withSufficientStock_shouldUpdateQuantities() {
        // ARRANGE: 10 adet mevcut stok
        ProductStock stock = buildStock(1L, "Laptop", 10, 0);

        // ACT: 3 adet rezerve et
        stock.reserve(3);

        // ASSERT
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);  // 10 - 3
        assertThat(stock.getReservedQuantity()).isEqualTo(3);   // 0 + 3
    }

    @Test
    @DisplayName("Tam stok kadar reserve() yapılabilir (sınır değer)")
    void reserve_exactAvailableQuantity_shouldSucceed() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Mouse", 5, 0);

        // ACT: Tam 5 adet rezerve et
        stock.reserve(5);

        // ASSERT
        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Yetersiz stokta reserve() IllegalStateException fırlatır")
    void reserve_withInsufficientStock_shouldThrowException() {
        // ARRANGE: sadece 2 adet mevcut
        ProductStock stock = buildStock(1L, "Klavye", 2, 0);

        // ASSERT: 3 adet rezerve etmeye çalışınca hata fırlatmalı
        assertThatThrownBy(() -> stock.reserve(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    @Test
    @DisplayName("Sıfır stokta reserve() hata fırlatır")
    void reserve_withZeroStock_shouldThrowException() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Kulaklık", 0, 0);

        // ASSERT
        assertThatThrownBy(() -> stock.reserve(1))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // release() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("release() reservedQuantity'yi azaltır, availableQuantity'yi artırır")
    void release_shouldRestoreQuantities() {
        // ARRANGE: 5 reserved, 3 available
        ProductStock stock = buildStock(1L, "Monitor", 3, 5);

        // ACT: 2 adeti serbest bırak
        stock.release(2);

        // ASSERT
        assertThat(stock.getReservedQuantity()).isEqualTo(3);   // 5 - 2
        assertThat(stock.getAvailableQuantity()).isEqualTo(5);  // 3 + 2
    }

    @Test
    @DisplayName("Rezerve edilenden fazlasını release() etmeye çalışınca hata fırlatır")
    void release_withMoreThanReserved_shouldThrowException() {
        // ARRANGE: sadece 2 reserved
        ProductStock stock = buildStock(1L, "Tablet", 10, 2);

        // ASSERT: 5 adet bırakmaya çalışınca hata
        assertThatThrownBy(() -> stock.release(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Yetersiz rezerve stok");
    }

    @Test
    @DisplayName("Tüm rezerve stoku release() edebilirsin (sınır değer)")
    void release_exactReservedAmount_shouldSucceed() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Telefon", 0, 3);

        // ACT
        stock.release(3);

        // ASSERT
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────────
    // commit() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("commit() reservedQuantity'yi azaltır (satış tamamlandı, stok tamamen düşüldü)")
    void commit_shouldDecreaseReservedQuantityOnly() {
        // ARRANGE: Ödeme başarılı, satış kesinleşiyor
        ProductStock stock = buildStock(1L, "Çanta", 7, 3);

        // ACT: 2 adet commit et
        stock.commit(2);

        // ASSERT: Reserved azalır, available değişmez
        assertThat(stock.getReservedQuantity()).isEqualTo(1);   // 3 - 2
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);  // değişmez
    }

    @Test
    @DisplayName("Rezerve edilenden fazlasını commit() etmeye çalışınca hata fırlatır")
    void commit_withMoreThanReserved_shouldThrowException() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Saat", 10, 1);

        // ASSERT: 5 commit etmeye çalışınca hata
        assertThatThrownBy(() -> stock.commit(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Yetersiz rezerve stok");
    }

    // ─────────────────────────────────────────────────────────────
    // Ardışık işlem senaryosu
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve → release → reserve sırasıyla doğru çalışır (tam senaryo)")
    void fullCycleScenario_reserveThenReleaseThenReserve() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Kamera", 10, 0);

        // Önce 4 adet rezerve et
        stock.reserve(4);
        assertThat(stock.getAvailableQuantity()).isEqualTo(6);
        assertThat(stock.getReservedQuantity()).isEqualTo(4);

        // Ödeme başarısız, 4 adeti geri bırak
        stock.release(4);
        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);

        // Tekrar 7 adet rezerve et
        stock.reserve(7);
        assertThat(stock.getAvailableQuantity()).isEqualTo(3);
        assertThat(stock.getReservedQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("reserve → commit sırasıyla doğru çalışır (başarılı satış senaryosu)")
    void fullCycleScenario_reserveThenCommit() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Yazıcı", 5, 0);

        // Rezerve et
        stock.reserve(2);
        assertThat(stock.getAvailableQuantity()).isEqualTo(3);
        assertThat(stock.getReservedQuantity()).isEqualTo(2);

        // Ödeme başarılı, commit et
        stock.commit(2);
        assertThat(stock.getAvailableQuantity()).isEqualTo(3); // değişmez
        assertThat(stock.getReservedQuantity()).isEqualTo(0);  // tamamen düşüldü
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metot
    // ─────────────────────────────────────────────────────────────

    private ProductStock buildStock(Long productId, String name,
                                    int available, int reserved) {
        return ProductStock.builder()
                .productId(productId)
                .productName(name)
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .build();
    }
}
