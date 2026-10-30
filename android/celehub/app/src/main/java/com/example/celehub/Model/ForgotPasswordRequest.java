package com.example.celehub.Model;

/** POST /api/auth/forgot-password/request */
public class ForgotPasswordRequest {
    private final String email;
    public ForgotPasswordRequest(String email) { this.email = email; }
}