package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
        @NotNull(message = "Giờ bắt đầu không được để trống") LocalDateTime startTime,
        @NotNull(message = "Giờ kết thúc không được để trống") LocalDateTime endTime,
        @Size(max = 50, message = "Tên phòng tối đa 50 ký tự") String room,
        @Size(max = 200, message = "Chủ đề tối đa 200 ký tự") String topic) {}
