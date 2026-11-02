package com.example.celehub.Model;

/** Một lớp học (khớp CourseDto của backend). */
public class CourseItem {
    private Long id;
    private String courseName;
    private String description;
    private Long teacherId;
    private String teacherName;
    private long tuitionFee;
    private Integer totalSessions;   // có thể null
    private String status;           // ACTIVE | LOCKED
    private long studentCount;
    private long lessonCount;        // số buổi chưa hủy
    private long heldCount;          // số buổi đã kết thúc
    private String createdAt;

    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public String getDescription() { return description; }
    public String getTeacherName() { return teacherName; }
    public long getStudentCount() { return studentCount; }
    public long getHeldCount() { return heldCount; }
    public String getStatus() { return status; }

    /** Tổng số buổi: ưu tiên totalSessions, không có thì lấy số buổi đã xếp lịch. */
    public long getTotalSessions() {
        return totalSessions != null && totalSessions > 0 ? totalSessions : lessonCount;
    }

    /** 0..100 */
    public int getProgressPercent() {
        long total = getTotalSessions();
        if (total <= 0) return 0;
        return (int) Math.min(100, heldCount * 100 / total);
    }

    /** Đã học đủ số buổi thì coi là kết thúc. */
    public boolean isFinished() {
        long total = getTotalSessions();
        return total > 0 && heldCount >= total;
    }
}