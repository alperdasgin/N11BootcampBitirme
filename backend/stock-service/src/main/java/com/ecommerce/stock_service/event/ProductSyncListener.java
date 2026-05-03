package com.ecommerce.stock_service.event;

import com.ecommerce.stock_service.entity.ProductStock;
import com.ecommerce.stock_service.repository.ProductStockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSyncListener {

    private final ProductStockRepository repo;

    @Transactional
    @RabbitListener(queues = "${product.sync.queue}")
    public void onProductSync(ProductSyncEvent event) {
        log.info("ProductSyncEvent alındı: type={}, productId={}, stock={}",
                event.getType(), event.getProductId(), event.getStock());

        switch (event.getType()) {
            case CREATED -> upsert(event);
            case UPDATED -> upsert(event);
            case DELETED -> delete(event.getProductId());
        }
    }

    private void upsert(ProductSyncEvent event) {
        Long productId = event.getProductId();
        Integer newStock = event.getStock() != null ? event.getStock() : 0;
        String name = event.getProductName() != null ? event.getProductName() : ("Product " + productId);

        ProductStock existing = repo.findById(productId).orElse(null);
        if (existing == null) {
            ProductStock fresh = new ProductStock(productId, name, newStock, 0);
            repo.save(fresh);
            log.info("Yeni ProductStock oluşturuldu: productId={}, available={}", productId, newStock);
        } else {
            existing.setProductName(name);
            existing.setAvailableQuantity(newStock);
            // reservedQuantity'e dokunma — saga (sipariş) onun sahibi
            repo.save(existing);
            log.info("ProductStock güncellendi: productId={}, available={}, reserved={}",
                    productId, newStock, existing.getReservedQuantity());
        }
    }

    private void delete(Long productId) {
        repo.findById(productId).ifPresent(ps -> {
            if (ps.getReservedQuantity() != null && ps.getReservedQuantity() > 0) {
                log.warn("Silinen ürünün rezerve stoğu var (productId={}, reserved={}) — yine de siliniyor.",
                        productId, ps.getReservedQuantity());
            }
            repo.deleteById(productId);
            log.info("ProductStock silindi: productId={}", productId);
        });
    }
}
