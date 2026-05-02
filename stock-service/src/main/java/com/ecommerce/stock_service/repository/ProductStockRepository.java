package com.ecommerce.stock_service.repository;

import com.ecommerce.stock_service.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {}