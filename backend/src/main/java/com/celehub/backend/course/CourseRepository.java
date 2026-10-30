package com.celehub.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {
    long countByTeacherIdAndStatus(Long teacherId, String status);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.id = :id")
    Optional<Course> findWithTeacher(@Param("id") Long id);

    /** status = "" / teacherId = 0 / search = "" nghĩa là không lọc (tránh tham số NULL trên PostgreSQL). */
    @Query("""
            SELECT c FROM Course c JOIN FETCH c.teacher t
            WHERE (:status = '' OR c.status = :status)
              AND (:teacherId = 0L OR t.id = :teacherId)
              AND (:search = '' OR LOWER(c.courseName) LIKE CONCAT('%', :search, '%'))
            ORDER BY c.id DESC""")
    List<Course> search(@Param("status") String status, @Param("teacherId") long teacherId,
                        @Param("search") String search);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.teacher.id = :teacherId ORDER BY c.status ASC, c.id DESC")
    List<Course> findByTeacher(@Param("teacherId") Long teacherId);

    @Query("""
            SELECT c FROM Course c JOIN FETCH c.teacher
            WHERE c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
            ORDER BY c.status ASC, c.id DESC""")
    List<Course> findByStudent(@Param("studentId") Long studentId);
}
