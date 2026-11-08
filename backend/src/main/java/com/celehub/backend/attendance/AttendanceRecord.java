package com.celehub.backend.attendance;

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

import com.celehub.backend.user.User;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceMethod method = AttendanceMethod.QR;

    protected AttendanceRecord() {}

    public AttendanceRecord(AttendanceSession session, User student, AttendanceStatus status, AttendanceMethod method) {
        this.session = session;
        this.student = student;
        this.status = status;
        this.method = method;
    }

    public Long getId() { return id; }
    public AttendanceSession getSession() { return session; }
    public User getStudent() { return student; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public AttendanceMethod getMethod() { return method; }
    public void setMethod(AttendanceMethod method) { this.method = method; }
}
