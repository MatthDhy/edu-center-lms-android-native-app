package com.celehub.backend.course.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Trường nào null thì giữ nguyên. status: ACTIVE | CLOSED. */
public record UpdateCourseRequest(
        @Size(max = 150, message = "Tên lớp tối đa 150 ký tự") String courseName,
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") String description,
        Long teacherId,
        @PositiveOrZero(message = "Học phí không hợp lệ") Long tuitionFee,
        @Positive(message = "Số buổi phải lớn hơn 0") Integer totalSessions,
        @Pattern(regexp = "ACTIVE|CLOSED", message = "Trạng thái chỉ nhận ACTIVE hoặc CLOSED") String status) {}
