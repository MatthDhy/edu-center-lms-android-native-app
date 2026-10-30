package com.celehub.backend.course;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.dto.CreateLessonRequest;
import com.celehub.backend.course.dto.GenerateLessonsRequest;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.course.dto.UpdateLessonRequest;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;

@Service
public class LessonService {
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;

    public LessonService(LessonRepository lessonRepository, EnrollmentRepository enrollmentRepository,
                         CourseService courseService, NotificationService notificationService) {
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
    }

    // ===================== đọc =====================

    @Transactional(readOnly = true)
    public List<LessonDto> listByCourse(AuthUser me, Long courseId) {
        courseService.requireAccess(me, courseId);
        return lessonRepository.findByCourse(courseId).stream().map(LessonDto::from).toList();
    }

    /** Lịch cá nhân (STUDENT / TEACHER) trong khoảng ngày [from, to], tối đa 62 ngày. */
    @Transactional(readOnly = true)
    public List<LessonDto> schedule(AuthUser me, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new AppException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        if (ChronoUnit.DAYS.between(from, to) > 62) throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ xem tối đa 62 ngày một lần");
        LocalDateTime a = from.atStartOfDay();
        LocalDateTime b = to.plusDays(1).atStartOfDay();
        List<Lesson> list = me.role() == Role.TEACHER
                ? lessonRepository.findScheduleForTeacher(me.id(), a, b)
                : lessonRepository.findScheduleForStudent(me.id(), a, b);
        return list.stream().map(LessonDto::from).toList();
    }


    /** Admin: toàn bộ buổi học trong khoảng ngày [from, to], tối đa 62 ngày. */
    @Transactional(readOnly = true)
    public List<LessonDto> adminSchedule(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new AppException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        if (ChronoUnit.DAYS.between(from, to) > 62) throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ xem tối đa 62 ngày một lần");
        return lessonRepository.findAllBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().map(LessonDto::from).toList();
    }

    // ===================== ghi (ADMIN) =====================

    @Transactional
    public LessonDto create(Long courseId, CreateLessonRequest req) {
        Course c = courseService.load(courseId);
        requireOpen(c);
        validateRange(req.startTime(), req.endTime());
        String room = blankToNull(req.room());
        checkConflict(c, room, req.startTime(), req.endTime(), 0L);
        Lesson l = lessonRepository.save(new Lesson(c, room, blankToNull(req.topic()), req.startTime(), req.endTime()));
        return LessonDto.from(l);
    }

