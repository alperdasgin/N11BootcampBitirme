package com.ecommerce.stock_service.lifecycle;

import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDataExporter {

    public static final String EXPORT_FILE = "stock-seed.json";

    private final ProductStockRepository repo;

    @Value("${app.export.dir}")
    private String exportDir;

    @PreDestroy
    @Transactional
    public void exportOnShutdown() {
        try {
            export();
        } catch (Exception e) {
            log.warn("Stok verileri export edilemedi: {}", e.getMessage());
        }
    }

    public void export() throws Exception {
        List<ProductStock> stocks = repo.findAll();
        if (stocks.isEmpty()) {
            log.info("Stok export atlandı: kayıt yok.");
            return;
        }

        List<Map<String, Object>> rows = stocks.stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", s.getProductId());
            row.put("productName", s.getProductName());
            row.put("availableQuantity", s.getAvailableQuantity());
            row.put("reservedQuantity", s.getReservedQuantity());
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("count", rows.size());
        payload.put("stocks", rows);

        File dir = new File(exportDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Export dizini oluşturulamadı: {}", dir.getAbsolutePath());
        }
        File out = new File(dir, EXPORT_FILE);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(out, payload);

        log.info("Stok verileri export edildi: {} kayıt → {}", rows.size(), out.getAbsolutePath());
    }
}
