package com.example.celehub.Model;

/** Bài tập (ESSAY) hoặc Quiz (QUIZ) sắp đến hạn (khớp TaskDto của backend). */
public class TaskItem {
    private Long id;
    private String type;        // "ESSAY" | "QUIZ"
    private String title;
    private String courseName;
    private String deadline;    // ISO "2026-10-16T23:59:00"
    private long minutesLeft;
    private boolean urgent;     // hạn trong vòng 24 giờ

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getCourseName() { return courseName; }
    public String getDeadline() { return deadline; }
    public long getMinutesLeft() { return minutesLeft; }
    public boolean isUrgent() { return urgent; }
}
