package com.celehub.backend.notification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken = :token AND d.userId = :userId")
    int deleteByTokenAndUser(@Param("token") String token, @Param("userId") Long userId);
}
