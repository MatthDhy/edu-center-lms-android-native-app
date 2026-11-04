package com.celehub.backend.quiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.celehub.backend.user.User;

/** Mỗi học viên đúng 1 attempt cho mỗi quiz (UNIQUE quiz_id, student_id). */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private BigDecimal score;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Hạn chót do SERVER tính: min(started_at + thời lượng, quizzes.end_time). */
    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "exit_count", nullable = false)
    private int exitCount;

    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted;

    protected QuizAttempt() {}

    public QuizAttempt(Quiz quiz, User student, LocalDateTime startedAt, LocalDateTime endsAt) {
        this.quiz = quiz;
        this.student = student;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public void incrementExit() { this.exitCount++; }

    public void finish(BigDecimal score, LocalDateTime at, boolean auto) {
        this.score = score;
        this.submittedAt = at;
        this.autoSubmitted = auto;
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public User getStudent() { return student; }
    public BigDecimal getScore() { return score; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public int getExitCount() { return exitCount; }
    public boolean isAutoSubmitted() { return autoSubmitted; }
}
