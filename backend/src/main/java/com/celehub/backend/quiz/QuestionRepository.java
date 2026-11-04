package com.celehub.backend.quiz;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.quiz.id = :quizId ORDER BY q.orderIndex ASC, q.id ASC")
    List<Question> findByQuizWithOptions(@Param("quizId") Long quizId);

    long countByQuizId(Long quizId);

    boolean existsByIdAndQuizId(Long id, Long quizId);

    /** Xóa hàng loạt; đáp án bị xóa theo nhờ ON DELETE CASCADE trong DB. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Question q WHERE q.quiz.id = :quizId")
    void deleteByQuiz(@Param("quizId") Long quizId);
}
