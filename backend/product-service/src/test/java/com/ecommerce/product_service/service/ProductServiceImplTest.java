package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Testleri")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    // ─────────────────────────────────────────────────────────────
    // createProduct testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli bir istek ile ürün başarıyla oluşturulur")
    void createProduct_withValidRequest_shouldSaveAndReturnProduct() {
        // ARRANGE
        ProductRequest request = new ProductRequest();
        request.setName("MacBook Pro");
        request.setDescription("M2 Chip");
        request.setPrice(new BigDecimal("25000.00"));
        request.setStock(50);
        request.setCategory("Electronics");

        Product savedProduct = Product.builder()
                .id(1L)
                .name("MacBook Pro")
                .description("M2 Chip")
                .price(new BigDecimal("25000.00"))
                .stock(50)
                .category("Electronics")
                .active(true)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // ACT
        ProductResponse response = productService.createProduct(request);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("MacBook Pro");
        assertThat(response.getActive()).isTrue();

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("MacBook Pro");
    }

    // ─────────────────────────────────────────────────────────────
    // getProductById testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Var olan bir ürün ID'si ile ürün getirilir")
    void getProductById_withExistingId_shouldReturnProduct() {
        // ARRANGE
        Product product = Product.builder()
                .id(1L)
                .name("iPhone 14")
                .price(new BigDecimal("15000.00"))
                .active(true)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // ACT
        ProductResponse response = productService.getProductById(1L);

        // ASSERT
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("iPhone 14");
    }

    @Test
    @DisplayName("Var olmayan bir ürün ID'si exception fırlatır")
    void getProductById_withNonExistingId_shouldThrowException() {
        // ARRANGE
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ürün bulunamadı");
    }

    // ─────────────────────────────────────────────────────────────
    // updateProduct testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Var olan ürün bilgileri güncellenir")
    void updateProduct_withExistingId_shouldUpdateAndReturnProduct() {
        // ARRANGE
        Product existingProduct = Product.builder()
                .id(1L)
                .name("Old Name")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .build();

        ProductRequest request = new ProductRequest();
        request.setName("New Name");
        request.setPrice(new BigDecimal("200.00"));
        request.setStock(20);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // ACT
        ProductResponse response = productService.updateProduct(1L, request);

        // ASSERT
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.getStock()).isEqualTo(20);
    }

    // ─────────────────────────────────────────────────────────────
    // deleteProduct testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ürün silme işlemi (soft delete) success")
    void deleteProduct_shouldSetStatusToInactive() {
        // ARRANGE
        Product product = Product.builder().id(1L).active(true).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // ACT
        productService.deleteProduct(1L);

        // ASSERT
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse(); // Soft delete check
    }

    // ─────────────────────────────────────────────────────────────
    // updateStock testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli stok güncellemesi başarılı olur")
    void updateStock_withValidQuantity_shouldUpdateStock() {
        // ARRANGE
        Product product = Product.builder().id(1L).stock(10).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        // ACT
        ProductResponse response = productService.updateStock(1L, -3); // 3 tane düşür

        // ASSERT
        assertThat(response.getStock()).isEqualTo(7); // 10 - 3 = 7
    }

    @Test
    @DisplayName("Mevcut stoktan fazla düşülmeye çalışılırsa exception fırlatır")
    void updateStock_withInsufficientQuantity_shouldThrowException() {
        // ARRANGE
        Product product = Product.builder().id(1L).stock(5).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // ACT & ASSERT
        assertThatThrownBy(() -> productService.updateStock(1L, -10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    // ─────────────────────────────────────────────────────────────
    // Listeleme / Arama (PageResponse) Testleri
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Aktif ürünler sayfalama ile listelenir")
    void getAllProducts_shouldReturnPageResponse() {
        // ARRANGE
        Product p1 = Product.builder().id(1L).name("P1").active(true).build();
        Page<Product> page = new PageImpl<>(List.of(p1), PageRequest.of(0, 10), 1);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        // ACT
        PageResponse<ProductResponse> response = productService.getAllProducts(0, 10, "id");

        // ASSERT
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("P1");
    }

    @Test
    @DisplayName("Kategoriye göre aktif ürünler listelenir")
    void getProductsByCategory_shouldReturnFilteredProducts() {
        // ARRANGE
        Product p1 = Product.builder().id(1L).name("TV").category("Electronics").active(true).build();
        Page<Product> page = new PageImpl<>(List.of(p1));
        when(productRepository.findByCategoryAndActiveTrue(eq("Electronics"), any(Pageable.class))).thenReturn(page);

        // ACT
        PageResponse<ProductResponse> response = productService.getProductsByCategory("Electronics", 0, 10);

        // ASSERT
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Kelimeye göre ürün araması sonuç döner")
    void searchProducts_shouldReturnMatchingProducts() {
        // ARRANGE
        Product p1 = Product.builder().id(1L).name("Smart TV").build();
        Page<Product> page = new PageImpl<>(List.of(p1));
        when(productRepository.searchByName(eq("TV"), any(Pageable.class))).thenReturn(page);

        // ACT
        PageResponse<ProductResponse> response = productService.searchProducts("TV", 0, 10);

        // ASSERT
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Smart TV");
    }
}
