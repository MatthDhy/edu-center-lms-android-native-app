package com.celehub.backend.course;

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

/** "courses" = một LỚP HỌC cụ thể (Toán 11A) có 1 giảng viên. Chỉ map các cột cần dùng; GĐ2 sẽ bổ sung thêm. */
@Entity
@Table(name = "courses")
public class Course {
    public static final String ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(nullable = false, length = 20)
    private String status;

    protected Course() {}

    public Long getId() { return id; }
    public User getTeacher() { return teacher; }
    public String getCourseName() { return courseName; }
    public Integer getTotalSessions() { return totalSessions; }
    public String getStatus() { return status; }
}
