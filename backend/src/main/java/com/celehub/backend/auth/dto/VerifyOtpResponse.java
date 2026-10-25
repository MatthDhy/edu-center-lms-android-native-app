package com.celehub.backend.auth.dto;

/** Sau khi OTP đúng: app giữ resetToken và gửi kèm mật khẩu mới ở bước cuối. */
public record VerifyOtpResponse(String resetToken) {}
