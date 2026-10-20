package com.celehub.backend.security;

import com.celehub.backend.user.Role;

/** Người đang gọi API — lấy bằng @AuthenticationPrincipal AuthUser me */
public record AuthUser(Long id, String email, Role role) {}
