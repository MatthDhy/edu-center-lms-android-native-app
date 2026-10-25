package com.celehub.backend.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.attendance.AttendanceStatsService;
import com.celehub.backend.common.AppException;
import com.celehub.backend.course.CourseRepository;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.LessonRepository;
import com.celehub.backend.user.dto.ProfileResponse;
import com.celehub.backend.user.dto.ProfileResponse.StudentStats;
import com.celehub.backend.user.dto.ProfileResponse.TeacherStats;
import com.celehub.backend.user.dto.UserDto;

@Service
@Transactional(readOnly = true)
public class ProfileService {
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final AttendanceStatsService attendanceStatsService;

    public ProfileService(UserRepository userRepository, EnrollmentRepository enrollmentRepository,
                          CourseRepository courseRepository, LessonRepository lessonRepository,
                          AttendanceStatsService attendanceStatsService) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.attendanceStatsService = attendanceStatsService;
    }

    public ProfileResponse getProfile(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        UserDto dto = UserDto.from(u);

        if (u.getRole() == Role.STUDENT) {
            var att = attendanceStatsService.studentSummary(userId);
            return new ProfileResponse(dto,
                    new StudentStats(enrollmentRepository.countActiveCoursesOfStudent(userId),
                            att.attended(), att.held(), att.rate()),
                    null);
        }
        if (u.getRole() == Role.TEACHER) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            return new ProfileResponse(dto, null,
                    new TeacherStats(courseRepository.countByTeacherIdAndStatus(userId, Course.ACTIVE),
                            enrollmentRepository.countActiveStudentsOfTeacher(userId),
                            lessonRepository.countTaughtByTeacher(userId, monthStart, now)));
        }
        return new ProfileResponse(dto, null, null);
    }
}
