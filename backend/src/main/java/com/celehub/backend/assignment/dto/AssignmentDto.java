package com.celehub.backend.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * type = "ESSAY" (bài tập) hoặc "QUIZ" (khi GET /api/assignments/mine?type=ALL|QUIZ).
 * Học viên: status = PENDING | OVERDUE | SUBMITTED | GRADED, mySubmission có giá trị nếu đã nộp,
 *           submittedCount/gradedCount/totalStudents = null.
 * Giảng viên / admin: status = null, mySubmission = null, 3 số đếm có giá trị.
 * minutesLeft âm nghĩa là đã quá hạn.
 * graded = bài có chấm điểm tay (essay). Bài graded=false chỉ có "Đã nộp", không điểm.
 * Chỉ dành cho QUIZ (essay: null): quizStatus = NOT_STARTED | IN_PROGRESS | SUBMITTED | MISSED,
 *   quizScore, exitCount (số lần đã thoát app), durationMinutes, attemptId (dùng gọi /review).
 *   Quiz đã nộp có status = GRADED; quiz bỏ lỡ có status = OVERDUE.
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
        Long totalStudents,
        boolean graded,
        String quizStatus,
        BigDecimal quizScore,
        Integer exitCount,
        Integer durationMinutes,
        Long attemptId) {}
