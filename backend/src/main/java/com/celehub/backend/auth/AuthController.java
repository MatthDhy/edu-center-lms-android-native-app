package com.celehub.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.auth.dto.AuthResponse;
import com.celehub.backend.auth.dto.ChangePasswordRequest;
import com.celehub.backend.auth.dto.ForceChangePasswordRequest;
import com.celehub.backend.auth.dto.ForgotPasswordRequest;
import com.celehub.backend.auth.dto.LoginRequest;
import com.celehub.backend.auth.dto.ResetPasswordRequest;
import com.celehub.backend.auth.dto.VerifyOtpRequest;
import com.celehub.backend.auth.dto.VerifyOtpResponse;
import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
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

    /** JWT stateless nên đăng xuất = app xóa token. Endpoint này để app gọi cho đối xứng (sau này hủy FCM token). */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success("Đã đăng xuất", null);
    }

    // ---- Quên mật khẩu (public, không cần token) ----

    @PostMapping("/forgot-password/request")
    public ApiResponse<Void> forgotRequest(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestOtp(req.email());
        return ApiResponse.success("Nếu email có trong hệ thống, mã OTP đã được gửi", null);
    }

    @PostMapping("/forgot-password/verify")
    public ApiResponse<VerifyOtpResponse> forgotVerify(@Valid @RequestBody VerifyOtpRequest req) {
        String token = passwordResetService.verifyOtp(req.email(), req.otp());
        return ApiResponse.success("Xác thực OTP thành công", new VerifyOtpResponse(token));
    }

    @PostMapping("/forgot-password/reset")
    public ApiResponse<Void> forgotReset(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.resetToken(), req.newPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công", null);
    }
}
