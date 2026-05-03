package com.ecommerce.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/orders/check-purchase")
    Boolean checkPurchase(@RequestParam("username") String username, @RequestParam("productId") Long productId);
}
