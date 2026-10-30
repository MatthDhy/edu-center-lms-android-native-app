package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

import com.celehub.backend.course.Course;
import com.celehub.backend.course.Lesson;
import com.celehub.backend.course.LessonStatus;

public record LessonDto(
        Long id,
        Long courseId,
        String courseName,
        Long teacherId,
        String teacherName,
        String room,
        String topic,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LessonStatus status) {

    /** Gọi trong transaction; course và teacher phải đã được nạp (JOIN FETCH). */
    public static LessonDto from(Lesson l) {
        Course c = l.getCourse();
        return new LessonDto(l.getId(), c.getId(), c.getCourseName(), c.getTeacher().getId(), c.getTeacher().getFullName(),
                l.getRoom(), l.getTopic(), l.getStartTime(), l.getEndTime(), l.getStatus());
    }
}
