package com.example.celehub.Model;

/** Một thông báo (khớp NotificationDto của backend). */
public class NotificationItem {
    private Long id;
    private String title;
    private String content;
    private boolean read;
    private String createdAt;   // ISO "2026-10-15T17:40:00"

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public String getCreatedAt() { return createdAt; }
}