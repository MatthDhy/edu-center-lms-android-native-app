package com.celehub.backend.notification;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.common.PageResponse;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseRepository;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.notification.dto.BroadcastRequest;
import com.celehub.backend.notification.dto.NotificationDto;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

@Service
public class NotificationService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
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
        own(userId, notificationId).markRead();
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(own(userId, notificationId));
    }

    /** Các nghiệp vụ gọi hàm này khi cần báo 1 người. */
    @Transactional
    public void create(Long userId, String title, String content) {
        notificationRepository.save(new Notification(userRepository.getReferenceById(userId), cut(title, 200), content));
    }

    /** Báo nhiều người cùng lúc (giao bài, nhắc nộp, đổi lịch...). Danh sách rỗng thì bỏ qua. */
    @Transactional
    public void createForUsers(Collection<Long> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) return;
        String t = cut(title, 200);
        List<Notification> batch = userIds.stream().distinct()
                .map(id -> new Notification(userRepository.getReferenceById(id), t, content)).toList();
        notificationRepository.saveAll(batch);
    }

    /** Admin gửi thông báo chung. Trả về số người nhận. */
    @Transactional
    public int broadcast(BroadcastRequest req) {
        Set<Long> ids = new LinkedHashSet<>();
        switch (req.target()) {
            case ALL -> ids.addAll(userRepository.findIdsByRolesAndStatus(
                    List.of(Role.STUDENT, Role.TEACHER), UserStatus.ACTIVE));
            case STUDENTS -> ids.addAll(userRepository.findIdsByRolesAndStatus(
                    List.of(Role.STUDENT), UserStatus.ACTIVE));
            case TEACHERS -> ids.addAll(userRepository.findIdsByRolesAndStatus(
                    List.of(Role.TEACHER), UserStatus.ACTIVE));
            case COURSE -> {
                if (req.courseId() == null) throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu courseId");
                Course c = courseRepository.findWithTeacher(req.courseId())
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));
                ids.addAll(enrollmentRepository.findActiveStudentIds(c.getId()));
                ids.add(c.getTeacher().getId());
            }
        }
        createForUsers(ids, req.title(), req.content());
        return ids.size();
    }

    private Notification own(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(x -> x.getUser().getId().equals(userId))   // không cho đọc/sửa/xóa thông báo của người khác
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
