package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Ürün işlemleri")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Yeni ürün ekle")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    @Operation(summary = "Tüm ürünleri sayfalı getir")
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, sortBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile ürün getir")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Kategoriye göre ürün getir")
    public ResponseEntity<PageResponse<ProductResponse>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getProductsByCategory(category, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Ürün ara")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.searchProducts(keyword, page, size));
    }

    @GetMapping("/categories")
    @Operation(summary = "Tüm kategorileri listele")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ürün güncelle")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ürün sil")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Stok güncelle (+ ekle, - çıkar)")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service çalışıyor");
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Ürüne yorum yap (Sadece satın alanlar)")
    public ResponseEntity<com.ecommerce.product_service.dto.ReviewResponse> addReview(
            @PathVariable Long id,
            @Valid @RequestBody com.ecommerce.product_service.dto.ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addReview(id, request));
    }
}