package com.example.celehub.Model;

/**
 * Một ca học / ca dạy trên Home (khớp LessonSummaryDto của backend).
 * Học viên: teacherName có giá trị, studentCount = null.
 * Giảng viên: studentCount là sĩ số lớp.
 * Thời gian là chuỗi ISO "2026-10-15T18:00:00".
 */
public class LessonSummary {
    private Long lessonId;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private String room;
    private String topic;
    private String startTime;
    private String endTime;
    private long minutesUntilStart;
    private boolean ongoing;
    private Integer studentCount;

    public Long getLessonId() { return lessonId; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTeacherName() { return teacherName; }
    public String getRoom() { return room; }
    public String getTopic() { return topic; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public long getMinutesUntilStart() { return minutesUntilStart; }
    public boolean isOngoing() { return ongoing; }
    public Integer getStudentCount() { return studentCount; }
}
