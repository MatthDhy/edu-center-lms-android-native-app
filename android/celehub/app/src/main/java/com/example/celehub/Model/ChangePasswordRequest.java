package com.example.celehub.Model;

/** Gửi lên POST /api/auth/change-password (đổi mật khẩu trong màn Cá nhân) */
public class ChangePasswordRequest {
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}