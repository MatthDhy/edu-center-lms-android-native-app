package com.celehub.backend.notification;

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
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationType type = NotificationType.CLASS;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "ref_type", length = 20)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Notification() {}

    public Notification(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    public Notification(User user, String title, String content, NotificationType type,
                        Long courseId, String imageUrl, String refType, Long refId) {
        this(user, title, content);
        this.type = type == null ? NotificationType.CLASS : type;
        this.courseId = courseId;
        this.imageUrl = imageUrl;
        this.refType = refType;
        this.refId = refId;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public void markRead() { this.read = true; }
    public NotificationType getType() { return type; }
    public Long getCourseId() { return courseId; }
    public String getImageUrl() { return imageUrl; }
    public String getRefType() { return refType; }
    public Long getRefId() { return refId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
