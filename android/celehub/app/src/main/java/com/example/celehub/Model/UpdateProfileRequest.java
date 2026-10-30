package com.example.celehub.Model;

/** PATCH /api/users/me. phone = "" nghĩa là xóa số điện thoại. */
public class UpdateProfileRequest {
    private final String fullName;
    private final String phone;
    public UpdateProfileRequest(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }
}