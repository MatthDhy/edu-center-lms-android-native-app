package com.celehub.backend.assignment.dto;

import java.util.List;

import com.celehub.backend.user.dto.StudentBriefDto;

/** Màn "Đã nộp / Chưa nộp" của giảng viên. */
public record SubmissionsOverviewDto(
        AssignmentDto assignment,
        List<SubmissionDto> submitted,
        List<StudentBriefDto> notSubmitted) {}
