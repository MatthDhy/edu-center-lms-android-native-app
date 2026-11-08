package com.example.celehub.Fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.LessonItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;

/** Lịch học (HS) / lịch dạy (GV) theo tuần. Dùng chung một màn, khác nhau ở nhãn chữ. */
public class ScheduleFragment extends Fragment {

    private static final String[] DOW = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private static final TimeZone VN = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    /** {màu nền thẻ, màu chữ}: gán theo courseId để mỗi môn luôn cùng một màu. */
    private static final int[][] PALETTE = {
            {0xFFD6E6FF, 0xFF1E4F9E},   // xanh dương
            {0xFFFDE7A8, 0xFF8A5A00},   // vàng
            {0xFFCDEFD7, 0xFF1F7A3E},   // xanh lá
            {0xFFE6DEFB, 0xFF5B3FD0},   // tím
    };

    private boolean isTeacher;
    private Calendar weekStart;      // thứ Hai 00:00 của tuần đang xem
    private String selectedKey;      // ngày đang chọn, dạng yyyy-MM-dd
    private boolean loadingNow;
    private final Map<String, List<LessonItem>> byDay = new HashMap<>();
    private Call<ApiResponse<List<LessonItem>>> loading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        isTeacher = "TEACHER".equals(SessionManager.get(requireContext()).getRole());
        ChildHeader.bind(this, view, isTeacher ? "Lịch dạy" : "Lịch học");

        Calendar today = Calendar.getInstance();
        weekStart = mondayOf(today);
        selectedKey = key(today);

        view.findViewById(R.id.btnPrevWeek).setOnClickListener(v -> shiftWeek(-1));
        view.findViewById(R.id.btnNextWeek).setOnClickListener(v -> shiftWeek(1));
        view.findViewById(R.id.btnPickDate).setOnClickListener(v -> pickDate());

