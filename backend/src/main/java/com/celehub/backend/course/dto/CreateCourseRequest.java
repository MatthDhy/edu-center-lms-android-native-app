package com.celehub.backend.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank(message = "Tên lớp không được để trống")
        @Size(max = 150, message = "Tên lớp tối đa 150 ký tự") String courseName,
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") String description,
        @NotNull(message = "Phải chọn giảng viên") Long teacherId,
        @PositiveOrZero(message = "Học phí không hợp lệ") Long tuitionFee,
        @Positive(message = "Số buổi phải lớn hơn 0") Integer totalSessions) {}
