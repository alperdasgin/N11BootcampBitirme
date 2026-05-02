package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.*;
import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDomainService {

    private final ProductStockRepository repo;

    @Transactional
    public StockUpdateResponse reserve(StockUpdateRequest req) {
        try {
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı: " + it.getProductId()));
                if (ps.getAvailableQuantity() < it.getQuantity())
                    throw new IllegalStateException("Yetersiz stok. productId=" + it.getProductId());
            }
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.reserve(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stok rezerve edildi");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse release(StockUpdateRequest req) {
        try {
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.release(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stok serbest bırakıldı");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse commit(StockUpdateRequest req) {
        try {
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = repo.findById(it.getProductId()).orElseThrow();
                ps.commit(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stok taahhüt edildi");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }
}