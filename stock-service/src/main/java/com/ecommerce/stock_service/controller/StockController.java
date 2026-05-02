package com.ecommerce.stock_service.controller;

import com.ecommerce.stock_service.dto.*;
import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import com.ecommerce.stock_service.service.StockDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Stock", description = "Stok işlemleri")
public class StockController {

    private final StockDomainService stock;
    private final ProductStockRepository stockRepository;

    @PostMapping("/reserve")
    @Operation(summary = "Stok rezerve et")
    public ResponseEntity<StockUpdateResponse> reserve(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.reserve(req));
    }

    @PostMapping("/release")
    @Operation(summary = "Rezervasyonu geri bırak")
    public ResponseEntity<StockUpdateResponse> release(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.release(req));
    }

    @PostMapping("/commit")
    @Operation(summary = "Rezervasyonu satışa çevir")
    public ResponseEntity<StockUpdateResponse> commit(@RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.commit(req));
    }

    @GetMapping
    @Operation(summary = "Tüm stokları listele")
    public ResponseEntity<List<ProductStock>> getAllStocks() {
        return ResponseEntity.ok(stockRepository.findAll());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Ürün stok bilgisi")
    public ResponseEntity<ProductStock> getStock(@PathVariable Long productId) {
        return stockRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Stock Service çalışıyor");
    }
}