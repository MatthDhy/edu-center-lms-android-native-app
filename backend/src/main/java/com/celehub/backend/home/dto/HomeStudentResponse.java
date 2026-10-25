package com.celehub.backend.home.dto;

import java.util.List;

import com.celehub.backend.attendance.AttendanceStatsService;
import com.celehub.backend.notification.dto.NotificationDto;
import com.celehub.backend.user.dto.UserDto;

public record HomeStudentResponse(
        UserDto user,
        long unreadNotifications,
        LessonSummaryDto nextLesson,                 // null nếu không còn buổi nào sắp tới
        AttendanceStatsService.Summary attendance,   // rate = null nếu chưa có buổi nào diễn ra
        List<TaskDto> upcomingTasks,                 // tối đa 5, hạn gần nhất lên đầu
        List<NotificationDto> recentNotifications) {}  // 3 thông báo mới nhất
