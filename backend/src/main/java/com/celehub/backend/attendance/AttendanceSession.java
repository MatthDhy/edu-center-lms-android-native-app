package com.celehub.backend.attendance;

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

import com.celehub.backend.course.Course;
import com.celehub.backend.course.Lesson;
import com.celehub.backend.user.User;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "pin_code", nullable = false, length = 6)
    private String pinCode;

    /** Khóa bí mật để ký mã QR động (không bao giờ gửi cho học viên). */
    @Column(name = "qr_token", nullable = false, unique = true, length = 100)
    private String qrToken;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AttendanceSession() {}

    public AttendanceSession(Course course, Lesson lesson, User createdBy, String pinCode, String qrToken,
                             LocalDateTime startTime, LocalDateTime expiresAt) {
        this.course = course;
        this.lesson = lesson;
        this.createdBy = createdBy;
        this.pinCode = pinCode;
        this.qrToken = qrToken;
        this.startTime = startTime;
        this.expiresAt = expiresAt;
    }

    public boolean isOpen(LocalDateTime now) { return expiresAt.isAfter(now); }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public Lesson getLesson() { return lesson; }
    public User getCreatedBy() { return createdBy; }
    public String getPinCode() { return pinCode; }
    public String getQrToken() { return qrToken; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
