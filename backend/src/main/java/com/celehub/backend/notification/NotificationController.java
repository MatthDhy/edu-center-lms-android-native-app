package com.celehub.backend.notification;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.common.PageResponse;
import com.celehub.backend.notification.dto.NotificationDto;
import com.celehub.backend.security.AuthUser;

/** Chuông thông báo — dùng chung STUDENT, TEACHER. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationDto>> list(@AuthenticationPrincipal AuthUser me,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ApiResponse.success(notificationService.list(me.id(), page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(Map.of("count", notificationService.unreadCount(me.id())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        notificationService.markRead(me.id(), id);
        return ApiResponse.success("Đã đánh dấu đã đọc", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success("Đã đánh dấu tất cả là đã đọc", Map.of("updated", notificationService.markAllRead(me.id())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        notificationService.delete(me.id(), id);
        return ApiResponse.success("Đã xóa thông báo", null);
    }
}
