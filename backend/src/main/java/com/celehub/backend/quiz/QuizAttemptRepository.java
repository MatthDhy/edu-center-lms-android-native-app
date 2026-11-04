package com.celehub.backend.quiz;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Khóa dòng để 2 request cùng lúc (nộp + tự nộp, resume 2 lần) không giẫm lên nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM QuizAttempt a WHERE a.id = :id")
    Optional<QuizAttempt> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.student.id = :studentId")
    Optional<QuizAttempt> lockByQuizAndStudent(@Param("quizId") Long quizId, @Param("studentId") Long studentId);

    @Query("""
            SELECT a FROM QuizAttempt a JOIN FETCH a.quiz q JOIN FETCH q.course c JOIN FETCH a.student
            WHERE a.id = :id""")
    Optional<QuizAttempt> findDetail(@Param("id") Long id);

    @Query("SELECT a FROM QuizAttempt a WHERE a.student.id = :studentId AND a.quiz.id IN :ids")
    List<QuizAttempt> findByStudentAndQuizIds(@Param("studentId") Long studentId, @Param("ids") Collection<Long> ids);

    @Query("SELECT a FROM QuizAttempt a JOIN FETCH a.student WHERE a.quiz.id = :quizId")
    List<QuizAttempt> findByQuiz(@Param("quizId") Long quizId);

    long countByQuizId(Long quizId);

    @Query("SELECT COUNT(a) FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.submittedAt IS NOT NULL")
    long countSubmitted(@Param("quizId") Long quizId);

    @Query("SELECT AVG(a.score) FROM QuizAttempt a WHERE a.quiz.id = :quizId AND a.submittedAt IS NOT NULL")
    Double avgScore(@Param("quizId") Long quizId);

    @Query("SELECT a.id FROM QuizAttempt a WHERE a.submittedAt IS NULL AND a.endsAt IS NOT NULL AND a.endsAt < :now")
    List<Long> findExpiredIds(@Param("now") LocalDateTime now);
}
