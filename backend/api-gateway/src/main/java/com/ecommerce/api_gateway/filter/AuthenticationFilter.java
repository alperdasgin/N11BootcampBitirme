package com.ecommerce.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    // JWT doğrulaması gerektirmeyen endpointler
    private static final java.util.List<String> OPEN_ENDPOINTS = java.util.List.of(
            "/api/user/signin",
            "/api/user/signup",
            "/api/user/verify"
    );

    // Sadece ADMIN'in yazma işlemi yapabileceği path prefix'leri
    private static final java.util.List<String> ADMIN_WRITE_PATHS = java.util.List.of(
            "/api/products"
    );

    // ADMIN_WRITE_PATHS prefix'i altında olsa bile normal kullanıcının yazabileceği endpointler
    // Örn: POST /api/products/{id}/reviews — kullanıcı yorum ekleyebilmeli
    private static final java.util.regex.Pattern USER_WRITABLE_UNDER_ADMIN_PATH =
            java.util.regex.Pattern.compile("^/api/products/\\d+/reviews/?$");

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();

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

            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            // ADMIN-only write endpoint kontrolü (POST, PUT, DELETE)
            boolean isWriteMethod = method == HttpMethod.POST
                    || method == HttpMethod.PUT
                    || method == HttpMethod.DELETE;
            boolean isAdminPath = ADMIN_WRITE_PATHS.stream().anyMatch(path::startsWith);
            boolean isUserWritableException = USER_WRITABLE_UNDER_ADMIN_PATH.matcher(path).matches();

            if (isAdminPath && isWriteMethod && !isUserWritableException && !"ADMIN".equals(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Username ve Role'ü downstream servislere ilet
            var mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Name", username)
                    .header("X-User-Role", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {
        // Gerekirse config parametreleri buraya
    }
}