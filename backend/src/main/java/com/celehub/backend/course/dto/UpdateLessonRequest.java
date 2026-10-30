package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

import com.celehub.backend.course.LessonStatus;

/** Trường nào null thì giữ nguyên. room = "" nghĩa là bỏ phòng. */
public record UpdateLessonRequest(
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Size(max = 50, message = "Tên phòng tối đa 50 ký tự") String room,
        @Size(max = 200, message = "Chủ đề tối đa 200 ký tự") String topic,
        LessonStatus status) {}
