package com.example.celehub.Model;

/** POST /api/auth/forgot-password/verify */
public class VerifyOtpRequest {
    private final String email;
    private final String otp;
    public VerifyOtpRequest(String email, String otp) { this.email = email; this.otp = otp; }
}