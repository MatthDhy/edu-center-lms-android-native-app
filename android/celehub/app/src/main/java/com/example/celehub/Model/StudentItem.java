package com.example.celehub.Model;

/** Học viên trong lớp (khớp EnrolledStudentDto của backend). */
public class StudentItem {
    private Long studentId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String joinedAt;

    public Long getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getJoinedAt() { return joinedAt; }
}