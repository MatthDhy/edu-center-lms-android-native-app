package com.celehub.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank(message = "fcmToken không được để trống")
        @Size(max = 500, message = "fcmToken quá dài") String fcmToken) {}
