package com.example.celehub.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.HomeStudentResponse;
import com.example.celehub.Model.HomeTeacherResponse;
import com.example.celehub.Model.LessonSummary;
import com.example.celehub.Model.NotificationItem;
import com.example.celehub.Model.TaskItem;
import com.example.celehub.Model.UserDto;
import com.example.celehub.R;
import com.example.celehub.Util.HomeFormat;

/**
 * Trang chủ dùng chung cho học viên và giảng viên: cùng một layout,
 * khác nhau ở API gọi (home/student hay home/teacher) và vài nhãn chữ.
 */
public class HomeFragment extends Fragment {

    private boolean isTeacher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SessionManager session = SessionManager.get(requireContext());
        isTeacher = "TEACHER".equals(session.getRole());

        // Hiện ngay tên đã lưu lúc đăng nhập, API về sau sẽ ghi đè nếu khác
        text(view, R.id.tvUserName).setText(session.getFullName());
        text(view, R.id.tvAvatar).setText(session.getInitials());
        text(view, R.id.tvBadge).setVisibility(View.GONE);
        text(view, R.id.tvNotif1).setVisibility(View.GONE);
        text(view, R.id.tvNotif2).setVisibility(View.GONE);

        applyRoleLabels(view);
        applyStatusBarInset(view.findViewById(R.id.headerContainer));
        setupClicks(view);
        loadHome();

