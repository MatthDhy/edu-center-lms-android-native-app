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

import com.example.celehub.Data.SessionManager;
import com.example.celehub.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ((TextView) view.findViewById(R.id.tvUserName))
                .setText(SessionManager.get(requireContext()).getFullName());

        applyStatusBarInset(view.findViewById(R.id.headerContainer));
        setupClicks(view);

        return view;
    }

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
        bindToast(root, R.id.tileTuition, "Học phí");
        bindToast(root, R.id.tileQuiz, "Quiz");
        bindToast(root, R.id.tileGrades, "Hồ sơ & Bảng điểm");
        bindToast(root, R.id.tileSchedule, "Lịch học");
        bindToast(root, R.id.tileDocs, "Tài liệu");
        bindToast(root, R.id.tvViewAllAssignments, "Xem tất cả bài tập");
        bindToast(root, R.id.cardAssignment, "Đề ôn chương 1");
        bindToast(root, R.id.tvViewAllNotifications, "Tất cả thông báo");
    }

    private void bindToast(View root, int viewId, String message) {
        root.findViewById(viewId).setOnClickListener(v ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }
}