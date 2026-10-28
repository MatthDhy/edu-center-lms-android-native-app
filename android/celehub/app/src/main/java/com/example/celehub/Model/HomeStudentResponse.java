package com.example.celehub.Model;

import java.util.List;

/**
 * GET /api/home/student. Gson bỏ qua các field backend trả thêm mà class này không khai báo
 * (attendance, recentNotifications) nên có thể bổ sung sau.
 */
public class HomeStudentResponse {
    private UserDto user;
    private long unreadNotifications;
    private LessonSummary nextLesson;        // null nếu không còn buổi nào
    private List<TaskItem> upcomingTasks;    // tối đa 5, hạn gần nhất đứng đầu

    private List<NotificationItem> recentNotifications;   // tối đa 3, mới nhất đứng đầu

    public List<NotificationItem> getRecentNotifications() { return recentNotifications; }
    public UserDto getUser() { return user; }
    public long getUnreadNotifications() { return unreadNotifications; }
    public LessonSummary getNextLesson() { return nextLesson; }
    public List<TaskItem> getUpcomingTasks() { return upcomingTasks; }
}
