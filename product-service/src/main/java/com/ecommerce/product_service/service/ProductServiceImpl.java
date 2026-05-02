package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final com.ecommerce.product_service.client.OrderClient orderClient;

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
        log.info("Ürün oluşturuldu. id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPageResponse(productPage);
    }

    @Override
    public PageResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByCategoryAndActiveTrue(category, pageable);
        return toPageResponse(productPage);
    }

    @Override
    public PageResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.searchByName(keyword, pageable);
        return toPageResponse(productPage);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        
        // Ürün görüntülendiğinde sayacı 1 artır
        if (product.getViewCount() == null) {
            product.setViewCount(0);
        }
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        return toResponse(product);
    }

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
        log.info("Ürün güncellendi. id={}", id);
        return toResponse(updated);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Ürün silindi (soft delete). id={}", id);
    }

    @Override
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    @Override
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));
        if (product.getStock() + quantity < 0) {
            throw new RuntimeException("Yetersiz stok. Mevcut: " + product.getStock());
        }
        product.setStock(product.getStock() + quantity);
        return toResponse(productRepository.save(product));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.ecommerce.product_service.dto.ReviewResponse addReview(Long productId, com.ecommerce.product_service.dto.ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı"));

        Boolean hasPurchased = false;
        try {
            hasPurchased = orderClient.checkPurchase(request.getUsername(), productId);
        } catch (Exception e) {
            log.error("Satın alma durumu kontrol edilemedi: {}", e.getMessage());
        }

        if (Boolean.FALSE.equals(hasPurchased)) {
            throw new RuntimeException("Bu ürüne yorum yapabilmek için önce satın almış olmanız gerekmektedir.");
        }

        com.ecommerce.product_service.entity.Review review = com.ecommerce.product_service.entity.Review.builder()
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

    private ProductResponse toResponse(Product p) {
        Double avgRating = p.getReviews() == null || p.getReviews().isEmpty() ? 0.0 :
                p.getReviews().stream().mapToInt(com.ecommerce.product_service.entity.Review::getRating).average().orElse(0.0);

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .category(p.getCategory())
                .images(p.getImages() != null ? p.getImages().stream().map(img -> img.getUrl()).toList() : java.util.Collections.emptyList())
                .active(p.getActive())
                .viewCount(p.getViewCount() != null ? p.getViewCount() : 0)
                .createdAt(p.getCreatedAt())
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .reviewCount(p.getReviews() != null ? p.getReviews().size() : 0)
                .reviews(p.getReviews() != null ? p.getReviews().stream().map(r -> com.ecommerce.product_service.dto.ReviewResponse.builder()
                        .id(r.getId())
                        .username(r.getUsername())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build()).toList() : java.util.Collections.emptyList())
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
