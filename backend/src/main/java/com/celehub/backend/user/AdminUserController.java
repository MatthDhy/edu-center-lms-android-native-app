package com.celehub.backend.user;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.user.dto.CreateUserRequest;
import com.celehub.backend.user.dto.UpdateStatusRequest;
import com.celehub.backend.user.dto.UserDto;

import jakarta.validation.Valid;

/** /api/admin/** chỉ ROLE_ADMIN (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserDto>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search) {
        return ApiResponse.success(userService.searchUsers(role, status, search));
    }

    @PostMapping
    public ApiResponse<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.success("Đã tạo tài khoản và gửi mật khẩu tạm qua email", userService.createUser(req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.success("Đã cập nhật trạng thái tài khoản", userService.updateStatus(id, req.status()));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id) {
        String tempPass = userService.resetPassword(id);
        return ApiResponse.success("Đã đặt lại mật khẩu tạm thành công", Map.of("tempPassword", tempPass));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(userService.getStats());
    }
}

