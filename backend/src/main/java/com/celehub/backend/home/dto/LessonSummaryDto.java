package com.celehub.backend.home.dto;

import java.time.LocalDateTime;

/**
 * Một buổi học trên Home.
 * Học viên: teacherName có giá trị, studentCount = null.
 * Giảng viên: studentCount có giá trị (sĩ số lớp), teacherName = chính mình.
 * minutesUntilStart = 0 và ongoing = true khi buổi học đã bắt đầu nhưng chưa kết thúc.
 */
public record LessonSummaryDto(
        Long lessonId,
        Long courseId,
        String courseName,
        String teacherName,
        String room,
        String topic,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long minutesUntilStart,
        boolean ongoing,
        Integer studentCount) {}
