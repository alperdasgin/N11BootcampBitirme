package com.ecommerce.product_service.config;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("Örnek ürünler yükleniyor...");

            productRepository.save(Product.builder()
                    .name("Laptop Pro 15").description("Yüksek performanslı laptop")
                    .price(new BigDecimal("24999.99")).stock(50)
                    .category("Elektronik").active(true).build());

            productRepository.save(Product.builder()
                    .name("Wireless Mouse").description("Ergonomik kablosuz mouse")
                    .price(new BigDecimal("299.99")).stock(100)
                    .category("Elektronik").active(true).build());

            productRepository.save(Product.builder()
                    .name("Mekanik Klavye").description("RGB aydınlatmalı mekanik klavye")
                    .price(new BigDecimal("899.99")).stock(75)
                    .category("Elektronik").active(true).build());

            productRepository.save(Product.builder()
                    .name("Spor Ayakkabı").description("Nefes alan spor ayakkabı")
                    .price(new BigDecimal("599.99")).stock(200)
                    .category("Giyim").active(true).build());

            productRepository.save(Product.builder()
                    .name("Akıllı Saat").description("Nabız ve uyku takibi")
                    .price(new BigDecimal("3499.99")).stock(30)
                    .category("Elektronik").active(true).build());

            productRepository.save(Product.builder()
                    .name("Kitaplık Roman Seti").description("Dünya klasikleri 10 kitap")
                    .price(new BigDecimal("450.00")).stock(60)
                    .category("Kitap").active(true).build());

            log.info("6 örnek ürün yüklendi.");
        }
    }
}