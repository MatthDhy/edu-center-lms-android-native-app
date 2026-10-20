package com.celehub.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.auth.dto.AuthResponse;
import com.celehub.backend.auth.dto.ChangePasswordRequest;
import com.celehub.backend.auth.dto.ForceChangePasswordRequest;
import com.celehub.backend.auth.dto.LoginRequest;
import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success("Đăng nhập thành công", authService.login(req));
    }

    @PostMapping("/force-change-password")
    public ApiResponse<Void> forceChange(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestBody ForceChangePasswordRequest req) {
        authService.forceChangePassword(me.id(), req.newPassword());
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> change(@AuthenticationPrincipal AuthUser me,
                                    @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(me.id(), req.oldPassword(), req.newPassword());
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }

    // TODO giai đoạn sau: /forgot-password/request (gửi OTP), /forgot-password/confirm
}
