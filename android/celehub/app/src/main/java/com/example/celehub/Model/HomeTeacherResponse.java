package com.example.celehub.Model;

import java.util.List;

/** GET /api/home/teacher. Các field khác (todayLessons, pendingGradingCount...) làm sau. */
public class HomeTeacherResponse {
    private UserDto user;
    private long unreadNotifications;
    private LessonSummary nextLesson;        // null nếu không còn buổi nào
    // LƯU Ý: backend hiện CHƯA trả field này cho giảng viên. Khi nào backend thêm thì app tự hiển thị,
    // còn chưa có thì giá trị là null và Home hiện dòng "Không có bài tập sắp đến hạn".
    private List<TaskItem> upcomingTasks;

    private List<NotificationItem> recentNotifications;   // tối đa 3, mới nhất đứng đầu

    public List<NotificationItem> getRecentNotifications() { return recentNotifications; }

    public UserDto getUser() { return user; }
    public long getUnreadNotifications() { return unreadNotifications; }
    public LessonSummary getNextLesson() { return nextLesson; }
    public List<TaskItem> getUpcomingTasks() { return upcomingTasks; }
}
