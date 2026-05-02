package com.ecommerce.api_gateway.config;

import com.ecommerce.api_gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authFilter;

    public GatewayConfig(AuthenticationFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // User Service - signup/signin açık, diğerleri JWT korumalı
                .route("user-service", r -> r.path("/api/user/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://user-service"))

                // Product Service - JWT korumalı
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://product-service"))

                // Cart Service - JWT korumalı
                .route("cart-service", r -> r.path("/api/cart/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://cart-service"))

                // Order Service - JWT korumalı
                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://order-service"))

                // Payment Service - JWT korumalı
                .route("payment-service", r -> r.path("/api/payments/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://payment-service"))

                // Stock Service - JWT korumalı
                .route("stock-service", r -> r.path("/api/stocks/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://stock-service"))

                .build();
    }
}