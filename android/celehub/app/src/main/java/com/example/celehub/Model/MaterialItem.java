package com.example.celehub.Model;

/** Tài liệu của lớp (khớp MaterialDto của backend). */
public class MaterialItem {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private String fileUrl;
    private String uploadedByName;
    private String createdAt;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileUrl() { return fileUrl; }
    public String getUploadedByName() { return uploadedByName; }
    public String getCreatedAt() { return createdAt; }
}