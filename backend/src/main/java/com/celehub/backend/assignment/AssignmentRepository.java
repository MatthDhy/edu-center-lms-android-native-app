package com.celehub.backend.assignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /** Bài tập còn hạn mà học viên CHƯA nộp, hạn gần nhất lên đầu. (dùng cho Home) */
    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c
            WHERE a.deadline IS NOT NULL AND a.deadline > :now
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND NOT EXISTS (SELECT s.id FROM Submission s WHERE s.assignment.id = a.id AND s.student.id = :studentId)
            ORDER BY a.deadline ASC""")
    List<Assignment> findOpenForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                                        Pageable pageable);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE a.id = :id""")
    Optional<Assignment> findDetail(@Param("id") Long id);

    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE c.id = :courseId ORDER BY a.deadline DESC, a.id DESC""")
    List<Assignment> findByCourse(@Param("courseId") Long courseId);

    /** Mọi bài tập thuộc các lớp học viên đang học. courseId = 0 nghĩa là tất cả lớp. */
    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND (:courseId = 0L OR c.id = :courseId)
            ORDER BY a.deadline ASC, a.id ASC""")
    List<Assignment> findAllForStudent(@Param("studentId") Long studentId, @Param("courseId") long courseId);
}
