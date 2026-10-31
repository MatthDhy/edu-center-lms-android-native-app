package com.celehub.backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.dto.MaterialDto;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.storage.StorageService;
import com.celehub.backend.storage.UploadValidator;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.UserRepository;

@Service
public class MaterialService {
    private static final long MAX_FILE = 20L * 1024 * 1024;

    private final MaterialRepository materialRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    public MaterialService(MaterialRepository materialRepository, EnrollmentRepository enrollmentRepository,
                           UserRepository userRepository, CourseService courseService,
                           StorageService storageService, NotificationService notificationService) {
        this.materialRepository = materialRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.storageService = storageService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> list(AuthUser me, Long courseId) {
        courseService.requireAccess(me, courseId);
        return materialRepository.findByCourse(courseId).stream().map(MaterialDto::from).toList();
    }

    @Transactional
    public MaterialDto upload(AuthUser me, Long courseId, String title, String description, MultipartFile file) {
        Course c = requireManage(me, courseId);
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề tài liệu không được để trống");
        if (t.length() > 200) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề tối đa 200 ký tự");
        UploadValidator.require(file, UploadValidator.DOCUMENTS, MAX_FILE, "tài liệu");

        String url = storageService.upload(file, "materials");
        String desc = description == null || description.isBlank() ? null : description.trim();
        Material m = materialRepository.save(
                new Material(c, userRepository.getReferenceById(me.id()), t, desc, url));

        notificationService.createForUsers(enrollmentRepository.findActiveStudentIds(courseId),
                "Tài liệu mới: " + t, c.getCourseName());
        return MaterialDto.from(m);
    }

    @Transactional
    public void delete(AuthUser me, Long materialId) {
        Material m = materialRepository.findDetail(materialId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu"));
        requireManage(me, m.getCourse().getId());
        String url = m.getFileUrl();
        materialRepository.delete(m);
        if (url != null) storageService.delete(url);
    }

    /** ADMIN: mọi lớp. TEACHER: chỉ lớp mình phụ trách. */
    private Course requireManage(AuthUser me, Long courseId) {
        if (me.role() == Role.ADMIN) return courseService.load(courseId);
        return courseService.requireTeacherOf(me, courseId);
    }
}