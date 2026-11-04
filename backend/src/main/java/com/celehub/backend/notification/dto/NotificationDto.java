package com.celehub.backend.notification.dto;

import java.time.LocalDateTime;

import com.celehub.backend.notification.Notification;
import com.celehub.backend.notification.NotificationType;

/** refType: ASSIGNMENT | QUIZ | LESSON | TUITION | MATERIAL — app dùng (refType, refId) để nhảy đúng màn. */
public record NotificationDto(Long id, String title, String content, boolean read, LocalDateTime createdAt,
                              NotificationType type, Long courseId, String imageUrl, String refType, Long refId) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getContent(), n.isRead(), n.getCreatedAt(),
                n.getType(), n.getCourseId(), n.getImageUrl(), n.getRefType(), n.getRefId());
    }
}
