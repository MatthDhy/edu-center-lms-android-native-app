package com.celehub.backend.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalse(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndType(Long userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndTypeAndReadFalse(Long userId, NotificationType type, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false GROUP BY n.type")
    List<Object[]> countUnreadByType(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false AND n.type = :type")
    int markAllReadByType(@Param("userId") Long userId, @Param("type") NotificationType type);
}
