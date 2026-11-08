package com.example.celehub.Model;

/** Học viên chưa nộp bài (phần notSubmitted của GET /api/assignments/{id}/submissions). */
public class StudentBrief {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
}