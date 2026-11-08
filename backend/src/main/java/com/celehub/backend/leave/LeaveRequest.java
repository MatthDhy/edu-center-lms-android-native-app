package com.celehub.backend.leave;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.celehub.backend.course.Lesson;
import com.celehub.backend.user.User;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_lesson_id")
    private Lesson makeupLesson;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected LeaveRequest() {}

    public LeaveRequest(User student, Lesson lesson, Lesson makeupLesson, String reason) {
        this.student = student;
        this.lesson = lesson;
        this.makeupLesson = makeupLesson;
        this.reason = reason;
    }

    public void approve(User reviewer) { this.status = LeaveStatus.APPROVED; this.reviewedBy = reviewer; }

    public void reject(User reviewer, String reason) {
        this.status = LeaveStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.rejectReason = reason;
    }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Lesson getLesson() { return lesson; }
    public Lesson getMakeupLesson() { return makeupLesson; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
