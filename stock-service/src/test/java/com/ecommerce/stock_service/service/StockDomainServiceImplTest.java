package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.StockUpdateRequest;
import com.ecommerce.stock_service.dto.StockUpdateResponse;
import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockDomainServiceImpl Unit Testleri")
class StockDomainServiceImplTest {

    @Mock
    private ProductStockRepository repo;

    @InjectMocks
    private StockDomainServiceImpl stockService;

    // ─────────────────────────────────────────────────────────────
    // reserve() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Yeterli stok varsa reserve() başarılı döner ve kayıt yapılır")
    void reserve_withSufficientStock_shouldReturnSuccess() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Laptop", 10, 0);
        when(repo.findAllById(anyList())).thenReturn(List.of(stock));
        when(repo.save(any(ProductStock.class))).thenReturn(stock);

        StockUpdateRequest req = buildRequest(1L, 3);

        // ACT
        StockUpdateResponse response = stockService.reserve(req);

        // ASSERT
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("rezerve");
        verify(repo, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Yetersiz stokta reserve() başarısız döner, hata mesajı içerir")
    void reserve_withInsufficientStock_shouldReturnFailure() {
        // ARRANGE: sadece 2 adet var, 5 isteniyor
        ProductStock stock = buildStock(1L, "Mouse", 2, 0);
        when(repo.findAllById(anyList())).thenReturn(List.of(stock));

        StockUpdateRequest req = buildRequest(1L, 5);

        // ACT
        StockUpdateResponse response = stockService.reserve(req);

        // ASSERT
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Yetersiz stok");
        // Yetersiz stok durumunda veritabanına kayıt yapılmamalı
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Var olmayan ürün için reserve() başarısız döner")
    void reserve_withNonExistentProduct_shouldReturnFailure() {
        // ARRANGE: repo boş liste döndürüyor (ürün yok)
        when(repo.findAllById(anyList())).thenReturn(List.of());

        StockUpdateRequest req = buildRequest(999L, 1);

        // ACT
        StockUpdateResponse response = stockService.reserve(req);

        // ASSERT
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("bulunamadı");
    }

    @Test
    @DisplayName("Birden fazla ürün için reserve() tek sorguda çalışır (N+1 yok)")
    void reserve_withMultipleProducts_shouldUseSingleQuery() {
        // ARRANGE
        ProductStock stock1 = buildStock(1L, "Laptop", 10, 0);
        ProductStock stock2 = buildStock(2L, "Mouse", 5, 0);
        when(repo.findAllById(anyList())).thenReturn(List.of(stock1, stock2));
        when(repo.save(any(ProductStock.class))).thenReturn(null);

        StockUpdateRequest req = new StockUpdateRequest(List.of(
                new StockUpdateRequest.StockItem(1L, 2),
                new StockUpdateRequest.StockItem(2L, 1)
        ));

        // ACT
        StockUpdateResponse response = stockService.reserve(req);

        // ASSERT
        assertThat(response.isSuccess()).isTrue();
        // findAllById sadece 1 kez çağrılmalı (N+1 önlemi)
        verify(repo, times(1)).findAllById(anyList());
        // 2 ürün için 2 kez save çağrılmalı
        verify(repo, times(2)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Birden fazla üründen biri yetersizse hiçbiri kaydedilmez (atomicity)")
    void reserve_whenOneProductHasInsufficientStock_shouldSaveNone() {
        // ARRANGE: 1. ürün yeterli, 2. ürün yetersiz
        ProductStock stock1 = buildStock(1L, "Laptop", 10, 0);
        ProductStock stock2 = buildStock(2L, "Mouse",   1, 0); // sadece 1 var
        when(repo.findAllById(anyList())).thenReturn(List.of(stock1, stock2));

        StockUpdateRequest req = new StockUpdateRequest(List.of(
                new StockUpdateRequest.StockItem(1L, 2),
                new StockUpdateRequest.StockItem(2L, 5)  // 5 isteniyor, sadece 1 var
        ));

        // ACT
        StockUpdateResponse response = stockService.reserve(req);

        // ASSERT: İşlem başarısız olmalı
        assertThat(response.isSuccess()).isFalse();
        // Hiçbir ürün kaydedilmemeli (atomicity)
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // release() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("release() başarılı olduğunda stok geri iade edilir ve SUCCESS döner")
    void release_shouldRestoreStockAndReturnSuccess() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Klavye", 3, 5);
        when(repo.findAllById(anyList())).thenReturn(List.of(stock));
        when(repo.save(any(ProductStock.class))).thenReturn(stock);

        StockUpdateRequest req = buildRequest(1L, 2);

        // ACT
        StockUpdateResponse response = stockService.release(req);

        // ASSERT
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("serbest");
        verify(repo, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Var olmayan ürün için release() başarısız döner")
    void release_withNonExistentProduct_shouldReturnFailure() {
        // ARRANGE
        when(repo.findAllById(anyList())).thenReturn(List.of());

        StockUpdateRequest req = buildRequest(999L, 1);

        // ACT
        StockUpdateResponse response = stockService.release(req);

        // ASSERT
        assertThat(response.isSuccess()).isFalse();
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // commit() testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("commit() başarılı olduğunda SUCCESS döner ve kayıt yapılır")
    void commit_shouldReturnSuccess() {
        // ARRANGE
        ProductStock stock = buildStock(1L, "Monitor", 0, 3);
        when(repo.findAllById(anyList())).thenReturn(List.of(stock));
        when(repo.save(any(ProductStock.class))).thenReturn(stock);

        StockUpdateRequest req = buildRequest(1L, 2);

        // ACT
        StockUpdateResponse response = stockService.commit(req);

        // ASSERT
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("taahhüt");
        verify(repo, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Var olmayan ürün için commit() başarısız döner")
    void commit_withNonExistentProduct_shouldReturnFailure() {
        // ARRANGE
        when(repo.findAllById(anyList())).thenReturn(List.of());

        // ACT
        StockUpdateResponse response = stockService.commit(buildRequest(999L, 1));

        // ASSERT
        assertThat(response.isSuccess()).isFalse();
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // Yardımcı metotlar
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

    private StockUpdateRequest buildRequest(Long productId, int quantity) {
        return new StockUpdateRequest(
                List.of(new StockUpdateRequest.StockItem(productId, quantity))
        );
    }
}
