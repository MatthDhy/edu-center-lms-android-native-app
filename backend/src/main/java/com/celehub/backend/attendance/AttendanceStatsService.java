package com.celehub.backend.attendance;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.course.LessonRepository;

/**
 * Chuyên cần của học viên = số buổi có mặt / số buổi ĐÃ diễn ra của các lớp đang theo.
 * rate = null khi chưa có buổi nào diễn ra (app hiển thị "—").
 */
@Service
@Transactional(readOnly = true)
public class AttendanceStatsService {

    public record Summary(int attended, int held, Double rate) {}

    private final AttendanceRecordRepository recordRepository;
    private final LessonRepository lessonRepository;

    public AttendanceStatsService(AttendanceRecordRepository recordRepository, LessonRepository lessonRepository) {
        this.recordRepository = recordRepository;
        this.lessonRepository = lessonRepository;
    }

    public Summary studentSummary(Long studentId) {
        long held = lessonRepository.countHeldForStudent(studentId, LocalDateTime.now());
        long attendedRaw = recordRepository.countAttended(studentId,
                List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE));
        long attended = Math.min(attendedRaw, held); // phòng khi 1 buổi có nhiều phiên điểm danh
        Double rate = held == 0 ? null : Math.round(attended * 1000.0 / held) / 10.0;
        return new Summary((int) attended, (int) held, rate);
    }
}
