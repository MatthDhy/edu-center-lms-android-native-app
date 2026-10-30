package com.celehub.backend.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * type luôn là "ESSAY" (để thống nhất với TaskDto của Home).
 * Học viên: status = PENDING | OVERDUE | SUBMITTED | GRADED, mySubmission có giá trị nếu đã nộp,
 *           submittedCount/gradedCount/totalStudents = null.
 * Giảng viên / admin: status = null, mySubmission = null, 3 số đếm có giá trị.
 * minutesLeft âm nghĩa là đã quá hạn.
 */
public record AssignmentDto(
        Long id,
        String type,
        Long courseId,
        String courseName,
        String title,
        String description,
        String attachmentUrl,
        LocalDateTime deadline,
        BigDecimal maxScore,
        LocalDateTime createdAt,
        Long minutesLeft,
        String status,
        SubmissionDto mySubmission,
        Long submittedCount,
        Long gradedCount,
        Long totalStudents) {}
