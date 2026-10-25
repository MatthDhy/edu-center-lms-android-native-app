package com.celehub.backend.notification.dto;

import java.time.LocalDateTime;

import com.celehub.backend.notification.Notification;

public record NotificationDto(Long id, String title, String content, boolean read, LocalDateTime createdAt) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getContent(), n.isRead(), n.getCreatedAt());
    }
}
