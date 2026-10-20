package com.celehub.backend.auth.dto;

import com.celehub.backend.user.Role;
import com.celehub.backend.user.dto.UserDto;

/** JSON: { token, role, mustChangePassword, user } — camelCase, thống nhất toàn dự án */
public record AuthResponse(String token, Role role, boolean mustChangePassword, UserDto user) {}
