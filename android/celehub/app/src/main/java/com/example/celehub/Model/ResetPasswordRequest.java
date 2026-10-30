package com.example.celehub.Model;

/** POST /api/auth/forgot-password/reset */
public class ResetPasswordRequest {
    private final String resetToken;
    private final String newPassword;
    public ResetPasswordRequest(String resetToken, String newPassword) {
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }
}