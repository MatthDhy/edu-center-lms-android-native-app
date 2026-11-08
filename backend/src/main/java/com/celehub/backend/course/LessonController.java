package com.celehub.backend.course;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.leave.LeaveService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;

/** Lịch cá nhân: GET /api/lessons?from=2026-10-01&to=2026-10-31 (học viên: gộp cả buổi học bù đã được duyệt). */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;
    private final LeaveService leaveService;

    public LessonController(LessonService lessonService, LeaveService leaveService) {
        this.lessonService = lessonService;
        this.leaveService = leaveService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    public ApiResponse<List<LessonDto>> schedule(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<LessonDto> list = new ArrayList<>(lessonService.schedule(me, from, to));
        if (me.role() == Role.STUDENT) {
            Set<Long> have = new HashSet<>();
            list.forEach(l -> have.add(l.id()));
            for (LessonDto m : leaveService.approvedMakeupLessons(me.id(), from, to)) {
                if (have.add(m.id())) list.add(m);
            }
            list.sort(Comparator.comparing(LessonDto::startTime));
        }
        return ApiResponse.success(list);
    }
}
