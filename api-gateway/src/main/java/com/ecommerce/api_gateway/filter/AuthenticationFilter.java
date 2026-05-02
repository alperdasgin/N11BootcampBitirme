package com.ecommerce.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    // JWT doğrulaması gerektirmeyen endpointler
    private static final java.util.List<String> OPEN_ENDPOINTS = java.util.List.of(
            "/api/user/signin",
            "/api/user/signup"
    );

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Open endpoint ise doğrulamayı atla
            boolean isOpen = OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
            if (isOpen) {
                return chain.filter(exchange);
            }

            // Authorization header kontrol
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Token geçerliyse username'i header'a ekle (downstream servisler kullanabilir)
            String username = jwtUtil.extractUsername(token);
            exchange.getRequest().mutate()
                    .header("X-User-Name", username)
                    .build();

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Gerekirse config parametreleri buraya
    }
}