        return view;
    }

    // ===================== Nhãn theo role =====================

    private void applyRoleLabels(View view) {
        if (!isTeacher) return;   // layout mặc định đang là bản học viên
        text(view, R.id.tvGreeting).setText("Xin chào, thầy/cô");
        text(view, R.id.tvLessonLabel).setText("Ca dạy tiếp theo");
        text(view, R.id.tvTaskSection).setText("Bài tập đã giao");
        text(view, R.id.tvTileTuition).setText("Lương");
        text(view, R.id.tvTileSchedule).setText("Lịch dạy");
    }

    // ===================== Gọi API =====================

    private void loadHome() {
        if (isTeacher) {
            ApiClient.get(requireContext()).getHomeTeacher()
                    .enqueue(new Callback<ApiResponse<HomeTeacherResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<HomeTeacherResponse>> call,
                                               Response<ApiResponse<HomeTeacherResponse>> res) {
                            if (!usable(res)) return;
                            HomeTeacherResponse d = res.body().getData();
                            bind(d.getUser(), d.getUnreadNotifications(),
                                    d.getNextLesson(), d.getUpcomingTasks(), d.getRecentNotifications());
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<HomeTeacherResponse>> call, Throwable t) {
                            networkError();
                        }
                    });
        } else {
            ApiClient.get(requireContext()).getHomeStudent()
                    .enqueue(new Callback<ApiResponse<HomeStudentResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<HomeStudentResponse>> call,
                                               Response<ApiResponse<HomeStudentResponse>> res) {
                            if (!usable(res)) return;
                            HomeStudentResponse d = res.body().getData();
                            bind(d.getUser(), d.getUnreadNotifications(),
                                    d.getNextLesson(), d.getUpcomingTasks(), d.getRecentNotifications());
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<HomeStudentResponse>> call, Throwable t) {
                            networkError();
                        }
                    });
        }
    }

    /** true nếu fragment còn sống và server trả dữ liệu hợp lệ; ngược lại báo lỗi rồi trả false. */
    private <T> boolean usable(Response<ApiResponse<T>> res) {
        if (!isAdded() || getView() == null) return false;   // người dùng đã rời màn hình
        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) return true;
        Toast.makeText(requireContext(),
                ApiErrors.messageOf(res, "Không tải được trang chủ"), Toast.LENGTH_SHORT).show();
        return false;
    }

    private void networkError() {
        if (!isAdded() || getView() == null) return;
        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
    }

    // ===================== Hiển thị =====================

    private void bind(UserDto user, long unread, LessonSummary lesson, List<TaskItem> tasks, List<NotificationItem> notifications) {
        View view = getView();
        if (view == null) return;

        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
            text(view, R.id.tvUserName).setText(user.getFullName());
        }

        TextView badge = text(view, R.id.tvBadge);
        badge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));

        bindLesson(view, lesson);
        bindTask(view, tasks);
        bindNotifications(view, notifications);
    }

    /** 1. Ca học / ca dạy tiếp theo. lesson = null nghĩa là chưa có buổi nào sắp tới. */
    private void bindLesson(View view, @Nullable LessonSummary lesson) {
        TextView name = text(view, R.id.tvLessonName);
        TextView time = text(view, R.id.tvLessonTime);
        TextView sub = text(view, R.id.tvLessonSub);
        View button = view.findViewById(R.id.btnViewClass);

        if (lesson == null) {
            name.setText(isTeacher ? "Chưa có ca dạy sắp tới" : "Chưa có ca học sắp tới");
            time.setVisibility(View.GONE);
            sub.setVisibility(View.GONE);
            button.setVisibility(View.GONE);
            return;
        }

        name.setText(lesson.getCourseName());
        time.setText(HomeFormat.lessonTime(lesson));
        time.setVisibility(View.VISIBLE);

        // Học viên: "Nguyễn Thu Hà • Bắt đầu sau 20 phút". Giảng viên: "24 học viên • Bắt đầu sau 20 phút".
        String who = isTeacher
                ? (lesson.getStudentCount() == null ? null : lesson.getStudentCount() + " học viên")
                : lesson.getTeacherName();
        String countdown = HomeFormat.countdown(lesson);
        sub.setText(who == null || who.isEmpty() ? countdown : who + " • " + countdown);
        sub.setVisibility(View.VISIBLE);

        button.setVisibility(View.VISIBLE);
    }

    /** 2. Bài tập sắp đến hạn (học viên) / đã giao (giảng viên). Hiện bài gần hạn nhất. */
    private void bindTask(View view, @Nullable List<TaskItem> tasks) {
        View card = view.findViewById(R.id.cardAssignment);
        View empty = view.findViewById(R.id.tvTaskEmpty);

        if (tasks == null || tasks.isEmpty()) {
            card.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            return;
        }

        TaskItem t = tasks.get(0);   // backend đã sắp xếp theo hạn gần nhất
        text(view, R.id.tvTaskTitle).setText(t.getTitle());
        text(view, R.id.tvTaskSub).setText(HomeFormat.taskSub(t));
        text(view, R.id.tvTaskBadge).setText(HomeFormat.remaining(t));
        card.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        card.setOnClickListener(v ->
                Toast.makeText(requireContext(), t.getTitle(), Toast.LENGTH_SHORT).show());
    }

    /** Thông báo mới. Hiện tối đa 2 dòng như Figma; không có thì báo "không có thông báo mới". */
    private void bindNotifications(View view, @Nullable List<NotificationItem> list) {
        TextView line1 = text(view, R.id.tvNotif1);
        TextView line2 = text(view, R.id.tvNotif2);
        View empty = view.findViewById(R.id.tvNotifEmpty);

        int count = list == null ? 0 : list.size();
        empty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

        line1.setVisibility(count >= 1 ? View.VISIBLE : View.GONE);
        if (count >= 1) line1.setText(HomeFormat.notification(list.get(0)));

        line2.setVisibility(count >= 2 ? View.VISIBLE : View.GONE);
        if (count >= 2) line2.setText(HomeFormat.notification(list.get(1)));
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }

    // ===================== Giữ nguyên từ bản cũ =====================

    /**
     * Header tím nằm sát mép trên màn hình, nên cần đẩy nội dung xuống một đoạn
     * bằng chiều cao thanh trạng thái (đồng hồ, pin...) để không bị che.
     */
    private void applyStatusBarInset(View header) {
        final int basePaddingTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), basePaddingTop + statusBarHeight,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /** Tạm thời bấm vào đâu cũng hiện Toast. Sau này thay bằng mở màn hình thật. */
    private void setupClicks(View root) {
        bindToast(root, R.id.btnNotification, "Thông báo");
        bindToast(root, R.id.btnViewClass, "Xem lớp học");
        bindToast(root, R.id.tileAttendance, "Điểm danh");
        bindToast(root, R.id.tileTuition, isTeacher ? "Lương" : "Học phí");
        bindToast(root, R.id.tileQuiz, "Quiz");
        bindToast(root, R.id.tileGrades, "Hồ sơ & Bảng điểm");
        bindToast(root, R.id.tileSchedule, isTeacher ? "Lịch dạy" : "Lịch học");
        bindToast(root, R.id.tileDocs, "Tài liệu");
        bindToast(root, R.id.tvViewAllAssignments, "Xem tất cả bài tập");
        bindToast(root, R.id.tvViewAllNotifications, "Tất cả thông báo");
    }

    private void bindToast(View root, int viewId, String message) {
        root.findViewById(viewId).setOnClickListener(v ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }
}
