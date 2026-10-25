package com.celehub.backend.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.common.PageResponse;
import com.celehub.backend.notification.dto.NotificationDto;
import com.celehub.backend.user.UserRepository;

@Service
public class NotificationService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, int page, int size, boolean unreadOnly) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), NEWEST_FIRST);
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalse(userId, pr)
                : notificationRepository.findByUserId(userId, pr);
        return PageResponse.from(result, NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> recent(Long userId, int limit) {
        return notificationRepository.findByUserId(userId, PageRequest.of(0, limit, NEWEST_FIRST))
                .getContent().stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(x -> x.getUser().getId().equals(userId))   // không cho đọc/sửa thông báo của người khác
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
        n.markRead();
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    /** Các giai đoạn sau gọi hàm này khi cần báo (duyệt đơn nghỉ, có điểm, bài tập mới...). */
    @Transactional
    public void create(Long userId, String title, String content) {
        notificationRepository.save(new Notification(userRepository.getReferenceById(userId), title, content));
    }
}
