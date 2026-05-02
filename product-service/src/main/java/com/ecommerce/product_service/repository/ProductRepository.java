package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Aktif ürünleri sayfalı getir
    Page<Product> findByActiveTrue(Pageable pageable);

    // Kategoriye göre filtrele
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

    // İsme göre arama (pagination ile)
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // Kategorileri listele
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true")
    List<String> findAllCategories();
}