    /** Sinh lịch lặp theo thứ trong tuần. Chỉ cần 1 buổi trùng giờ GV/phòng là hủy cả đợt (409). */
    @Transactional
    public List<LessonDto> generate(Long courseId, GenerateLessonsRequest req) {
        Course c = courseService.load(courseId);
        requireOpen(c);
        if (!req.endTime().isAfter(req.startTime())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        String room = blankToNull(req.room());
        String topic = blankToNull(req.topic());
        Set<Integer> days = new HashSet<>(req.daysOfWeek());

        List<Lesson> created = new ArrayList<>();
        LocalDate d = req.startDate();
        LocalDate limit = d.plusDays(366);
        while (created.size() < req.sessions() && !d.isAfter(limit)) {
            if (days.contains(d.getDayOfWeek().getValue())) {
                LocalDateTime s = d.atTime(req.startTime());
                LocalDateTime e = d.atTime(req.endTime());
                checkConflict(c, room, s, e, 0L);
                created.add(new Lesson(c, room, topic, s, e));
            }
            d = d.plusDays(1);
        }
        if (created.size() < req.sessions()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không đủ ngày phù hợp trong vòng 1 năm để xếp " + req.sessions() + " buổi");
        }
        return lessonRepository.saveAll(created).stream().map(LessonDto::from).toList();
    }

    @Transactional
    public LessonDto update(Long lessonId, UpdateLessonRequest req) {
        Lesson l = loadLesson(lessonId);
        LocalDateTime start = req.startTime() != null ? req.startTime() : l.getStartTime();
        LocalDateTime end = req.endTime() != null ? req.endTime() : l.getEndTime();
        String room = req.room() != null ? blankToNull(req.room()) : l.getRoom();
        LessonStatus status = req.status() != null ? req.status() : l.getStatus();
        validateRange(start, end);

        boolean timeChanged = !start.equals(l.getStartTime()) || !end.equals(l.getEndTime());
        boolean roomChanged = room == null ? l.getRoom() != null : !room.equalsIgnoreCase(l.getRoom() == null ? "" : l.getRoom());
        if (status != LessonStatus.CANCELED) {
            checkConflict(l.getCourse(), room, start, end, l.getId());
        }
        boolean newlyCanceled = status == LessonStatus.CANCELED && l.getStatus() != LessonStatus.CANCELED;

        l.setStartTime(start);
        l.setEndTime(end);
        l.setRoom(room);
        if (req.topic() != null) l.setTopic(blankToNull(req.topic()));
        l.setStatus(status);

        if (newlyCanceled) {
            notifyStudents(l, "Buổi học bị hủy", l.getCourse().getCourseName() + " - " + start.format(FULL));
        } else if (status != LessonStatus.CANCELED && (timeChanged || roomChanged)) {
            notifyStudents(l, "Lịch học thay đổi", l.getCourse().getCourseName() + " - buổi mới: "
                    + start.format(FULL) + (room == null ? "" : " tại " + room));
        }
        return LessonDto.from(l);
    }

    /** Hủy buổi (không xóa cứng vì còn liên kết đơn nghỉ / điểm danh). */
    @Transactional
    public void cancel(Long lessonId) {
        Lesson l = loadLesson(lessonId);
        if (l.getStatus() == LessonStatus.CANCELED) return;
        l.setStatus(LessonStatus.CANCELED);
        notifyStudents(l, "Buổi học bị hủy", l.getCourse().getCourseName() + " - " + l.getStartTime().format(FULL));
    }

    // ===================== helpers =====================

    private Lesson loadLesson(Long id) {
        return lessonRepository.findDetail(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi học"));
    }

    private void requireOpen(Course c) {
        if (Course.CLOSED.equals(c.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp đã đóng, không thể thêm buổi học");
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) throw new AppException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
        if (!start.toLocalDate().equals(end.toLocalDate())) throw new AppException(HttpStatus.BAD_REQUEST, "Buổi học phải diễn ra trong cùng một ngày");
        if (ChronoUnit.HOURS.between(start, end) > 12) throw new AppException(HttpStatus.BAD_REQUEST, "Buổi học dài tối đa 12 giờ");
    }

    private void checkConflict(Course c, String room, LocalDateTime start, LocalDateTime end, long excludeId) {
        List<Lesson> t = lessonRepository.findTeacherOverlaps(
                c.getTeacher().getId(), start, end, excludeId, LessonStatus.CANCELED);
        if (!t.isEmpty()) {
            throw new AppException(HttpStatus.CONFLICT, "Giảng viên bị trùng lịch: " + describe(t.get(0)));
        }
        if (room != null) {
            List<Lesson> r = lessonRepository.findRoomOverlaps(room, start, end, excludeId, LessonStatus.CANCELED);
            if (!r.isEmpty()) {
                throw new AppException(HttpStatus.CONFLICT, "Phòng " + room + " đã có lớp: " + describe(r.get(0)));
            }
        }
    }

    private String describe(Lesson l) {
        return l.getCourse().getCourseName() + " (" + l.getStartTime().format(FULL) + " - " + l.getEndTime().format(TIME) + ")";
    }

    private void notifyStudents(Lesson l, String title, String content) {
        List<Long> ids = enrollmentRepository.findActiveStudentIds(l.getCourse().getId());
        notificationService.createForUsers(ids, title, content);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
