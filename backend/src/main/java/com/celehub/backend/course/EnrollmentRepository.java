package com.celehub.backend.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /** Sĩ số một lớp. */
    long countByCourseIdAndStatus(Long courseId, String status);

    /** Số lớp ĐANG học của học viên (cả enrollment và lớp đều ACTIVE). */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE' AND e.course.status = 'ACTIVE'")
    long countActiveCoursesOfStudent(@Param("studentId") Long studentId);

    /** Số học viên khác nhau trong các lớp đang dạy của giảng viên. */
    @Query("SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.status = 'ACTIVE' AND e.course.status = 'ACTIVE'")
    long countActiveStudentsOfTeacher(@Param("teacherId") Long teacherId);
}
