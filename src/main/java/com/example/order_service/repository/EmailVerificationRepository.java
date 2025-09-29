package com.example.order_service.repository;

import com.example.order_service.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByToken(String token);

    Optional<EmailVerification> findByEmailAndVerificationCodeAndIsUsedFalse(String email, String verificationCode);

    Optional<EmailVerification> findByEmailAndIsUsedFalse(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteByEmail(String email);
}