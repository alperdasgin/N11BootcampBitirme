package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // Ürün oluştur
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Yeni ürün oluşturuluyor: {}", request.getName());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        log.info("Ürün oluşturuldu. id={}", saved.getId());
        return toResponse(saved);
    }

    // Tüm aktif ürünleri sayfalı getir (PAGINATION)
    public PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPageResponse(productPage);
    }

    // Kategoriye göre filtrele (PAGINATION)
    public PageResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByCategoryAndActiveTrue(category, pageable);
        return toPageResponse(productPage);
    }

    // İsme göre arama (PAGINATION)
    public PageResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.searchByName(keyword, pageable);
        return toPageResponse(productPage);
    }

    // Tekil ürün getir
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        return toResponse(product);
    }

    // Ürün güncelle
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        Product updated = productRepository.save(product);
        log.info("Ürün güncellendi. id={}", id);
        return toResponse(updated);
    }

    // Ürün sil (soft delete)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Ürün silindi (soft delete). id={}", id);
    }

    // Kategorileri listele
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // Stok güncelle (Order/Stock servisleri çağırır)
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        if (product.getStock() + quantity < 0) {
            throw new RuntimeException("Yetersiz stok. Mevcut: " + product.getStock());
        }
        product.setStock(product.getStock() + quantity);
        return toResponse(productRepository.save(product));
    }

    // Entity → DTO dönüşümü
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .category(p.getCategory())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}