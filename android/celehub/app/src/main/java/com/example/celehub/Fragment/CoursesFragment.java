package com.example.celehub.Fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Adapter.CourseAdapter;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.CourseItem;
import com.example.celehub.Model.HomeTeacherResponse;
import com.example.celehub.R;

/** Tab Khóa học, dùng chung cho học viên và giảng viên (giảng viên có thêm 3 ô thống kê). */
public class CoursesFragment extends Fragment {

    private static final int TAB_ALL = 0;
    private static final int TAB_ACTIVE = 1;
    private static final int TAB_DONE = 2;

    private boolean isTeacher;
    private final List<CourseItem> all = new ArrayList<>();
    private int tab = TAB_ALL;
    private String query = "";

    private CourseAdapter adapter;
    private TextView[] tabViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        isTeacher = "TEACHER".equals(SessionManager.get(requireContext()).getRole());

        applyStatusBarInset(view.findViewById(R.id.headerContainer));

        // Nhãn theo role
        text(view, R.id.tabActive).setText(isTeacher ? "Đang dạy" : "Đang học");
        text(view, R.id.tvListTitle).setText(isTeacher ? "Lớp của tôi" : "Khóa học của bạn");
        view.findViewById(R.id.llStats).setVisibility(isTeacher ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Thông báo", Toast.LENGTH_SHORT).show());

        // Danh sách
        adapter = new CourseAdapter(course -> {
            if (course.getId() == null) return;
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer,
                            CourseDetailFragment.newInstance(course.getId(), course.getCourseName()))
                    .addToBackStack(null)
                    .commit();
        });
        RecyclerView rv = view.findViewById(R.id.rvCourses);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Tab lọc
        tabViews = new TextView[]{
                text(view, R.id.tabAll), text(view, R.id.tabActive), text(view, R.id.tabDone)};
        for (int i = 0; i < tabViews.length; i++) {
            final int index = i;
            tabViews[i].setOnClickListener(v -> selectTab(index));
        }
        selectTab(TAB_ALL);

        // Tìm kiếm
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                applyFilter();
            }
        });

        loadCourses();
        if (isTeacher) loadPendingGrading();
        return view;
    }

    // ===================== Gọi API =====================

    private void loadCourses() {
        ApiClient.get(requireContext()).getMyCourses()
                .enqueue(new Callback<ApiResponse<List<CourseItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<CourseItem>>> call,
                                           Response<ApiResponse<List<CourseItem>>> res) {
                        View v = getView();
                        if (!isAdded() || v == null) return;
                        v.findViewById(R.id.progressLoading).setVisibility(View.GONE);

                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            all.clear();
                            all.addAll(res.body().getData());
                            updateStats();
                            applyFilter();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được danh sách khóa học"),
                                    Toast.LENGTH_SHORT).show();
                            showEmpty("Không tải được danh sách khóa học");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<CourseItem>>> call, Throwable t) {
                        View v = getView();
                        if (!isAdded() || v == null) return;
                        v.findViewById(R.id.progressLoading).setVisibility(View.GONE);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                        showEmpty(ApiErrors.NETWORK_ERROR);
                    }
                });
    }

    /** Ô "Chờ chấm": lấy từ API Trang chủ giảng viên (tổng bài nộp chưa chấm ở mọi lớp). */
    private void loadPendingGrading() {
        ApiClient.get(requireContext()).getHomeTeacher()
                .enqueue(new Callback<ApiResponse<HomeTeacherResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<HomeTeacherResponse>> call,
                                           Response<ApiResponse<HomeTeacherResponse>> res) {
                        View v = getView();
                        if (!isAdded() || v == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            text(v, R.id.tvStatPending).setText(
                                    String.valueOf(res.body().getData().getPendingGradingCount()));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<HomeTeacherResponse>> call, Throwable t) {
                        // Ô vẫn hiện "–", không cần báo lỗi riêng
                    }
                });
    }

    // ===================== Lọc và hiển thị =====================

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

        String q = normalize(query);
        List<CourseItem> shown = new ArrayList<>();
        for (CourseItem c : all) {
            if (tab == TAB_ACTIVE && c.isFinished()) continue;
            if (tab == TAB_DONE && !c.isFinished()) continue;
            if (!q.isEmpty()
                    && !normalize(c.getCourseName()).contains(q)
                    && !normalize(c.getTeacherName()).contains(q)) continue;
            shown.add(c);
        }

        adapter.submit(shown);
        text(view, R.id.tvCount).setText(shown.size() + " khóa học");

        if (shown.isEmpty()) {
            showEmpty(all.isEmpty() ? "Chưa có khóa học nào" : "Không tìm thấy khóa học phù hợp");
        } else {
            view.findViewById(R.id.tvEmpty).setVisibility(View.GONE);
        }
    }

    /** Thống kê của giảng viên, tính trên các lớp chưa kết thúc. */
    private void updateStats() {
        View view = getView();
        if (view == null || !isTeacher) return;
        int classes = 0;
        long students = 0;
        for (CourseItem c : all) {
            if (c.isFinished()) continue;
            classes++;
            students += c.getStudentCount();
        }
        text(view, R.id.tvStatClasses).setText(String.valueOf(classes));
        text(view, R.id.tvStatStudents).setText(String.valueOf(students));
    }

    private void showEmpty(String message) {
        View view = getView();
        if (view == null) return;
        TextView empty = text(view, R.id.tvEmpty);
        empty.setText(message);
        empty.setVisibility(View.VISIBLE);
    }

    /** Bỏ dấu và chữ hoa để "toan" tìm được "Toán", "pho" tìm được "Phú". */
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return n.toLowerCase().trim();
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