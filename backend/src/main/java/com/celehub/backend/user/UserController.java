package com.celehub.backend.user;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.dto.ProfileResponse;
import com.celehub.backend.user.dto.UpdateProfileRequest;
import com.celehub.backend.user.dto.UserDto;

import jakarta.validation.Valid;

/** Tab "Cá nhân" — dùng chung cho STUDENT, TEACHER (và ADMIN nếu cần). */
@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService userService;
    private final ProfileService profileService;

    public UserController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<UserDto> me(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(userService.getById(me.id()));
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(profileService.getProfile(me.id()));
    }

    @PatchMapping
    public ApiResponse<UserDto> update(@AuthenticationPrincipal AuthUser me,
                                       @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.success("Đã cập nhật hồ sơ", userService.updateProfile(me.id(), req));
    }

    /** multipart/form-data, tên part = "file" (JPG/PNG/WEBP, ≤ 5MB). */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserDto> uploadAvatar(@AuthenticationPrincipal AuthUser me,
                                             @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Đã cập nhật ảnh đại diện", userService.updateAvatar(me.id(), file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserDto> deleteAvatar(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success("Đã xóa ảnh đại diện", userService.deleteAvatar(me.id()));
    }
}
