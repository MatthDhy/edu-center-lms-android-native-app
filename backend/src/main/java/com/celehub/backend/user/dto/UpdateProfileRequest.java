package com.celehub.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/me — trường nào null thì giữ nguyên.
 * Muốn XÓA số điện thoại: gửi phone = "" (chuỗi rỗng). Email KHÔNG được đổi ở đây.
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
        @Pattern(regexp = "^$|^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ") String phone) {}
