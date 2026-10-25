package com.celehub.backend.home;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.home.dto.HomeStudentResponse;
import com.celehub.backend.home.dto.HomeTeacherResponse;
import com.celehub.backend.security.AuthUser;

/** Tab "Trang chủ": mỗi role một endpoint. Sai role -> 403. */
@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<HomeStudentResponse> student(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(homeService.student(me.id()));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<HomeTeacherResponse> teacher(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(homeService.teacher(me.id()));
    }
}
