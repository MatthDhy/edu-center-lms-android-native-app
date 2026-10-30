package com.celehub.backend.course;

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

/** "courses" = một LỚP HỌC cụ thể (Toán 11A) có 1 giảng viên. */
@Entity
@Table(name = "courses")
public class Course {
    public static final String ACTIVE = "ACTIVE";
    public static final String CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    private String description;

    @Column(name = "tuition_fee", nullable = false)
    private BigDecimal tuitionFee = BigDecimal.ZERO;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(nullable = false, length = 20)
    private String status = ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Course() {}

    public Course(User teacher, String courseName, String description, BigDecimal tuitionFee, Integer totalSessions) {
        this.teacher = teacher;
        this.courseName = courseName;
        this.description = description;
        this.tuitionFee = tuitionFee == null ? BigDecimal.ZERO : tuitionFee;
        this.totalSessions = totalSessions;
    }

    public Long getId() { return id; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public void setTuitionFee(BigDecimal tuitionFee) { this.tuitionFee = tuitionFee; }
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
