package com.celehub.backend.course;

import java.util.List;
import java.util.Optional;

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

    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

    boolean existsByCourseIdAndStudentIdAndStatus(Long courseId, Long studentId, String status);

    @Query("""
            SELECT e FROM Enrollment e JOIN FETCH e.student s
            WHERE e.course.id = :courseId AND e.status = 'ACTIVE'
            ORDER BY s.fullName ASC""")
    List<Enrollment> findActiveByCourse(@Param("courseId") Long courseId);

    @Query("SELECT e.student.id FROM Enrollment e WHERE e.course.id = :courseId AND e.status = 'ACTIVE'")
    List<Long> findActiveStudentIds(@Param("courseId") Long courseId);
}
