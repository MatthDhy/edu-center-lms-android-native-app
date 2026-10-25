package com.celehub.backend.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(Long userId);

    Optional<PasswordResetOtp> findByResetTokenHashAndUsedAtIsNull(String resetTokenHash);

    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.usedAt = :now WHERE o.userId = :userId AND o.usedAt IS NULL")
    int invalidateAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
