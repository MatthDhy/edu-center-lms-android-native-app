package com.celehub.backend.home.dto;

import java.time.LocalDateTime;

/** Bài nộp đang chờ giảng viên chấm. */
public record SubmissionSummaryDto(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        String courseName,
        String studentName,
        LocalDateTime submittedAt,
        boolean late) {}
