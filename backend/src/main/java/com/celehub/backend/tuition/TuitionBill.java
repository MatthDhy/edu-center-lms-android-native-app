package com.celehub.backend.tuition;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.celehub.backend.course.Course;
import com.celehub.backend.user.User;

@Entity
@Table(name = "tuition_bills")
public class TuitionBill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Trong DB chỉ lưu UNPAID/PAID; OVERDUE được tính theo due_date (xem TuitionService.effective). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TuitionStatus status = TuitionStatus.UNPAID;

    @Column(name = "transfer_content", nullable = false, unique = true, length = 100)
    private String transferContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected TuitionBill() {}

    public TuitionBill(User student, Course course, String title, BigDecimal amount, LocalDate dueDate, String transferContent) {
        this.student = student;
        this.course = course;
        this.title = title;
        this.amount = amount;
        this.dueDate = dueDate;
        this.transferContent = transferContent;
    }

    public void markPaid() { this.status = TuitionStatus.PAID; }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public TuitionStatus getStatus() { return status; }
    public String getTransferContent() { return transferContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
