package com.example.celehub.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Adapter.AssignmentAdapter;
import com.example.celehub.Adapter.SubmissionAdapter;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.CourseItem;
import com.example.celehub.Model.GradeRow;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.Model.SubmissionsOverview;
import com.example.celehub.R;

/** Tab Bài tập của giảng viên: Chờ chấm, Đã chấm và Quản lý, theo từng lớp. */
public class TeacherAssignmentsFragment extends Fragment {

    private static final int TAB_PENDING = 0;
    private static final int TAB_GRADED = 1;
    private static final int TAB_MANAGE = 2;

    // Nhớ tab đang xem để quay lại từ màn Chấm bài hoặc Tạo bài vẫn ở đúng tab
    private static int lastTab = TAB_PENDING;

    private final List<CourseItem> courses = new ArrayList<>();
    private final List<AssignmentItem> assignments = new ArrayList<>();
    private final List<GradeRow> rows = new ArrayList<>();      // mọi bài nộp của lớp đang chọn
    private List<GradeRow> shown = new ArrayList<>();           // bài nộp đang hiện trên tab

    private int selected = -1;
    private int requestId = 0;          // bỏ qua kết quả của lớp đã chọn trước đó
    private int tab = lastTab;
    private boolean assignmentsReady;
    private boolean rowsReady;

