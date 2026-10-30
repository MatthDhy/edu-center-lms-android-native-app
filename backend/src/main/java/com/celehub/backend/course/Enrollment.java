package com.celehub.backend.course;

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
@Table(name = "enrollments")
public class Enrollment {
    public static final String ACTIVE = "ACTIVE";
    public static final String DROPPED = "DROPPED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 20)
    private String status = ACTIVE;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected Enrollment() {}

    public Enrollment(Course course, User student) {
        this.course = course;
        this.student = student;
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public User getStudent() { return student; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
