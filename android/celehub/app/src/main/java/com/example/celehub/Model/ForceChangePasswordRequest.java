package com.example.celehub.Model;

/** Gửi lên POST /api/auth/force-change-password (đổi mật khẩu lần đầu) */
public class ForceChangePasswordRequest {
    private final String newPassword;

    public ForceChangePasswordRequest(String newPassword) {
        this.newPassword = newPassword;
    }
}