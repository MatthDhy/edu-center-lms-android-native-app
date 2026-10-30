package com.celehub.backend.course;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Một buổi học: nguồn của "ca học kế tiếp", lịch, điểm danh, lương. */
@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 50)
    private String room;

    @Column(length = 200)
    private String topic;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonStatus status = LessonStatus.SCHEDULED;

    protected Lesson() {}

    public Lesson(Course course, String room, String topic, LocalDateTime startTime, LocalDateTime endTime) {
        this.course = course;
        this.room = room;
        this.topic = topic;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LessonStatus getStatus() { return status; }
    public void setStatus(LessonStatus status) { this.status = status; }
}
