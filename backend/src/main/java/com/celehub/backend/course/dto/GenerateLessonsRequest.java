package com.celehub.backend.course.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sinh lịch lặp. daysOfWeek: 1 = Thứ 2 ... 7 = Chủ nhật. startDate "2026-10-26", giờ "18:00". */
public record GenerateLessonsRequest(
        @NotNull(message = "Ngày bắt đầu không được để trống") LocalDate startDate,
        @NotEmpty(message = "Chọn ít nhất 1 thứ trong tuần") List<@Min(1) @Max(7) Integer> daysOfWeek,
        @NotNull(message = "Giờ bắt đầu không được để trống") LocalTime startTime,
        @NotNull(message = "Giờ kết thúc không được để trống") LocalTime endTime,
        @Size(max = 50, message = "Tên phòng tối đa 50 ký tự") String room,
        @Size(max = 200, message = "Chủ đề tối đa 200 ký tự") String topic,
        @NotNull(message = "Số buổi không được để trống")
        @Min(value = 1, message = "Số buổi tối thiểu là 1")
        @Max(value = 200, message = "Số buổi tối đa là 200") Integer sessions) {}
