package com.celehub.backend.course.dto;

import java.time.LocalDateTime;

import com.celehub.backend.course.Enrollment;
import com.celehub.backend.user.User;

public record EnrolledStudentDto(Long studentId, String fullName, String email, String phone,
                                 String avatarUrl, LocalDateTime joinedAt) {
    public static EnrolledStudentDto from(Enrollment e) {
        User u = e.getStudent();
        return new EnrolledStudentDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getAvatarUrl(), e.getJoinedAt());
    }
}
