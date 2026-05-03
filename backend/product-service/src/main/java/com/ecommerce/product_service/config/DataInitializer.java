package com.ecommerce.product_service.config;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.entity.ProductImage;
import com.ecommerce.product_service.lifecycle.ProductDataExporter;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.service.ProductServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductServiceImpl productService;

    @Value("${app.export.dir}")
    private String exportDir;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            File seedFile = new File(exportDir, ProductDataExporter.EXPORT_FILE);
            if (seedFile.exists()) {
                int loaded = loadFromSeedFile(seedFile);
                log.info("Önceki çalıştırmadan {} ürün yüklendi ({}).", loaded, seedFile.getAbsolutePath());
            } else {
                loadDefaultSeed();
            }
        } else {
            log.info("Ürün tablosu zaten dolu, seed atlandı. count={}", productRepository.count());
        }

        // ── Tüm aktif ürünleri Elasticsearch'e reindex et ─────────────────────
        try {
            log.info("Elasticsearch reindex başlatılıyor...");
            List<Product> allProducts = productRepository.findAll();
            int count = 0;
            for (Product product : allProducts) {
                if (Boolean.TRUE.equals(product.getActive())) {
                    productService.indexToElasticsearch(product);
                    count++;
                }
            }
            log.info("Elasticsearch reindex tamamlandı. {} ürün indexlendi.", count);
        } catch (Exception e) {
            log.warn("Elasticsearch reindex başarısız (ES çalışmıyor olabilir): {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private int loadFromSeedFile(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> rows = (List<Map<String, Object>>) payload.get("products");
            if (rows == null) return 0;

            int n = 0;
            for (Map<String, Object> row : rows) {
                Product p = Product.builder()
                        .name(asString(row.get("name")))
                        .description(asString(row.get("description")))
                        .price(asBigDecimal(row.get("price")))
                        .stock(asInt(row.get("stock"), 0))
                        .category(asString(row.get("category")))
                        .active(row.get("active") == null ? Boolean.TRUE : (Boolean) row.get("active"))
                        .viewCount(asInt(row.get("viewCount"), 0))
                        .build();

                Object imgs = row.get("images");
                if (imgs instanceof List<?> list) {
                    for (Object url : list) {
                        if (url == null) continue;
                        ProductImage img = new ProductImage();
                        img.setUrl(url.toString());
                        img.setProduct(p);
                        p.getImages().add(img);
                    }
                }

                productRepository.save(p);
                n++;
            }
            return n;
        } catch (Exception e) {
            log.warn("Seed dosyası okunamadı, varsayılan seed yüklenecek: {}", e.getMessage());
            loadDefaultSeed();
            return (int) productRepository.count();
        }
    }

    private void loadDefaultSeed() {
        log.info("Örnek ürünler yükleniyor...");

        productRepository.save(Product.builder()
                .name("Laptop Pro 15").description("Yüksek performanslı laptop")
                .price(new BigDecimal("24999.99")).stock(50)
                .category("Elektronik").active(true).viewCount(0).build());

        productRepository.save(Product.builder()
                .name("Wireless Mouse").description("Ergonomik kablosuz mouse")
                .price(new BigDecimal("299.99")).stock(100)
                .category("Elektronik").active(true).viewCount(0).build());

        productRepository.save(Product.builder()
                .name("Mekanik Klavye").description("RGB aydınlatmalı mekanik klavye")
                .price(new BigDecimal("899.99")).stock(75)
                .category("Elektronik").active(true).viewCount(0).build());

        productRepository.save(Product.builder()
                .name("Spor Ayakkabı").description("Nefes alan spor ayakkabı")
                .price(new BigDecimal("599.99")).stock(200)
                .category("Giyim").active(true).viewCount(0).build());

        productRepository.save(Product.builder()
                .name("Akıllı Saat").description("Nabız ve uyku takibi özellikli akıllı saat")
                .price(new BigDecimal("3499.99")).stock(30)
                .category("Elektronik").active(true).viewCount(0).build());

        productRepository.save(Product.builder()
                .name("Kitaplık Roman Seti").description("Dünya klasikleri 10 kitap")
                .price(new BigDecimal("450.00")).stock(60)
                .category("Kitap").active(true).viewCount(0).build());

        log.info("6 örnek ürün yüklendi.");
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static Integer asInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private static BigDecimal asBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
