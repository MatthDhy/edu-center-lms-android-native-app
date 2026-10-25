package com.celehub.backend.home.dto;

import java.util.List;

import com.celehub.backend.notification.dto.NotificationDto;
import com.celehub.backend.user.dto.UserDto;

public record HomeTeacherResponse(
        UserDto user,
        long unreadNotifications,
        LessonSummaryDto nextLesson,                 // buổi đang diễn ra / sắp tới gần nhất (có thể không phải hôm nay)
        List<LessonSummaryDto> todayLessons,         // toàn bộ ca dạy trong hôm nay
        long pendingGradingCount,                    // bài nộp chưa chấm
        long pendingLeaveCount,                      // đơn xin nghỉ chờ duyệt
        long lessonsThisMonth,                       // số buổi đã dạy trong tháng
        List<SubmissionSummaryDto> recentSubmissions,// 5 bài nộp chưa chấm mới nhất
        List<NotificationDto> recentNotifications) {}
