package com.example.celehub.Model;

/** Phần "data" của API đăng nhập: { token, role, mustChangePassword, user } */
public class AuthResponse {
    private String token;
    private String role;
    private boolean mustChangePassword;
    private UserDto user;

    public String getToken() { return token; }

    public String getRole() { return role; }

    public boolean isMustChangePassword() { return mustChangePassword; }

    public UserDto getUser() { return user; }
}