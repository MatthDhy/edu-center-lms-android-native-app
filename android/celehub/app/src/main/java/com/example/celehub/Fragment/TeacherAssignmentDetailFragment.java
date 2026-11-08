package com.example.celehub.Fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.GradeRow;
import com.example.celehub.Model.StudentBrief;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.Model.SubmissionsOverview;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;
import com.example.celehub.Util.HomeFormat;
import com.example.celehub.Util.StatusChip;

/**
 * Giảng viên xem một bài tập: ai đã nộp, ai chưa nộp, bấm vào bài nộp để chấm,
 * nhắc cả lớp, sửa hoặc xóa bài tập.
 */
public class TeacherAssignmentDetailFragment extends Fragment {

    private static final String ARG_ID = "assignment_id";

    private long assignmentId;
    private AssignmentItem assignment;
    private List<SubmissionItem> submitted = new ArrayList<>();
    private List<StudentBrief> notSubmitted = new ArrayList<>();
    private boolean showSubmitted = true;   // tab đang xem
    private boolean busy;                   // đang gọi API nhắc hoặc xóa

    public static TeacherAssignmentDetailFragment newInstance(long assignmentId) {
        TeacherAssignmentDetailFragment f = new TeacherAssignmentDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, assignmentId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_assignment_detail, container, false);
        assignmentId = requireArguments().getLong(ARG_ID);
        ChildHeader.bind(this, view, "Chi tiết bài tập");

