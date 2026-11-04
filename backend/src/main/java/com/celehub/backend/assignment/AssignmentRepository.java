package com.celehub.backend.assignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c
            WHERE a.deadline IS NOT NULL AND a.deadline > :now
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND NOT EXISTS (SELECT s.id FROM Submission s WHERE s.assignment.id = a.id AND s.student.id = :studentId)
            ORDER BY a.deadline ASC""")
    List<Assignment> findOpenForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                                        Pageable pageable);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c
            WHERE c.teacher.id = :teacherId AND a.deadline IS NOT NULL AND a.deadline > :now
            ORDER BY a.deadline ASC""")
    List<Assignment> findOpenForTeacher(@Param("teacherId") Long teacherId, @Param("now") LocalDateTime now,
                                        Pageable pageable);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE a.id = :id""")
    Optional<Assignment> findDetail(@Param("id") Long id);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE c.id = :courseId ORDER BY a.deadline DESC, a.id DESC""")
    List<Assignment> findByCourse(@Param("courseId") Long courseId);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND (:courseId = 0L OR c.id = :courseId)
            ORDER BY a.deadline ASC, a.id ASC""")
    List<Assignment> findAllForStudent(@Param("studentId") Long studentId, @Param("courseId") long courseId);

    /** Mọi bài tập của giảng viên (courseId = 0: mọi lớp). Dùng cho tab Bài tập của giảng viên. */
    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c
            WHERE c.teacher.id = :teacherId AND (:courseId = 0L OR c.id = :courseId)
            ORDER BY a.createdAt DESC""")
    List<Assignment> findForTeacher(@Param("teacherId") Long teacherId, @Param("courseId") long courseId);
}
