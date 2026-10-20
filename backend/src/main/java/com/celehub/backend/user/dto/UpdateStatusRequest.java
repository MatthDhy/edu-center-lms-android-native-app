package com.celehub.backend.user.dto;

import com.celehub.backend.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Trạng thái không được để trống") UserStatus status) {}
