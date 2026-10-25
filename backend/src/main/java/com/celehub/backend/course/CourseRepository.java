package com.celehub.backend.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    long countByTeacherIdAndStatus(Long teacherId, String status);
}
