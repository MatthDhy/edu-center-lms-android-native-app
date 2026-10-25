package com.celehub.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Phiên đặt lại mật khẩu không hợp lệ") String resetToken,
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu phải từ 8 ký tự trở lên") String newPassword) {}
