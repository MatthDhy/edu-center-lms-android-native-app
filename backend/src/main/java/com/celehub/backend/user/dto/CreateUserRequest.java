package com.celehub.backend.user.dto;

import com.celehub.backend.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank(message = "Họ tên không được để trống") String fullName,
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
        String phone,
        @NotNull(message = "Vai trò không được để trống") Role role) {}