        loadWeek();
        return view;
    }

    @Override
    public void onDestroyView() {
        if (loading != null) loading.cancel();
        super.onDestroyView();
    }

    // ===================== Chuyển tuần / chọn ngày =====================

    private void shiftWeek(int delta) {
        weekStart.add(Calendar.DAY_OF_MONTH, 7 * delta);
        String todayKey = key(Calendar.getInstance());
        Calendar end = (Calendar) weekStart.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        boolean containsToday = todayKey.compareTo(key(weekStart)) >= 0 && todayKey.compareTo(key(end)) <= 0;
        selectedKey = containsToday ? todayKey : key(weekStart);   // tuần khác: chọn thứ Hai
        loadWeek();
    }

    private void pickDate() {
        Calendar sel = parseKey(selectedKey);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(sel.get(Calendar.YEAR), sel.get(Calendar.MONTH), sel.get(Calendar.DAY_OF_MONTH));

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày")
                .setSelection(utc.getTimeInMillis())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            u.setTimeInMillis(selection);
            Calendar picked = Calendar.getInstance();
            picked.clear();
            picked.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH));
            weekStart = mondayOf(picked);
            selectedKey = key(picked);
            loadWeek();
        });
        picker.show(getParentFragmentManager(), "schedule_date");
    }

    // ===================== Gọi API =====================

    private void loadWeek() {
        View v = getView();
        if (v == null) return;
        if (loading != null) loading.cancel();

        byDay.clear();
        Calendar end = (Calendar) weekStart.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);
        ((TextView) v.findViewById(R.id.tvWeekRange)).setText(
                String.format(Locale.US, "%02d/%02d – %02d/%02d/%d",
                        weekStart.get(Calendar.DAY_OF_MONTH), weekStart.get(Calendar.MONTH) + 1,
                        end.get(Calendar.DAY_OF_MONTH), end.get(Calendar.MONTH) + 1, end.get(Calendar.YEAR)));

        loadingNow = true;
        render();

        loading = ApiClient.get(requireContext()).getLessons(key(weekStart), key(end));
        loading.enqueue(new Callback<ApiResponse<List<LessonItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LessonItem>>> call,
                                   Response<ApiResponse<List<LessonItem>>> res) {
                if (call.isCanceled() || !isAdded() || getView() == null) return;
                loadingNow = false;
                if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                    group(res.body().getData());
                } else {
                    Toast.makeText(requireContext(),
                            ApiErrors.messageOf(res, "Không tải được lịch"), Toast.LENGTH_SHORT).show();
                }
                render();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LessonItem>>> call, Throwable t) {
                if (call.isCanceled() || !isAdded() || getView() == null) return;
                loadingNow = false;
                Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                render();
            }
        });
    }

    /** Gom buổi học theo ngày (lấy 10 ký tự đầu của startTime) và sắp theo giờ bắt đầu. */
    private void group(List<LessonItem> lessons) {
        for (LessonItem l : lessons) {
            if (l.getStartTime() == null || l.getStartTime().length() < 10) continue;
            String day = l.getStartTime().substring(0, 10);
            List<LessonItem> list = byDay.get(day);
            if (list == null) {
                list = new ArrayList<>();
                byDay.put(day, list);
            }
            list.add(l);
        }
        for (List<LessonItem> list : byDay.values()) {
            Collections.sort(list, (a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        }
    }

    // ===================== Vẽ giao diện =====================

    private void render() {
        View v = getView();
        if (v == null) return;
        renderDays(v);
        renderLessons(v);
    }

    private void renderDays(View v) {
        LinearLayout row = v.findViewById(R.id.llDays);
        row.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String todayKey = key(Calendar.getInstance());
        int primary = requireContext().getColor(R.color.color_primary);

        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) weekStart.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            String k = key(d);
            boolean selected = k.equals(selectedKey);

            View cell = inflater.inflate(R.layout.item_schedule_day, row, false);
            TextView dow = cell.findViewById(R.id.tvDow);
            TextView num = cell.findViewById(R.id.tvDayNum);
            View dot = cell.findViewById(R.id.dotDay);

            dow.setText(DOW[i]);
            num.setText(String.valueOf(d.get(Calendar.DAY_OF_MONTH)));

            if (selected) {
                cell.setBackgroundTintList(ColorStateList.valueOf(primary));
                dow.setTextColor(0xFFFFFFFF);
                num.setTextColor(0xFFFFFFFF);
                dot.setBackgroundTintList(ColorStateList.valueOf(0xFFFFFFFF));
            } else {
                cell.setBackgroundTintList(ColorStateList.valueOf(
                        requireContext().getColor(R.color.color_primary_container)));
                dow.setTextColor(requireContext().getColor(R.color.color_text_muted));
                num.setTextColor(k.equals(todayKey) ? primary
                        : requireContext().getColor(R.color.color_on_surface));   // hôm nay: số màu tím
                dot.setBackgroundTintList(ColorStateList.valueOf(primary));
            }
            dot.setVisibility(hasActiveLesson(byDay.get(k)) ? View.VISIBLE : View.INVISIBLE);

            cell.setOnClickListener(x -> {
                selectedKey = k;
                render();
            });
            row.addView(cell);
        }
    }

    private void renderLessons(View v) {
        LinearLayout list = v.findViewById(R.id.llLessons);
        TextView empty = v.findViewById(R.id.tvEmpty);
        list.removeAllViews();

        Calendar sel = parseKey(selectedKey);
        ((TextView) v.findViewById(R.id.tvSelectedDate)).setText(String.format(Locale.US, "%s, %02d/%02d/%d",
                weekdayName(sel), sel.get(Calendar.DAY_OF_MONTH), sel.get(Calendar.MONTH) + 1, sel.get(Calendar.YEAR)));

        if (loadingNow) {
            empty.setText("Đang tải...");
            empty.setVisibility(View.VISIBLE);
            return;
        }
        List<LessonItem> items = byDay.get(selectedKey);
        if (items == null || items.isEmpty()) {
            empty.setText(isTeacher ? "Không có ca dạy nào trong ngày này."
                    : "Không có ca học nào trong ngày này.");
            empty.setVisibility(View.VISIBLE);
            return;
        }
        empty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LessonItem l : items) list.addView(buildLessonRow(inflater, list, l));
    }

    private View buildLessonRow(LayoutInflater inflater, ViewGroup parent, LessonItem l) {
        View row = inflater.inflate(R.layout.item_schedule_lesson, parent, false);

        long id = l.getCourseId() == null ? 0 : l.getCourseId();
        int[] pal = PALETTE[(int) (id % PALETTE.length)];
        boolean canceled = "CANCELED".equals(l.getStatus());

        TextView start = row.findViewById(R.id.tvStart);
        TextView end = row.findViewById(R.id.tvEnd);
        start.setText(hhmm(l.getStartTime()));
        end.setText(hhmm(l.getEndTime()));
        start.setTextColor(canceled ? requireContext().getColor(R.color.color_disable) : pal[1]);

        View box = row.findViewById(R.id.boxLesson);
        box.setBackgroundTintList(ColorStateList.valueOf(canceled ? 0xFFF3F4F6 : pal[0]));

        TextView course = row.findViewById(R.id.tvCourse);
        course.setText(l.getCourseName());
        course.setTextColor(canceled ? requireContext().getColor(R.color.color_text_muted) : pal[1]);
        if (canceled) course.setPaintFlags(course.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // HS: "Tên GV • Phòng". GV: "Chủ đề • Phòng"
        String who = isTeacher ? l.getTopic() : l.getTeacherName();
        String room = l.getRoom();
        StringBuilder sb = new StringBuilder();
        if (who != null && !who.isEmpty()) sb.append(who);
        if (room != null && !room.isEmpty()) sb.append(sb.length() > 0 ? " • " : "").append(room);
        TextView sub = row.findViewById(R.id.tvSub);
        sub.setText(sb);
        sub.setVisibility(sb.length() > 0 ? View.VISIBLE : View.GONE);
        sub.setTextColor(canceled ? requireContext().getColor(R.color.color_text_muted) : pal[1]);

        // Nhãn trạng thái
        long now = System.currentTimeMillis();
        long s = millis(l.getStartTime());
        long e = millis(l.getEndTime());
        String badgeText = null;
        int badgeColor = requireContext().getColor(R.color.color_primary);
        if (canceled) {
            badgeText = "Đã hủy";
            badgeColor = requireContext().getColor(R.color.color_danger);
        } else if ("DONE".equals(l.getStatus())) {
            badgeText = isTeacher ? "Đã dạy" : "Đã học";
            badgeColor = requireContext().getColor(R.color.color_disable);
        } else if (s > 0 && e > 0 && now >= s && now < e) {
            badgeText = "Đang diễn ra";
            badgeColor = requireContext().getColor(R.color.color_success);
        } else if (s > 0 && now < s && s - now <= DAY_MS) {
            badgeText = "Sắp diễn ra";
        }
        TextView badge = row.findViewById(R.id.tvBadge);
        if (badgeText == null) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setText(badgeText);
            badge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            badge.setVisibility(View.VISIBLE);
        }

        // Nút thêm vào lịch: chỉ cho buổi chưa hủy và chưa kết thúc
        TextView add = row.findViewById(R.id.tvAddCalendar);
        boolean addable = !canceled && !"DONE".equals(l.getStatus()) && s > 0 && e > now;
        add.setVisibility(addable ? View.VISIBLE : View.GONE);
        add.setTextColor(pal[1]);
        add.setOnClickListener(x -> addToCalendar(l, s, e));

        return row;
    }

    // ===================== Thêm vào lịch (Google Calendar hoặc app lịch khác) =====================

    private void addToCalendar(LessonItem l, long startMillis, long endMillis) {
        String desc = isTeacher ? l.getTopic() : l.getTeacherName();
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, l.getCourseName())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, l.getRoom())
                .putExtra(CalendarContract.Events.DESCRIPTION, desc)
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Ho_Chi_Minh");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "Máy chưa có ứng dụng lịch", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== Tiện ích ngày giờ (không dùng java.time vì minSdk 24) =====================

    private boolean hasActiveLesson(@Nullable List<LessonItem> list) {
        if (list == null) return false;
        for (LessonItem l : list) if (!"CANCELED".equals(l.getStatus())) return true;
        return false;
    }

    private static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar parseKey(String key) {
        String[] p = key.split("-");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        return c;
    }

    /** Thứ Hai của tuần chứa ngày src, lúc 00:00. */
    private static Calendar mondayOf(Calendar src) {
        Calendar c = (Calendar) src.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // T2=0 ... CN=6
        c.add(Calendar.DAY_OF_MONTH, -offset);
        return c;
    }

    private static String weekdayName(Calendar c) {
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: return "Thứ 2";
            case Calendar.TUESDAY: return "Thứ 3";
            case Calendar.WEDNESDAY: return "Thứ 4";
            case Calendar.THURSDAY: return "Thứ 5";
            case Calendar.FRIDAY: return "Thứ 6";
            case Calendar.SATURDAY: return "Thứ 7";
            default: return "Chủ nhật";
        }
    }

    /** "2026-10-15T18:00:00" -> "18:00" */
    private static String hhmm(String iso) {
        return iso == null || iso.length() < 16 ? "" : iso.substring(11, 16);
    }

    /** Chuỗi giờ của backend là giờ Việt Nam; đổi ra mili-giây tuyệt đối, lỗi trả -1. */
    private static long millis(String iso) {
        if (iso == null || iso.length() < 19) return -1;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            f.setTimeZone(VN);
            Date d = f.parse(iso.substring(0, 19));
            return d == null ? -1 : d.getTime();
        } catch (ParseException ex) {
            return -1;
        }
    }
}