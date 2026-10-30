package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

/** tuitionFee: số nguyên VNĐ. lessonCount = số buổi chưa hủy, heldCount = số buổi đã kết thúc. */
public record CourseDto(
        Long id,
        String courseName,
        String description,
        Long teacherId,
        String teacherName,
        long tuitionFee,
        Integer totalSessions,
        String status,
        long studentCount,
        long lessonCount,
        long heldCount,
        LocalDateTime createdAt) {}
