package com.celehub.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    boolean existsByIdAndQuestionId(Long id, Long questionId);
}