        view.findViewById(R.id.tabSubmitted).setOnClickListener(v -> selectTab(true));
        view.findViewById(R.id.tabNotSubmitted).setOnClickListener(v -> selectTab(false));
        view.findViewById(R.id.btnRemindAll).setOnClickListener(v -> confirmRemind());
        view.findViewById(R.id.btnEdit).setOnClickListener(v -> openChild(EditAssignmentFragment.newInstance(assignmentId)));
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());

        styleTabs(view);
        // Quay lại từ màn Chấm bài / Sửa bài thì view được tạo lại nên dữ liệu luôn mới
        load();
        return view;
    }

    // ===================== Tải dữ liệu =====================

    private void load() {
        ApiClient.get(requireContext()).getSubmissions(assignmentId)
                .enqueue(new Callback<ApiResponse<SubmissionsOverview>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SubmissionsOverview>> call,
                                           Response<ApiResponse<SubmissionsOverview>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null
                                && res.body().getData().getAssignment() != null) {
                            bind(res.body().getData());
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được bài tập"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SubmissionsOverview>> call, Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bind(SubmissionsOverview o) {
        View v = getView();
        if (v == null) return;

        assignment = o.getAssignment();
        submitted = o.getSubmitted() != null ? o.getSubmitted() : new ArrayList<>();
        notSubmitted = o.getNotSubmitted() != null ? o.getNotSubmitted() : new ArrayList<>();

        AssignmentItem a = assignment;
        text(v, R.id.tvTitle).setText(a.getTitle());
        StatusChip.forTeacherAssignment(text(v, R.id.tvStatusChip), a);
        text(v, R.id.tvCourse).setText(a.getCourseName());
        text(v, R.id.tvDeadline).setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));

        int pending = 0;
        for (SubmissionItem s : submitted) if (s.getScore() == null) pending++;
        int total = submitted.size() + notSubmitted.size();
        text(v, R.id.tvStats).setText(submitted.size() + "/" + total + " đã nộp • " + pending
                + " chờ chấm • Điểm tối đa " + HomeFormat.score(a.getMaxScoreOrDefault()));

        // Mô tả và tệp đề bài
        boolean hasDesc = a.getDescription() != null && !a.getDescription().trim().isEmpty();
        boolean hasFile = a.getAttachmentUrl() != null && !a.getAttachmentUrl().isEmpty();
        v.findViewById(R.id.llBrief).setVisibility(hasDesc || hasFile ? View.VISIBLE : View.GONE);
        TextView desc = text(v, R.id.tvDescription);
        desc.setVisibility(hasDesc ? View.VISIBLE : View.GONE);
        if (hasDesc) desc.setText(a.getDescription());
        TextView file = text(v, R.id.tvAttachment);
        file.setVisibility(hasFile ? View.VISIBLE : View.GONE);
        if (hasFile) {
            file.setText("Tệp đề bài: " + FileUtils.nameFromUrl(a.getAttachmentUrl()));
            file.setOnClickListener(x -> FileUtils.open(requireContext(), a.getAttachmentUrl()));
        }

        styleTabs(v);
        renderList(v);
    }

    // ===================== Tab và danh sách =====================

    private void selectTab(boolean submittedTab) {
        showSubmitted = submittedTab;
        View v = getView();
        if (v == null) return;
        styleTabs(v);
        renderList(v);
    }

    private void styleTabs(View v) {
        styleTab(text(v, R.id.tabSubmitted), "Đã nộp (" + submitted.size() + ")", showSubmitted);
        styleTab(text(v, R.id.tabNotSubmitted), "Chưa nộp (" + notSubmitted.size() + ")", !showSubmitted);
    }

    private void styleTab(TextView tab, String label, boolean selected) {
        tab.setText(label);
        tab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(
                selected ? R.color.color_primary : R.color.color_primary_container)));
        tab.setTextColor(selected ? 0xFFFFFFFF : requireContext().getColor(R.color.color_text_secondary));
    }

    private void renderList(View v) {
        LinearLayout list = v.findViewById(R.id.llList);
        TextView empty = text(v, R.id.tvEmpty);
        View remind = v.findViewById(R.id.btnRemindAll);
        list.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        if (showSubmitted) {
            remind.setVisibility(View.GONE);
            empty.setText("Chưa có học viên nào nộp bài.");
            empty.setVisibility(submitted.isEmpty() ? View.VISIBLE : View.GONE);
            for (int i = 0; i < submitted.size(); i++) {
                final int index = i;
                SubmissionItem s = submitted.get(i);
                View row = inflater.inflate(R.layout.item_assignment_student, list, false);
                fillStudent(row, s.getStudentName());
                text(row, R.id.tvSub).setText("Nộp " + HomeFormat.shortDateTime(s.getSubmittedAt())
                        + (s.isLate() ? " • Nộp muộn" : ""));
                if (s.getScore() != null) {
                    chip(text(row, R.id.tvChip),
                            HomeFormat.score(s.getScore()) + "/" + HomeFormat.score(assignment.getMaxScoreOrDefault()),
                            R.color.color_success_container, R.color.color_success);
                } else {
                    chip(text(row, R.id.tvChip), "Chờ chấm",
                            R.color.color_warning_container, R.color.color_warning);
                }
                row.setOnClickListener(x -> openGrade(index));
                list.addView(row);
            }
        } else {
            remind.setVisibility(notSubmitted.isEmpty() ? View.GONE : View.VISIBLE);
            empty.setText("Tất cả học viên đã nộp bài.");
            empty.setVisibility(notSubmitted.isEmpty() ? View.VISIBLE : View.GONE);
            for (StudentBrief s : notSubmitted) {
                View row = inflater.inflate(R.layout.item_assignment_student, list, false);
                fillStudent(row, s.getFullName());
                text(row, R.id.tvSub).setText(s.getEmail() == null ? "" : s.getEmail());
                row.findViewById(R.id.tvChip).setVisibility(View.GONE);
                row.setClickable(false);
                list.addView(row);
            }
        }
    }

    private void fillStudent(View row, String fullName) {
        text(row, R.id.tvAvatar).setText(HomeFormat.initials(fullName));
        text(row, R.id.tvName).setText(fullName);
    }

    private void chip(TextView chip, String label, int bgColor, int textColor) {
        chip.setText(label);
        chip.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(bgColor)));
        chip.setTextColor(requireContext().getColor(textColor));
    }

    /** Mở màn Chấm bài với toàn bộ bài đã nộp, đứng ở bài vừa bấm. */
    private void openGrade(int index) {
        List<GradeRow> rows = new ArrayList<>();
        for (SubmissionItem s : submitted) {
            rows.add(new GradeRow(s, assignment.getTitle(), assignment.getCourseName(),
                    assignment.getMaxScoreOrDefault()));
        }
        openChild(GradeSubmissionFragment.newInstance(rows, index, false));
    }

    private void openChild(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ===================== Nhắc nộp bài =====================

    private void confirmRemind() {
        if (busy || notSubmitted.isEmpty()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nhắc nộp bài?")
                .setMessage("Gửi thông báo nhắc nộp bài tới " + notSubmitted.size() + " học viên chưa nộp.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi", (d, w) -> remindAll())
                .show();
    }

    private void remindAll() {
        busy = true;
        ApiClient.get(requireContext()).remindAll(assignmentId)
                .enqueue(new Callback<ApiResponse<Map<String, Integer>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Integer>>> call,
                                           Response<ApiResponse<Map<String, Integer>>> res) {
                        busy = false;
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Map<String, Integer> data = res.body() == null ? null : res.body().getData();
                            Integer n = data == null ? null : data.get("reminded");
                            Toast.makeText(requireContext(),
                                    n == null ? "Đã gửi nhắc nhở" : "Đã nhắc " + n + " học viên",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Gửi nhắc nhở thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Integer>>> call, Throwable t) {
                        busy = false;
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===================== Xóa bài tập =====================

    private void confirmDelete() {
        if (busy || assignment == null) return;
        String message = "Bài tập \"" + assignment.getTitle() + "\" sẽ bị xóa vĩnh viễn"
                + (submitted.isEmpty() ? "" : ", cùng " + submitted.size() + " bài nộp và tệp đính kèm")
                + ". Học viên sẽ không còn thấy bài này.";
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa bài tập?")
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> deleteAssignment())
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(requireContext().getColor(R.color.color_danger));
    }

    private void deleteAssignment() {
        busy = true;
        ApiClient.get(requireContext()).deleteAssignment(assignmentId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> res) {
                        busy = false;
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đã xóa bài tập", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();   // về danh sách, danh sách tải lại
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Xóa bài tập thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        busy = false;
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}