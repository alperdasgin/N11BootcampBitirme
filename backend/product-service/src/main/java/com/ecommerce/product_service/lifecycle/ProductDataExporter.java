package com.ecommerce.product_service.lifecycle;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataExporter {

    public static final String EXPORT_FILE = "products-seed.json";

    private final ProductRepository productRepository;

    @Value("${app.export.dir}")
    private String exportDir;

    @PreDestroy
    @Transactional
    public void exportOnShutdown() {
        try {
            export();
        } catch (Exception e) {
            log.warn("Ürün verileri export edilemedi: {}", e.getMessage());
        }
    }

    public void export() throws Exception {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            log.info("Export atlandı: ürün yok.");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        List<Map<String, Object>> rows = products.stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("name", p.getName());
            row.put("description", p.getDescription());
            row.put("price", p.getPrice());
            row.put("stock", p.getStock());
            row.put("category", p.getCategory());
            row.put("active", p.getActive());
            row.put("viewCount", p.getViewCount());
            row.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().format(fmt) : null);
            row.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().format(fmt) : null);
            row.put("images", p.getImages() == null ? List.of() :
                    p.getImages().stream()
                            .map(img -> img.getUrl())
                            .collect(Collectors.toList()));
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportedAt", java.time.LocalDateTime.now().format(fmt));
        payload.put("count", rows.size());
        payload.put("products", rows);

        File dir = new File(exportDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Export dizini oluşturulamadı: {}", dir.getAbsolutePath());
        }
        File out = new File(dir, EXPORT_FILE);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(out, payload);

        log.info("Ürün verileri export edildi: {} ürün → {}", rows.size(), out.getAbsolutePath());
    }
}
