package com.celehub.backend.home.dto;

import java.time.LocalDateTime;

/** Bài tập tự luận (type = ESSAY) hoặc Quiz (type = QUIZ) sắp đến hạn. urgent = hạn trong vòng 24 giờ. */
public record TaskDto(
        Long id,
        String type,
        String title,
        String courseName,
        LocalDateTime deadline,
        long minutesLeft,
        boolean urgent) {}
