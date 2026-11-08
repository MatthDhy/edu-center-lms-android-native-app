package com.example.celehub.Model;

import java.util.List;

/** Một trang thông báo (khớp PageResponse của backend). */
public class NotificationPage {
    private List<NotificationItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<NotificationItem> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}