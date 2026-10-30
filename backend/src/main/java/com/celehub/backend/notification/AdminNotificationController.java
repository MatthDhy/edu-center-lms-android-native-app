package com.celehub.backend.notification;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.notification.dto.BroadcastRequest;

import jakarta.validation.Valid;

/** /api/admin/** chỉ ROLE_ADMIN. */
@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/broadcast")
    public ApiResponse<Map<String, Integer>> broadcast(@Valid @RequestBody BroadcastRequest req) {
        int n = notificationService.broadcast(req);
        return ApiResponse.success("Đã gửi thông báo", Map.of("recipients", n));
    }
}
