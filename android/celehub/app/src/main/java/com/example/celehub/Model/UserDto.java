package com.example.celehub.Model;

/** Thông tin người dùng backend trả về (tên trường phải trùng với JSON). */
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String role;   // "STUDENT" | "TEACHER" | "ADMIN"
    private String status; // "ACTIVE" | "LOCKED"
    private boolean mustChangePassword;
    private String createdAt;

    public Long getId() { return id; }

    public String getFullName() { return fullName; }

    public String getEmail() { return email; }

    public String getPhone() { return phone; }

    public String getAvatarUrl() { return avatarUrl; }

    public String getRole() { return role; }

    public String getStatus() { return status; }

    public boolean isMustChangePassword() { return mustChangePassword; }

    public String getCreatedAt() { return createdAt; }
}