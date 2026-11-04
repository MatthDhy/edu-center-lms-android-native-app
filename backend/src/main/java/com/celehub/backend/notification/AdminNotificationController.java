package com.celehub.backend.notification;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.common.AppException;
import com.celehub.backend.notification.dto.BroadcastRequest;
import com.celehub.backend.storage.StorageService;
import com.celehub.backend.storage.UploadValidator;

import jakarta.validation.Valid;

/** /api/admin/** chỉ ROLE_ADMIN. Gửi được JSON (không ảnh) hoặc multipart (kèm 1 ảnh ≤ 5MB). */
@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private static final long MAX_IMAGE = 5L * 1024 * 1024;

    private final NotificationService notificationService;
    private final StorageService storageService;

    public AdminNotificationController(NotificationService notificationService, StorageService storageService) {
        this.notificationService = notificationService;
        this.storageService = storageService;
    }

    @PostMapping(value = "/broadcast", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Integer>> broadcast(@Valid @RequestBody BroadcastRequest req) {
        int n = notificationService.broadcast(req);
        return ApiResponse.success("Đã gửi thông báo", Map.of("recipients", n));
    }

    /** multipart: title, content?, target, courseId?, image? */
    @PostMapping(value = "/broadcast", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Integer>> broadcastWithImage(
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam BroadcastRequest.Target target,
            @RequestParam(required = false) Long courseId,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề không được để trống");
        if (t.length() > 200) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề tối đa 200 ký tự");
        if (content != null && content.length() > 2000) throw new AppException(HttpStatus.BAD_REQUEST, "Nội dung tối đa 2000 ký tự");

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            UploadValidator.require(image, UploadValidator.IMAGES, MAX_IMAGE, "ảnh");
            imageUrl = storageService.upload(image, "notifications");
        }
        try {
            int n = notificationService.broadcast(new BroadcastRequest(t, content, target, courseId), imageUrl);
            return ApiResponse.success("Đã gửi thông báo", Map.of("recipients", n));
        } catch (RuntimeException e) {
            if (imageUrl != null) storageService.delete(imageUrl);
            throw e;
        }
    }
}
