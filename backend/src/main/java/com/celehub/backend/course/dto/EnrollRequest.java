package com.celehub.backend.course.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record EnrollRequest(
        @NotEmpty(message = "Chọn ít nhất 1 học viên") List<Long> studentIds) {}
