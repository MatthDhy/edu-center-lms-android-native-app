package com.celehub.backend.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.notification.dto.RegisterDeviceRequest;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

/**
 * Đăng ký / hủy FCM token của thiết bị. Hiện chỉ LƯU token; việc bắn push thật
 * (firebase-admin) làm ở bước sau — mọi thông báo vẫn nằm trong bảng notifications.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceTokenRepository repository;

    public DeviceController(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    /** Gọi sau khi đăng nhập. Token đã tồn tại (máy đổi tài khoản) thì chuyển sang người dùng hiện tại. */
    @PostMapping
    @Transactional
    public ApiResponse<Void> register(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody RegisterDeviceRequest req) {
        String token = req.fcmToken().trim();
        DeviceToken existing = repository.findByFcmToken(token).orElse(null);
        if (existing == null) {
            repository.save(new DeviceToken(me.id(), token));
        } else if (!existing.getUserId().equals(me.id())) {
            existing.setUserId(me.id());
        }
        return ApiResponse.success("Đã đăng ký thiết bị", null);
    }

    /** Gọi khi đăng xuất: DELETE /api/devices?token=... */
    @DeleteMapping
    @Transactional
    public ApiResponse<Void> unregister(@AuthenticationPrincipal AuthUser me, @RequestParam String token) {
        repository.deleteByTokenAndUser(token.trim(), me.id());
        return ApiResponse.success("Đã hủy đăng ký thiết bị", null);
    }
}
