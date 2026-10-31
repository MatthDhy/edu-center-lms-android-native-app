package com.celehub.backend.course;

import java.time.LocalDateTime;

import com.celehub.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "materials")
public class Material {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Material() {}

    public Material(Course course, User uploadedBy, String title, String description, String fileUrl) {
        this.course = course; this.uploadedBy = uploadedBy;
        this.title = title; this.description = description; this.fileUrl = fileUrl;
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public User getUploadedBy() { return uploadedBy; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileUrl() { return fileUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}