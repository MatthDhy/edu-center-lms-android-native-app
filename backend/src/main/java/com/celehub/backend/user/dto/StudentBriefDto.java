package com.celehub.backend.user.dto;

import com.celehub.backend.user.User;

public record StudentBriefDto(Long id, String fullName, String email, String phone, String avatarUrl) {
    public static StudentBriefDto from(User u) {
        return new StudentBriefDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAvatarUrl());
    }
}
