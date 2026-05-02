package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.PageResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy);

    PageResponse<ProductResponse> getProductsByCategory(String category, int page, int size);

    PageResponse<ProductResponse> searchProducts(String keyword, int page, int size);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    List<String> getAllCategories();

    ProductResponse updateStock(Long id, Integer quantity);
}
