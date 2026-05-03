package com.ecommerce.product_service.service;

import com.ecommerce.product_service.document.ProductDocument;
import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.event.ProductSyncPublisher;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final com.ecommerce.product_service.client.OrderClient orderClient;
    private final ProductSyncPublisher productSyncPublisher;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Yeni ürün oluşturuluyor: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .active(true)
                .viewCount(0)
                .build();

        if (request.getImages() != null) {
            request.getImages().forEach(url -> {
                com.ecommerce.product_service.entity.ProductImage img = new com.ecommerce.product_service.entity.ProductImage();
                img.setUrl(url);
                img.setProduct(product);
                product.getImages().add(img);
            });
        }

        Product saved = productRepository.save(product);
        indexToElasticsearch(saved);
        productSyncPublisher.publishCreated(saved);
        log.info("Ürün oluşturuldu ve ES'e indexlendi. id={}", saved.getId());
        return toResponse(saved);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    public PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPageResponse(productPage);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));

        if (product.getViewCount() == null) product.setViewCount(0);
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        return toResponse(product);
    }

    @Override
    public PageResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByCategoryAndActiveTrue(category, pageable);
        return toPageResponse(productPage);
    }

    // ── SEARCH (Elasticsearch) ────────────────────────────────────────────────

    @Override
    public PageResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductDocument> esPage = productSearchRepository.searchByKeyword(keyword, pageable);

            List<ProductResponse> responses = esPage.getContent().stream()
                    .map(doc -> {
                        try {
                            Long productId = Long.parseLong(doc.getId());
                            return productRepository.findById(productId)
                                    .map(this::toResponse)
                                    .orElse(null);
                        } catch (Exception e) {
                            log.warn("Ürün JPA'dan alınamadı, id={}: {}", doc.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return PageResponse.<ProductResponse>builder()
                    .content(responses)
                    .pageNumber(esPage.getNumber())
                    .pageSize(esPage.getSize())
                    .totalElements(esPage.getTotalElements())
                    .totalPages(esPage.getTotalPages())
                    .last(esPage.isLast())
                    .build();

        } catch (Exception e) {
            log.warn("Elasticsearch araması başarısız, JPA LIKE sorgusuna düşülüyor: {}", e.getMessage());
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Product> productPage = productRepository.searchByName(keyword, pageable);
            return toPageResponse(productPage);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        product.getImages().clear();
        if (request.getImages() != null) {
            request.getImages().forEach(url -> {
                com.ecommerce.product_service.entity.ProductImage img = new com.ecommerce.product_service.entity.ProductImage();
                img.setUrl(url);
                img.setProduct(product);
                product.getImages().add(img);
            });
        }

        Product updated = productRepository.save(product);
        indexToElasticsearch(updated);
        productSyncPublisher.publishUpdated(updated);
        log.info("Ürün güncellendi ve ES sync edildi. id={}", id);
        return toResponse(updated);
    }

    @Override
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));

        if (product.getStock() + quantity < 0) {
            throw new RuntimeException("Yetersiz stok. Mevcut: " + product.getStock());
        }

        product.setStock(product.getStock() + quantity);
        Product updated = productRepository.save(product);
        indexToElasticsearch(updated);
        productSyncPublisher.publishUpdated(updated);
        return toResponse(updated);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        product.setActive(false);
        productRepository.save(product);

        // ES'den de kaldır (active=false, aramada çıkmamalı)
        try {
            productSearchRepository.deleteById(String.valueOf(id));
            log.info("Ürün ES'den silindi. id={}", id);
        } catch (Exception e) {
            log.warn("Ürün ES'den silinemedi, id={}: {}", id, e.getMessage());
        }

        productSyncPublisher.publishDeleted(id, product.getName());
        log.info("Ürün silindi (soft delete). id={}", id);
    }

    // ── REVIEW ────────────────────────────────────────────────────────────────

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.ecommerce.product_service.dto.ReviewResponse addReview(
            Long productId, com.ecommerce.product_service.dto.ReviewRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        log.info("Yorum denemesi: productId={}, username={}", productId, request.getUsername());
        Boolean hasPurchased = null;
        try {
            hasPurchased = orderClient.checkPurchase(request.getUsername(), productId);
            log.info("checkPurchase yanıtı: productId={}, username={}, hasPurchased={}",
                    productId, request.getUsername(), hasPurchased);
        } catch (Exception e) {
            log.error("Satın alma durumu kontrol edilemedi (Feign): productId={}, username={}, error={}",
                    productId, request.getUsername(), e.toString());
            throw new RuntimeException(
                    "Satın alma durumu doğrulanamadı, lütfen daha sonra tekrar deneyin.");
        }

        if (!Boolean.TRUE.equals(hasPurchased)) {
            throw new RuntimeException(
                    "Bu ürüne yorum yapabilmek için önce satın almış olmanız gerekmektedir.");
        }

        com.ecommerce.product_service.entity.Review review =
                com.ecommerce.product_service.entity.Review.builder()
                        .product(product)
                        .username(request.getUsername())
                        .rating(request.getRating())
                        .comment(request.getComment())
                        .build();

        product.getReviews().add(review);
        productRepository.save(product);

        return com.ecommerce.product_service.dto.ReviewResponse.builder()
                .id(review.getId())
                .username(review.getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // ── ES SYNC HELPER ────────────────────────────────────────────────────────

    public void indexToElasticsearch(Product product) {
        try {
            ProductDocument doc = toDocument(product);
            productSearchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Elasticsearch indexleme başarısız, id={}: {}", product.getId(), e.getMessage());
        }
    }

    public ProductDocument toDocument(Product p) {
        return ProductDocument.builder()
                .id(String.valueOf(p.getId()))
                .name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .price(p.getPrice() != null ? p.getPrice().doubleValue() : 0.0)
                .stock(p.getStock())
                .active(p.getActive())
                .viewCount(p.getViewCount() != null ? p.getViewCount() : 0)
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private ProductResponse toResponse(Product p) {
        Double avgRating = (p.getReviews() == null || p.getReviews().isEmpty()) ? 0.0 :
                p.getReviews().stream()
                        .mapToInt(com.ecommerce.product_service.entity.Review::getRating)
                        .average().orElse(0.0);

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .category(p.getCategory())
                .images(p.getImages() != null
                        ? p.getImages().stream().map(img -> img.getUrl()).toList()
                        : Collections.emptyList())
                .active(p.getActive())
                .viewCount(p.getViewCount() != null ? p.getViewCount() : 0)
                .createdAt(p.getCreatedAt())
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .reviewCount(p.getReviews() != null ? p.getReviews().size() : 0)
                .reviews(p.getReviews() != null
                        ? p.getReviews().stream().map(r ->
                                com.ecommerce.product_service.dto.ReviewResponse.builder()
                                        .id(r.getId())
                                        .username(r.getUsername())
                                        .rating(r.getRating())
                                        .comment(r.getComment())
                                        .createdAt(r.getCreatedAt())
                                        .build()).toList()
                        : Collections.emptyList())
                .build();
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
