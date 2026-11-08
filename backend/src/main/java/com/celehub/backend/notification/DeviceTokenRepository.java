package com.celehub.backend.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken = :token AND d.userId = :userId")
    int deleteByTokenAndUser(@Param("token") String token, @Param("userId") Long userId);

    /** Dọn token chết (app bị gỡ / token hết hạn) sau khi FCM báo lỗi. */
    @Transactional
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken IN :tokens")
    int deleteByTokens(@Param("tokens") Collection<String> tokens);
}
