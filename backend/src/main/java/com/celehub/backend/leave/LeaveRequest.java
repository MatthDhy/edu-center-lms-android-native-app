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

/** Chỉ map các cột Home cần; GĐ4 sẽ bổ sung makeup_lesson_id, reason, reject_reason... */
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LeaveRequest() {}

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Lesson getLesson() { return lesson; }
    public LeaveStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
