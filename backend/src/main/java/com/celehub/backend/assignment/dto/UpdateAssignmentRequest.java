package com.celehub.backend.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/** Trường nào null thì giữ nguyên. */
public record UpdateAssignmentRequest(
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        LocalDateTime deadline,
        @DecimalMin(value = "0.01", message = "Điểm tối đa phải lớn hơn 0")
        @DecimalMax(value = "999.99", message = "Điểm tối đa không hợp lệ") BigDecimal maxScore) {}
