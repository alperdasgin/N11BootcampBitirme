package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.dto.*;
import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDomainServiceImpl implements StockDomainService {

    private final ProductStockRepository repo;

    @Override
    @Transactional
    public StockUpdateResponse reserve(StockUpdateRequest req) {
        try {
            List<Long> ids = req.getItems().stream()
                    .map(StockUpdateRequest.StockItem::getProductId)
                    .collect(Collectors.toList());

            // N+1 önlemi: tek sorguda tüm stokları çek
            Map<Long, ProductStock> stockMap = repo.findAllById(ids).stream()
                    .collect(Collectors.toMap(ProductStock::getProductId, Function.identity()));

            // Önce tüm stokları doğrula
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = stockMap.get(it.getProductId());
                if (ps == null) throw new IllegalArgumentException("Ürün bulunamadı: " + it.getProductId());
                if (ps.getAvailableQuantity() < it.getQuantity())
                    throw new IllegalStateException("Yetersiz stok. productId=" + it.getProductId()
                            + " mevcut=" + ps.getAvailableQuantity() + " istenen=" + it.getQuantity());
            }

            // Sonra hepsini güncelle
            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = stockMap.get(it.getProductId());
                ps.reserve(it.getQuantity());
                repo.save(ps);
            }

            return StockUpdateResponse.ok("Stok rezerve edildi");
        } catch (Exception e) {
            log.error("Stok rezerve hatası: {}", e.getMessage());
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Override
    @Transactional
    public StockUpdateResponse release(StockUpdateRequest req) {
        try {
            List<Long> ids = req.getItems().stream()
                    .map(StockUpdateRequest.StockItem::getProductId)
                    .collect(Collectors.toList());

            Map<Long, ProductStock> stockMap = repo.findAllById(ids).stream()
                    .collect(Collectors.toMap(ProductStock::getProductId, Function.identity()));

            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = stockMap.get(it.getProductId());
                if (ps == null) throw new IllegalArgumentException("Ürün bulunamadı: " + it.getProductId());
                ps.release(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stok serbest bırakıldı");
        } catch (Exception e) {
            log.error("Stok serbest bırakma hatası: {}", e.getMessage());
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Override
    @Transactional
    public StockUpdateResponse commit(StockUpdateRequest req) {
        try {
            List<Long> ids = req.getItems().stream()
                    .map(StockUpdateRequest.StockItem::getProductId)
                    .collect(Collectors.toList());

            Map<Long, ProductStock> stockMap = repo.findAllById(ids).stream()
                    .collect(Collectors.toMap(ProductStock::getProductId, Function.identity()));

            for (StockUpdateRequest.StockItem it : req.getItems()) {
                ProductStock ps = stockMap.get(it.getProductId());
                if (ps == null) throw new IllegalArgumentException("Ürün bulunamadı: " + it.getProductId());
                ps.commit(it.getQuantity());
                repo.save(ps);
            }
            return StockUpdateResponse.ok("Stok taahhüt edildi");
        } catch (Exception e) {
            log.error("Stok commit hatası: {}", e.getMessage());
            return StockUpdateResponse.fail(e.getMessage());
        }
    }
}
