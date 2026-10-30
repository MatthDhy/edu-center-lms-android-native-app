package com.celehub.backend.assignment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotNull(message = "Điểm không được để trống")
        @DecimalMin(value = "0", message = "Điểm không được âm") BigDecimal score,
        @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String feedback) {}
