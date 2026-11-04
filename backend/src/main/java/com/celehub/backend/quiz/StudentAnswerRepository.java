package com.celehub.backend.quiz;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findByAttemptId(Long attemptId);

    Optional<StudentAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    /** [questionId, số người trả lời, số người trả lời đúng] — chỉ tính bài đã nộp. */
    @Query("""
            SELECT sa.questionId, COUNT(sa), SUM(CASE WHEN o.correct = true THEN 1 ELSE 0 END)
            FROM StudentAnswer sa, QuestionOption o, QuizAttempt a
            WHERE o.id = sa.selectedOptionId AND a.id = sa.attemptId
              AND a.quiz.id = :quizId AND a.submittedAt IS NOT NULL
            GROUP BY sa.questionId""")
    List<Object[]> questionStats(@Param("quizId") Long quizId);
}
