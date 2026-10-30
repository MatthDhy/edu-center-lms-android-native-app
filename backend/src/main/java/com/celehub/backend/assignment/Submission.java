package com.celehub.backend.assignment;

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

@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "is_late", nullable = false)
    private boolean late;

    /** null = chưa chấm */
    private BigDecimal score;

    private String feedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    protected Submission() {}

    public Submission(Assignment assignment, User student, String fileUrl, LocalDateTime submittedAt, boolean late) {
        this.assignment = assignment;
        this.student = student;
        this.fileUrl = fileUrl;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    /** Nộp lại bài (chỉ khi chưa được chấm). */
    public void resubmit(String fileUrl, LocalDateTime submittedAt, boolean late) {
        this.fileUrl = fileUrl;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    public void grade(BigDecimal score, String feedback, LocalDateTime gradedAt) {
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = gradedAt;
    }

    public Long getId() { return id; }
    public Assignment getAssignment() { return assignment; }
    public User getStudent() { return student; }
    public String getFileUrl() { return fileUrl; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean isLate() { return late; }
    public BigDecimal getScore() { return score; }
    public String getFeedback() { return feedback; }
    public LocalDateTime getGradedAt() { return gradedAt; }
}
