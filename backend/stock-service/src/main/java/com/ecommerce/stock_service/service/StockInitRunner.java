package com.ecommerce.stock_service.service;

import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.lifecycle.StockDataExporter;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockInitRunner implements CommandLineRunner {

    private final ProductStockRepository repo;

    @Value("${app.export.dir}")
    private String exportDir;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("Stok tablosu zaten dolu, init atlandı. count={}", repo.count());
            return;
        }

        File seedFile = new File(exportDir, StockDataExporter.EXPORT_FILE);
        if (seedFile.exists()) {
            int loaded = loadFromSeedFile(seedFile);
            log.info("Önceki çalıştırmadan {} stok kaydı yüklendi ({}).", loaded, seedFile.getAbsolutePath());
            return;
        }

        loadDefaultSeed();
    }

    @SuppressWarnings("unchecked")
    private int loadFromSeedFile(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> rows = (List<Map<String, Object>>) payload.get("stocks");
            if (rows == null) return 0;

            int n = 0;
            for (Map<String, Object> row : rows) {
                Long productId = asLong(row.get("productId"));
                if (productId == null) continue;
                String name = row.get("productName") == null ? ("Product " + productId) : row.get("productName").toString();
                int avail = asInt(row.get("availableQuantity"), 0);
                int reserved = asInt(row.get("reservedQuantity"), 0);
                repo.save(new ProductStock(productId, name, avail, reserved));
                n++;
            }
            return n;
        } catch (Exception e) {
            log.warn("Stok seed dosyası okunamadı, varsayılan seed yüklenecek: {}", e.getMessage());
            loadDefaultSeed();
            return (int) repo.count();
        }
    }

    private void loadDefaultSeed() {
        repo.save(new ProductStock(1L, "Laptop Pro 15", 50, 0));
        repo.save(new ProductStock(2L, "Wireless Mouse", 100, 0));
        repo.save(new ProductStock(3L, "Mekanik Klavye", 75, 0));
        repo.save(new ProductStock(4L, "Spor Ayakkabı", 200, 0));
        repo.save(new ProductStock(5L, "Akıllı Saat", 30, 0));
        repo.save(new ProductStock(6L, "Kitaplık Roman Seti", 60, 0));
        repo.save(new ProductStock(7L, "Güneş Gözlüğü", 80, 0));
        log.info("Stok verileri yüklendi (varsayılan).");
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private static Integer asInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
}
