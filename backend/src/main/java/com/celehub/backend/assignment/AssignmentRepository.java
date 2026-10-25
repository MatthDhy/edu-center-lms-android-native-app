package com.celehub.backend.assignment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /** Bài tập còn hạn mà học viên CHƯA nộp, hạn gần nhất lên đầu. */
    @Query("""
            SELECT a FROM Assignment a JOIN FETCH a.course c
            WHERE a.deadline IS NOT NULL AND a.deadline > :now
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND NOT EXISTS (SELECT s.id FROM Submission s WHERE s.assignment.id = a.id AND s.student.id = :studentId)
            ORDER BY a.deadline ASC""")
    List<Assignment> findOpenForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                                        Pageable pageable);
}
