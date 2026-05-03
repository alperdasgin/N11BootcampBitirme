package com.ecommerce.product_service.config;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.service.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductServiceImpl productService;

    @Override
    public void run(String... args) {

        // ── Örnek veri yükle ──────────────────────────────────────────────────
        if (productRepository.count() == 0) {
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
}
