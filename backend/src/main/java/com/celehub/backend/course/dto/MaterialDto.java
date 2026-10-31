package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

import com.celehub.backend.course.Material;

public record MaterialDto(Long id, Long courseId, String title, String description,
                          String fileUrl, String uploadedByName, LocalDateTime createdAt) {
    /** Gọi trong transaction. */
    public static MaterialDto from(Material m) {
        return new MaterialDto(m.getId(), m.getCourse().getId(), m.getTitle(), m.getDescription(),
                m.getFileUrl(), m.getUploadedBy() == null ? null : m.getUploadedBy().getFullName(),
                m.getCreatedAt());
    }
}