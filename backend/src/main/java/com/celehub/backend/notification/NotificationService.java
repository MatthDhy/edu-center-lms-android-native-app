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

    /** type = null: cả 2 loại. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, int page, int size, boolean unreadOnly, NotificationType type) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), NEWEST_FIRST);
        Page<Notification> result;
        if (type == null) {
            result = unreadOnly ? notificationRepository.findByUserIdAndReadFalse(userId, pr)
                    : notificationRepository.findByUserId(userId, pr);
        } else {
            result = unreadOnly ? notificationRepository.findByUserIdAndTypeAndReadFalse(userId, type, pr)
                    : notificationRepository.findByUserIdAndType(userId, type, pr);
        }
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

    /** { general, class, total, count } — count = total (giữ tương thích bản cũ). */
    @Transactional(readOnly = true)
    public Map<String, Long> unreadCounts(Long userId) {
        long general = 0;
        long cls = 0;
        for (Object[] r : notificationRepository.countUnreadByType(userId)) {
            long n = ((Number) r[1]).longValue();
            if (r[0] == NotificationType.GENERAL) general = n; else cls = n;
        }
        long total = general + cls;
        return Map.of("general", general, "class", cls, "total", total, "count", total);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        own(userId, notificationId).markRead();
    }

    @Transactional
    public int markAllRead(Long userId, NotificationType type) {
        return type == null ? notificationRepository.markAllRead(userId)
                : notificationRepository.markAllReadByType(userId, type);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(own(userId, notificationId));
    }

    // ---------- tạo thông báo loại CLASS (từ nghiệp vụ) ----------

    @Transactional
    public void create(Long userId, String title, String content) {
        create(userId, title, content, null, null, null);
    }

    @Transactional
    public void create(Long userId, String title, String content, Long courseId, String refType, Long refId) {
        notificationRepository.save(new Notification(userRepository.getReferenceById(userId), cut(title, 200), content,
                NotificationType.CLASS, courseId, null, refType, refId));
    }

    @Transactional
    public void createForUsers(Collection<Long> userIds, String title, String content) {
        createForUsers(userIds, title, content, null, null, null);
    }

    @Transactional
    public void createForUsers(Collection<Long> userIds, String title, String content,
                               Long courseId, String refType, Long refId) {
        saveBatch(userIds, title, content, NotificationType.CLASS, courseId, null, refType, refId);
    }

    // ---------- admin broadcast: loại GENERAL, có thể kèm 1 ảnh ----------

    @Transactional
    public int broadcast(BroadcastRequest req) {
        return broadcast(req, null);
    }

    @Transactional
    public int broadcast(BroadcastRequest req, String imageUrl) {
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
        Long courseId = req.target() == BroadcastRequest.Target.COURSE ? req.courseId() : null;
        saveBatch(ids, req.title(), req.content(), NotificationType.GENERAL, courseId, imageUrl, null, null);
        return ids.size();
    }

    private void saveBatch(Collection<Long> userIds, String title, String content, NotificationType type,
                           Long courseId, String imageUrl, String refType, Long refId) {
        if (userIds == null || userIds.isEmpty()) return;
        String t = cut(title, 200);
        List<Notification> batch = userIds.stream().distinct()
                .map(id -> new Notification(userRepository.getReferenceById(id), t, content, type,
                        courseId, imageUrl, refType, refId)).toList();
        notificationRepository.saveAll(batch);
    }

    private Notification own(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(x -> x.getUser().getId().equals(userId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
