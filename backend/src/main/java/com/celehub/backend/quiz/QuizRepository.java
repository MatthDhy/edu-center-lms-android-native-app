package com.celehub.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /** Quiz đã xuất bản, đang mở, chưa nộp (làm dở vẫn hiện), hạn gần nhất lên đầu. (Home học viên) */
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

    @Query("SELECT q FROM Quiz q JOIN FETCH q.course c JOIN FETCH c.teacher WHERE q.id = :id")
    Optional<Quiz> findDetail(@Param("id") Long id);

    @Query("SELECT q FROM Quiz q JOIN FETCH q.course c WHERE c.id = :courseId ORDER BY q.createdAt DESC, q.id DESC")
    List<Quiz> findByCourse(@Param("courseId") Long courseId);

    /** Quiz đã xuất bản và đã mở của các lớp học viên đang học (courseId = 0: mọi lớp). */
    @Query("""
            SELECT q FROM Quiz q JOIN FETCH q.course c
            WHERE q.published = true AND (q.startTime IS NULL OR q.startTime <= :now)
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
              AND (:courseId = 0L OR c.id = :courseId)
            ORDER BY q.endTime ASC, q.id ASC""")
    List<Quiz> findPublishedForStudent(@Param("studentId") Long studentId, @Param("courseId") long courseId,
                                       @Param("now") LocalDateTime now);

    @Query("""
            SELECT q FROM Quiz q JOIN FETCH q.course c
            WHERE c.teacher.id = :teacherId AND (:courseId = 0L OR c.id = :courseId)
            ORDER BY q.createdAt DESC""")
    List<Quiz> findForTeacher(@Param("teacherId") Long teacherId, @Param("courseId") long courseId);
}