    private AssignmentAdapter assignmentAdapter;
    private SubmissionAdapter submissionAdapter;
    private TextView[] tabViews;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_assignments, container, false);

        applyStatusBarInset(view.findViewById(R.id.headerContainer));
        view.findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Thông báo", Toast.LENGTH_SHORT).show());

        // Tab Quản lý: danh sách bài đã giao
        assignmentAdapter = new AssignmentAdapter(true, a ->
                Toast.makeText(requireContext(), a.getTitle(), Toast.LENGTH_SHORT).show());
        RecyclerView rvAssignments = view.findViewById(R.id.rvAssignments);
        rvAssignments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAssignments.setAdapter(assignmentAdapter);

        // Tab Chờ chấm / Đã chấm: danh sách bài nộp
        submissionAdapter = new SubmissionAdapter((position, row) -> openGrade(position));
        RecyclerView rvSubmissions = view.findViewById(R.id.rvSubmissions);
        rvSubmissions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSubmissions.setAdapter(submissionAdapter);

        tabViews = new TextView[]{
                text(view, R.id.tabPending), text(view, R.id.tabGraded), text(view, R.id.tabManage)};
        for (int i = 0; i < tabViews.length; i++) {
            final int index = i;
            tabViews[i].setOnClickListener(v -> selectTab(index));
        }
        styleTabs();

        view.findViewById(R.id.boxClass).setOnClickListener(v -> showCoursePicker());
        view.findViewById(R.id.btnCreate).setOnClickListener(v -> openCreate());

        applyTab();
        loadCourses();
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

                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            courses.clear();
                            courses.addAll(res.body().getData());
                            if (courses.isEmpty()) {
                                assignmentsReady = true;
                                rowsReady = true;
                                applyTab();
                                return;
                            }
                            selected = 0;
                            bindSelected();
                            loadClassData();
                        } else {
                            assignmentsReady = true;
                            rowsReady = true;
                            applyTab();
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được danh sách lớp"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<CourseItem>>> call, Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        assignmentsReady = true;
                        rowsReady = true;
                        applyTab();
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Tải bài tập của lớp đang chọn, rồi tải bài nộp của từng bài đã có người nộp. */
    private void loadClassData() {
        if (selected < 0) return;
        Long courseId = courses.get(selected).getId();
        if (courseId == null) return;

        final int myRequest = ++requestId;
        assignmentsReady = false;
        rowsReady = false;
        assignments.clear();
        rows.clear();
        assignmentAdapter.submit(new ArrayList<>());
        applyTab();

        ApiClient.get(requireContext()).getCourseAssignments(courseId)
                .enqueue(new Callback<ApiResponse<List<AssignmentItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<AssignmentItem>>> call,
                                           Response<ApiResponse<List<AssignmentItem>>> res) {
                        if (!isAdded() || getView() == null || myRequest != requestId) return;

                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            assignments.addAll(res.body().getData());
                            Collections.sort(assignments, TeacherAssignmentsFragment::compareAssignments);
                            assignmentAdapter.submit(assignments);
                            assignmentsReady = true;
                            fetchSubmissions(myRequest);
                        } else {
                            assignmentsReady = true;
                            rowsReady = true;
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được bài tập"),
                                    Toast.LENGTH_SHORT).show();
                        }
                        applyTab();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<AssignmentItem>>> call, Throwable t) {
                        if (!isAdded() || getView() == null || myRequest != requestId) return;
                        assignmentsReady = true;
                        rowsReady = true;
                        applyTab();
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Backend không có API gộp bài nộp, nên gọi cho từng bài tập có người nộp rồi gộp lại. */
    private void fetchSubmissions(final int myRequest) {
        List<AssignmentItem> withSubmissions = new ArrayList<>();
        for (AssignmentItem a : assignments) {
            long submitted = a.getSubmittedCount() == null ? 0 : a.getSubmittedCount();
            if (submitted > 0 && a.getId() != null) withSubmissions.add(a);
        }
        if (withSubmissions.isEmpty()) {
            rowsReady = true;
            applyTab();
            return;
        }

        final int[] remaining = {withSubmissions.size()};
        for (AssignmentItem a : withSubmissions) {
            ApiClient.get(requireContext()).getSubmissions(a.getId())
                    .enqueue(new Callback<ApiResponse<SubmissionsOverview>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<SubmissionsOverview>> call,
                                               Response<ApiResponse<SubmissionsOverview>> res) {
                            if (!isAdded() || myRequest != requestId) return;
                            if (res.isSuccessful() && res.body() != null && res.body().getData() != null
                                    && res.body().getData().getSubmitted() != null) {
                                for (SubmissionItem s : res.body().getData().getSubmitted()) {
                                    rows.add(new GradeRow(s, a.getTitle(), a.getCourseName(),
                                            a.getMaxScoreOrDefault()));
                                }
                            }
                            finishOne(myRequest, remaining);
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<SubmissionsOverview>> call, Throwable t) {
                            if (!isAdded() || myRequest != requestId) return;
                            finishOne(myRequest, remaining);
                        }
                    });
        }
    }

    private void finishOne(int myRequest, int[] remaining) {
        if (myRequest != requestId) return;
        remaining[0]--;
        if (remaining[0] == 0) {
            rowsReady = true;
            applyTab();
        }
    }

    // ===================== Tab và hiển thị =====================

    private void selectTab(int index) {
        tab = index;
        lastTab = index;
        styleTabs();
        applyTab();
    }

    private void styleTabs() {
        for (int i = 0; i < tabViews.length; i++) {
            boolean on = i == tab;
            if (on) {
                tabViews[i].setBackgroundResource(R.drawable.bg_round_12);
                tabViews[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
                tabViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
            } else {
                tabViews[i].setBackground(null);
                tabViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_muted));
            }
        }
    }

    /** Cập nhật mọi thứ trên màn hình theo tab, lớp và trạng thái tải hiện tại. */
    private void applyTab() {
        View v = getView();
        if (v == null) return;

        boolean manage = tab == TAB_MANAGE;
        boolean ready = manage ? assignmentsReady : rowsReady;
        boolean hasCourses = !courses.isEmpty();

        v.findViewById(R.id.rvAssignments).setVisibility(manage ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.rvSubmissions).setVisibility(manage ? View.GONE : View.VISIBLE);
        v.findViewById(R.id.btnCreate).setVisibility(manage && hasCourses ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.progressLoading).setVisibility(ready ? View.GONE : View.VISIBLE);

        TextView title = text(v, R.id.tvListTitle);
        TextView count = text(v, R.id.tvCount);
        TextView empty = text(v, R.id.tvEmpty);
        empty.setVisibility(View.GONE);

        if (manage) {
            title.setText("Bài tập đã giao");
            count.setText(ready ? assignments.size() + " bài tập" : "");
            if (ready && assignments.isEmpty()) {
                showEmpty(hasCourses ? "Lớp này chưa có bài tập nào" : "Bạn chưa phụ trách lớp nào");
            }
            return;
        }

        // Chờ chấm và Đã chấm dùng chung danh sách bài nộp, chỉ khác bộ lọc và thứ tự
        boolean wantGraded = tab == TAB_GRADED;
        List<GradeRow> list = new ArrayList<>();
        for (GradeRow r : rows) if (r.isGraded() == wantGraded) list.add(r);
        Collections.sort(list, (x, y) -> wantGraded
                ? nz(y.getSubmission().getGradedAt()).compareTo(nz(x.getSubmission().getGradedAt()))   // chấm gần nhất trước
                : nz(x.getSubmission().getSubmittedAt()).compareTo(nz(y.getSubmission().getSubmittedAt()))); // nộp sớm nhất trước
        shown = list;
        submissionAdapter.submit(list);

        title.setText(wantGraded ? "Bài đã chấm" : "Bài đang chờ chấm");
        count.setText(ready ? list.size() + " bài" : "");
        if (ready && list.isEmpty()) {
            showEmpty(!hasCourses ? "Bạn chưa phụ trách lớp nào"
                    : wantGraded ? "Chưa có bài nào được chấm" : "Không có bài nào đang chờ chấm");
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Bài đang mở lên trước (hạn gần nhất trước), bài đã đóng xuống sau (mới nhất trước). */
    private static int compareAssignments(AssignmentItem x, AssignmentItem y) {
        boolean cx = x.isClosed();
        boolean cy = y.isClosed();
        if (cx != cy) return cx ? 1 : -1;
        String dx = nz(x.getDeadline());
        String dy = nz(y.getDeadline());
        return cx ? dy.compareTo(dx) : dx.compareTo(dy);
    }

    // ===================== Chọn lớp, mở màn khác =====================

    private void showCoursePicker() {
        if (courses.isEmpty()) return;
        String[] names = new String[courses.size()];
        for (int i = 0; i < names.length; i++) names[i] = courses.get(i).getCourseName();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn lớp học")
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == selected) return;
                    selected = which;
                    bindSelected();
                    loadClassData();
                })
                .show();
    }

    private void bindSelected() {
        View view = getView();
        if (view == null || selected < 0) return;
        text(view, R.id.tvClassName).setText(courses.get(selected).getCourseName());
    }

    /** Mở màn Chấm bài với danh sách đang hiện, bắt đầu từ bài được bấm. */
    private void openGrade(int position) {
        if (position < 0 || position >= shown.size()) return;
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer,
                        GradeSubmissionFragment.newInstance(shown, position, tab == TAB_PENDING))
                .addToBackStack(null)
                .commit();
    }

    private void openCreate() {
        if (courses.isEmpty() || selected < 0) {
            Toast.makeText(requireContext(), "Bạn chưa có lớp nào để giao bài", Toast.LENGTH_SHORT).show();
            return;
        }
        long[] ids = new long[courses.size()];
        String[] names = new String[courses.size()];
        for (int i = 0; i < ids.length; i++) {
            Long id = courses.get(i).getId();
            ids[i] = id == null ? -1 : id;
            names[i] = courses.get(i).getCourseName();
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, CreateAssignmentFragment.newInstance(ids, names, selected))
                .addToBackStack(null)
                .commit();
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