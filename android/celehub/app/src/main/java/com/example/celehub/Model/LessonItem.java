package com.example.celehub.Model;

/** Một buổi học trong lịch cá nhân (khớp LessonDto của backend). */
public class LessonItem {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String room;
    private String topic;
    private String startTime;   // "2026-10-15T18:00:00" (giờ Việt Nam)
    private String endTime;
    private String status;      // SCHEDULED | DONE | CANCELED

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getRoom() { return room; }
    public String getTopic() { return topic; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
}