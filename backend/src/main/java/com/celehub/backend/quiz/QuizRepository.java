package com.celehub.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /** Quiz đã xuất bản, đang mở, chưa nộp, hạn gần nhất lên đầu. */
    @Query("""
            SELECT q FROM Quiz q JOIN FETCH q.course c
            WHERE q.published = true AND q.endTime IS NOT NULL AND q.endTime > :now
              AND (q.startTime IS NULL OR q.startTime <= :now)
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND NOT EXISTS (SELECT qa.id FROM QuizAttempt qa
                              WHERE qa.quiz.id = q.id AND qa.student.id = :studentId AND qa.submittedAt IS NOT NULL)
            ORDER BY q.endTime ASC""")
    List<Quiz> findOpenForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                                  Pageable pageable);
}
