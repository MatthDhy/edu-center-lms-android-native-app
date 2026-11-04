package com.celehub.backend.task;

import java.time.LocalDateTime;

/** Một dòng của tab Bài tập (giảng viên): bài tập tự luận hoặc quiz. type = ESSAY | QUIZ. */
public record TeacherTaskDto(String type, Long id, Long courseId, String courseName, String title,
                             LocalDateTime deadline, boolean graded, boolean published,
                             long submittedCount, long gradedCount, long totalStudents,
                             Double avgScore, LocalDateTime createdAt) {}
