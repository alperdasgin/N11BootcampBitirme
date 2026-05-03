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

        saveWithImages(Product.builder()
                .name("Laptop Pro 15").description("Yüksek performanslı laptop")
                .price(new BigDecimal("24999.99")).stock(50)
                .category("Elektronik").active(true).viewCount(0).build(),
                List.of(
                        "https://static.ticimax.cloud/cdn-cgi/image/width=-,quality=85/54992/uploads/urunresimleri/buyuk/macbook-pro-13-8-core-cpu-8-core-gpu-a-b45b-4.jpeg",
                        "https://productimages.hepsiburada.net/s/777/300-400/110000951875381.jpg",
                        "https://sm.pcmag.com/pcmag_me/photo/default/macbook-6_hgfm.jpg"
                ));

        saveWithImages(Product.builder()
                .name("Wireless Mouse").description("Ergonomik kablosuz mouse")
                .price(new BigDecimal("299.99")).stock(100)
                .category("Elektronik").active(true).viewCount(0).build(),
                List.of(
                        "https://productimages.hepsiburada.net/s/45/375-375/10828357533746.jpg",
                        "https://www.hpstore.com.tr/hp-z3700-kablosuz-mouse-gri-758a9aa-14821-18-B.jpg",
                        "https://cdn.akakce.com/z/hadron/hadron-hdx3407-kablosuz-optik.jpg",
                        "https://cdn.dsmcdn.com/mnresize/420/620/ty1569/prod/QC/20240926/00/d88597cc-2a74-3412-83d9-0c0556062bee/1_org_zoom.jpg"
                ));

        saveWithImages(Product.builder()
                .name("Mekanik Klavye").description("RGB aydınlatmalı mekanik klavye")
                .price(new BigDecimal("899.99")).stock(75)
                .category("Elektronik").active(true).viewCount(0).build(),
                List.of(
                        "https://ideacdn.net/idea/fp/51/myassets/products/002/gamebooster-shock-g25-gorsel01.jpg?revision=1697143329",
                        "https://m.media-amazon.com/images/I/714Y4KXLVPL._AC_UF1000,1000_QL80_.jpg",
                        "https://cdn.evkur.com.tr/c/Product/thumbs/22_r5g0kb_500.jpg",
                        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSAMr9DrBIzjM5VznT07MVCcFbOXtsDg7MUrg&s"
                ));

        saveWithImages(Product.builder()
                .name("Spor Ayakkabı").description("Nefes alan spor ayakkabı")
                .price(new BigDecimal("599.99")).stock(200)
                .category("Giyim").active(true).viewCount(0).build(),
                List.of(
                        "https://m.media-amazon.com/images/I/41FHna7VtEL._AC_.jpg",
                        "https://www.libas.com.tr/cdn/shop/files/7419600193-1.jpg?crop=center&height=1200&v=1732286022&width=1200",
                        "https://8f08a8-hoss.akinoncloudcdn.com/products/2026/02/25/76771249/a7015a19-184b-4eaa-a79b-387fcc4b14f1_size740x740_cropCenter.jpg",
                        "https://newbalance.sm.mncdn.com/mnresize/750/-/newbalance/products/NBU9060SRA_3.jpg"
                ));

        saveWithImages(Product.builder()
                .name("Akıllı Saat").description("Nabız ve uyku takibi özellikli akıllı saat")
                .price(new BigDecimal("3499.99")).stock(30)
                .category("Elektronik").active(true).viewCount(0).build(),
                List.of(
                        "https://m.media-amazon.com/images/I/71DvUOAGdsL._AC_UF1000,1000_QL80_.jpg",
                        "https://www.beko.com.tr/media/resize/9241641600_LO1_20240912_164634.png/1000Wx1000H/image.webp",
                        "https://cdn.evkur.com.tr/c/Product/thumbs/siyah-1_n4y3aj_500.jpg"
                ));

        saveWithImages(Product.builder()
                .name("Kitaplık Roman Seti").description("Dünya klasikleri 10 kitap")
                .price(new BigDecimal("450.00")).stock(60)
                .category("Kitap").active(true).viewCount(0).build(),
                List.of(
                        "https://productimages.hepsiburada.net/s/140/375-375/110000092943570.jpg",
                        "https://www.kitapkolik.com/wp-content/uploads/2022/12/Dunya-1-2-3.jpg",
                        "https://i.dr.com.tr/cache/600x600-0/originals/0002002728001-1.jpg",
                        "https://www.profkitap.com/dunya-klasikleri-seti-3-12-kitap-nef-kitap-nef-kitap-2345-41-B.webp"
                ));

        saveWithImages(Product.builder()
                .name("Güneş Gözlüğü").description("UV korumalı şık güneş gözlüğü")
                .price(new BigDecimal("749.99")).stock(80)
                .category("Giyim").active(true).viewCount(0).build(),
                List.of(
                        "https://cdn.ozkanoptik.com/burberry-cocuk-kirmizi-gunes-gozlugu-4339-391984-48-23006-12-B.jpg",
                        "https://261239-lacostetr.akinoncloudcdn.com/products/2022/04/13/183337/fbfb103c-25b8-416f-829a-9bffde4b79dd_cropCenter.jpg",
                        "https://cdn.myikas.com/images/d6f9ffbc-eaac-4c23-84c6-530a77d57b55/8a75ec97-aa45-4596-aa1f-6c6ef5adc2f5/3840/gozluk-5-2000-px-1.webp",
                        "https://cdn.dsmcdn.com/mnresize/420/620/ty1350/product/media/images/prod/QC/20240607/18/39393305-2b43-3b15-ad77-305b247556e3/1_org_zoom.jpg",
                        "https://static.ticimax.cloud/48567/uploads/urunresimleri/buyuk/calvin-klein-ckj-23656s-675-52-kadin-c-c0-8a9.jpg",
                        "https://static.ticimax.cloud/65800/uploads/urunresimleri/buyuk/yeni-gold-besgen-cerceveli-kirmizi-camli-7ac8.jpg"
                ));

        log.info("7 örnek ürün yüklendi.");
    }

    private void saveWithImages(Product product, List<String> imageUrls) {
        imageUrls.forEach(url -> {
            ProductImage img = new ProductImage();
            img.setUrl(url);
            img.setProduct(product);
            product.getImages().add(img);
        });
        productRepository.save(product);
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
