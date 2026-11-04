package com.example.celehub.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.celehub.Model.LessonSummary;
import com.example.celehub.Model.TaskItem;
import com.example.celehub.Model.NotificationItem;

/**
 * Biến dữ liệu thô từ API thành chữ hiển thị trên Home.
 * minSdk = 24 nên dùng SimpleDateFormat thay cho java.time (cần API 26).
 */
public final class HomeFormat {

    private HomeFormat() { }

    /** "2026-10-15T18:00:00" (có thể kèm phần giây lẻ) -> Date. Sai định dạng thì trả null. */
    public static Date parse(String iso) {
        if (iso == null || iso.length() < 19) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso.substring(0, 19));
        } catch (ParseException e) {
            return null;
        }
    }

    private static String fmt(Date d, String pattern) {
        return new SimpleDateFormat(pattern, Locale.US).format(d);
    }

    private static String weekday(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int day = c.get(Calendar.DAY_OF_WEEK);   // CHỦ NHẬT = 1 ... THỨ BẢY = 7
        return day == Calendar.SUNDAY ? "Chủ nhật" : "Thứ " + day;
    }

    /** "Thứ 5, 15/10 • 18:00–19:30 • P.302" */
    public static String lessonTime(LessonSummary l) {
        Date start = parse(l.getStartTime());
        Date end = parse(l.getEndTime());
        if (start == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(weekday(start)).append(", ").append(fmt(start, "dd/MM"))
                .append(" • ").append(fmt(start, "HH:mm"));
        if (end != null) sb.append("–").append(fmt(end, "HH:mm"));
        if (l.getRoom() != null && !l.getRoom().isEmpty()) sb.append(" • ").append(l.getRoom());
        return sb.toString();
    }

    /** "Bắt đầu sau 20 phút" / "Bắt đầu sau 2 giờ" / "Đang diễn ra". */
    public static String countdown(LessonSummary l) {
        if (l.isOngoing()) return "Đang diễn ra";
        long m = l.getMinutesUntilStart();
        if (m < 60) return "Bắt đầu sau " + Math.max(m, 1) + " phút";
        if (m < 24 * 60) return "Bắt đầu sau " + (m / 60) + " giờ";
        return "Bắt đầu sau " + (m / (24 * 60)) + " ngày";
    }

    /** "Toán 11 • Hạn 16/10, 23:59" */
    public static String taskSub(TaskItem t) {
        Date d = parse(t.getDeadline());
        String deadline = d == null ? "" : " • Hạn " + fmt(d, "dd/MM, HH:mm");
        return t.getCourseName() + deadline;
    }

    /** "Còn 25 phút" / "Còn 5 giờ" / "Còn 1 ngày". */
    public static String remaining(TaskItem t) {
        long m = t.getMinutesLeft();
        if (m < 60) return "Còn " + Math.max(m, 1) + " phút";
        if (m < 24 * 60) return "Còn " + (m / 60) + " giờ";
        return "Còn " + (m / (24 * 60)) + " ngày";
    }
    /** "Quiz Chương 1 đã mở • 15 phút trước" */
    public static String notification(NotificationItem n) {
        String ago = ago(n.getCreatedAt());
        return ago.isEmpty() ? n.getTitle() : n.getTitle() + " • " + ago;
    }

    /** "Vừa xong" / "15 phút trước" / "1 giờ trước" / "2 ngày trước" / "05/10/2026". */
    public static String ago(String iso) {
        Date d = parse(iso);
        if (d == null) return "";
        long minutes = (System.currentTimeMillis() - d.getTime()) / 60000;
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (minutes < 24 * 60) return (minutes / 60) + " giờ trước";
        if (minutes < 7 * 24 * 60) return (minutes / (24 * 60)) + " ngày trước";
        return fmt(d, "dd/MM/yyyy");
    }
    /** "2026-11-01T09:30:00" -> "01/11/2026" */
    public static String date(String iso) {
        Date d = parse(iso);
        return d == null ? "" : fmt(d, "dd/MM/yyyy");
    }
    /** "2026-10-15T23:59:00" -> "15/10/2026, 23:59" */
    public static String dateTime(String iso) {
        Date d = parse(iso);
        return d == null ? "" : fmt(d, "dd/MM/yyyy, HH:mm");
    }

    /** "Hôm nay, 7/10/2026" */
    public static String todayLabel() {
        return "Hôm nay, " + fmt(new Date(), "d/M/yyyy");
    }

    /** 8.5 -> "8.5", 10.0 -> "10" */
    public static String score(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
    /** "2026-10-15T20:15:00" -> "15/10, 20:15" */
    public static String shortDateTime(String iso) {
        Date d = parse(iso);
        return d == null ? "" : fmt(d, "dd/MM, HH:mm");
    }

    /** "Gia Huy" -> "GH" (chữ cái đầu của từ đầu và từ cuối). */
    public static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    /** 248 -> "HV0248", giống mã hiển thị ở màn Cá nhân. */
    public static String studentCode(Long id) {
        return id == null ? "" : String.format(Locale.US, "HV%04d", id);
    }
}
