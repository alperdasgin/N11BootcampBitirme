package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findTopByEmailOrderByIdDesc(String email);
}
