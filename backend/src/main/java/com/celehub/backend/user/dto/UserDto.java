package com.celehub.backend.user.dto;

import java.time.LocalDateTime;

import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserStatus;

public record UserDto(
        Long id,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        Role role,
        UserStatus status,
        boolean mustChangePassword,
        LocalDateTime createdAt) {

    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getAvatarUrl(),
                u.getRole(),
                u.getStatus(),
                u.isMustChangePassword(),
                u.getCreatedAt());
    }
}

