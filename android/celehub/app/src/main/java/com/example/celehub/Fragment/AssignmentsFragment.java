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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Adapter.AssignmentAdapter;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.CourseItem;
import com.example.celehub.R;
import com.example.celehub.Util.HomeFormat;

/** Tab Bài tập của học viên: banner, tab lọc theo trạng thái, danh sách bài tập. */
public class AssignmentsFragment extends Fragment {

    private static final int TAB_ALL = 0;
    private static final int TAB_PENDING = 1;
    private static final int TAB_SUBMITTED = 2;
    private static final int TAB_OVERDUE = 3;

    private final List<AssignmentItem> all = new ArrayList<>();
    private int tab = TAB_ALL;
    private AssignmentAdapter adapter;
    private TextView[] tabViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assignments, container, false);

        applyStatusBarInset(view.findViewById(R.id.headerContainer));
        text(view, R.id.tvToday).setText(HomeFormat.todayLabel());
        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Thông báo", Toast.LENGTH_SHORT).show());

        adapter = new AssignmentAdapter(a -> {
            if (a.getId() == null) return;
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, AssignmentDetailFragment.newInstance(a.getId()))
                    .addToBackStack(null)
                    .commit();
        });
        RecyclerView rv = view.findViewById(R.id.rvAssignments);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        tabViews = new TextView[]{
                text(view, R.id.tabAll), text(view, R.id.tabPending),
                text(view, R.id.tabSubmitted), text(view, R.id.tabOverdue)};
        for (int i = 0; i < tabViews.length; i++) {
            final int index = i;
            tabViews[i].setOnClickListener(v -> selectTab(index));
        }
        selectTab(TAB_ALL);

        loadAssignments();
        loadTeachers();
        return view;
    }

    // ===================== Gọi API =====================

    private void loadAssignments() {
        ApiClient.get(requireContext()).getMyAssignments()
                .enqueue(new Callback<ApiResponse<List<AssignmentItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<AssignmentItem>>> call,
                                           Response<ApiResponse<List<AssignmentItem>>> res) {
                        View v = getView();
                        if (!isAdded() || v == null) return;
                        v.findViewById(R.id.progressLoading).setVisibility(View.GONE);

                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            all.clear();
                            all.addAll(res.body().getData());
                            Collections.sort(all, AssignmentsFragment::compare);
                            updateBanner();
                            applyFilter();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được danh sách bài tập"),
                                    Toast.LENGTH_SHORT).show();
                            showEmpty("Không tải được danh sách bài tập");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<AssignmentItem>>> call, Throwable t) {
                        View v = getView();
                        if (!isAdded() || v == null) return;
                        v.findViewById(R.id.progressLoading).setVisibility(View.GONE);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                        showEmpty(ApiErrors.NETWORK_ERROR);
                    }
                });
    }

    /** Lấy tên giảng viên theo lớp. Lỗi thì thôi, thẻ vẫn hiện, chỉ thiếu tên giảng viên. */
    private void loadTeachers() {
        ApiClient.get(requireContext()).getMyCourses()
                .enqueue(new Callback<ApiResponse<List<CourseItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<CourseItem>>> call,
                                           Response<ApiResponse<List<CourseItem>>> res) {
                        if (!isAdded() || adapter == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            Map<Long, String> map = new HashMap<>();
                            for (CourseItem c : res.body().getData()) {
                                if (c.getId() != null) map.put(c.getId(), c.getTeacherName());
                            }
                            adapter.setTeachers(map);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<CourseItem>>> call, Throwable t) { }
                });
    }

    // ===================== Sắp xếp, lọc, hiển thị =====================

    private static int rank(AssignmentItem a) {
        if (a.isPending()) return 0;
        if (a.isOverdue()) return 1;
        return 2;   // đã nộp, đã chấm
    }

    /** Chưa nộp lên đầu (hạn gần nhất trước), rồi quá hạn, cuối cùng là bài đã nộp (mới nhất trước). */
    private static int compare(AssignmentItem x, AssignmentItem y) {
        int rx = rank(x);
        int ry = rank(y);
        if (rx != ry) return Integer.compare(rx, ry);
        String dx = x.getDeadline() == null ? "" : x.getDeadline();
        String dy = y.getDeadline() == null ? "" : y.getDeadline();
        return rx == 2 ? dy.compareTo(dx) : dx.compareTo(dy);
    }

    private void selectTab(int index) {
        tab = index;
        for (int i = 0; i < tabViews.length; i++) {
            boolean on = i == index;
            tabViews[i].setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            tabViews[i].setTextColor(on ? 0xFFFFFFFF : 0xFF6C4CDC);
        }
        applyFilter();
    }

    private void applyFilter() {
        View view = getView();
        if (view == null) return;

        List<AssignmentItem> shown = new ArrayList<>();
        for (AssignmentItem a : all) {
            boolean keep;
            switch (tab) {
                case TAB_PENDING: keep = a.isPending(); break;
                case TAB_SUBMITTED: keep = a.isSubmitted() || a.isGraded(); break;
                case TAB_OVERDUE: keep = a.isOverdue(); break;
                default: keep = true;
            }
            if (keep) shown.add(a);
        }

        adapter.submit(shown);
        text(view, R.id.tvCount).setText(shown.size() + " bài tập");

        if (shown.isEmpty()) {
            showEmpty(all.isEmpty() ? "Chưa có bài tập nào" : "Không có bài tập ở mục này");
        } else {
            view.findViewById(R.id.tvEmpty).setVisibility(View.GONE);
        }
    }

    private void updateBanner() {
        View view = getView();
        if (view == null) return;
        int pending = 0;
        for (AssignmentItem a : all) if (a.isPending()) pending++;
        text(view, R.id.tvBanner).setText(pending > 0
                ? "Bạn còn " + pending + " bài tập cần hoàn thành."
                : "Bạn đã hoàn thành hết bài tập!");
    }

    private void showEmpty(String message) {
        View view = getView();
        if (view == null) return;
        TextView empty = text(view, R.id.tvEmpty);
        empty.setText(message);
        empty.setVisibility(View.VISIBLE);
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }

    /** Đẩy header xuống đúng bằng chiều cao thanh trạng thái (giống Trang chủ). */
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
}