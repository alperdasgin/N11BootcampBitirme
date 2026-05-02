package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockInitRunner implements CommandLineRunner {

    private final ProductStockRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new ProductStock(1L, "Laptop Pro 15", 50, 0));
            repo.save(new ProductStock(2L, "Wireless Mouse", 100, 0));
            repo.save(new ProductStock(3L, "Mekanik Klavye", 75, 0));
            repo.save(new ProductStock(4L, "Spor Ayakkabı", 200, 0));
            repo.save(new ProductStock(5L, "Akıllı Saat", 30, 0));
            repo.save(new ProductStock(6L, "Kitaplık Roman Seti", 60, 0));
            log.info("Stok verileri yüklendi.");
        }
    }
}