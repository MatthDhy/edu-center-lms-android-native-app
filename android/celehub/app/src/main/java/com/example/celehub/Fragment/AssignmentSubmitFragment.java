package com.example.celehub.Fragment;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;
import com.example.celehub.Util.HomeFormat;
import com.example.celehub.Util.StatusChip;

/**
 * Học viên nộp bài. Ba trạng thái:
 *  - chưa nộp: chọn tệp rồi "Nộp bài";
 *  - đã nộp, chưa bấm sửa: chỉ xem, nút dưới là "Sửa bài nộp";
 *  - đang sửa: chọn lại tệp + "Nộp lại", hoặc "Xóa bài nộp" (có hộp thoại xác nhận).
 */
public class AssignmentSubmitFragment extends Fragment {

    private static final String ARG_ID = "assignment_id";
    private static final String ARG_EDIT = "start_editing";

    private long assignmentId;
    private AssignmentItem assignment;
    private FileUtils.Picked picked;
    private boolean submitting;          // đang gọi API nộp hoặc xóa
    private String busyLabel = "Đang xử lý...";
    private boolean editing;             // đã bấm "Sửa bài nộp"
    private boolean firstBind = true;

    // Phải đăng ký trước khi fragment được hiển thị nên khai báo ở đây
    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    public static AssignmentSubmitFragment newInstance(long assignmentId) {
        return newInstance(assignmentId, false);
    }

    /** startEditing = true: nếu đã nộp thì vào thẳng chế độ sửa (bỏ qua bước bấm "Sửa bài nộp"). */
    public static AssignmentSubmitFragment newInstance(long assignmentId, boolean startEditing) {
        AssignmentSubmitFragment f = new AssignmentSubmitFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, assignmentId);
        args.putBoolean(ARG_EDIT, startEditing);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assignment_submit, container, false);
        assignmentId = requireArguments().getLong(ARG_ID);

        ChildHeader.bind(this, view, "Nộp bài tập");

        view.findViewById(R.id.boxPick).setOnClickListener(v -> launchPicker());
        view.findViewById(R.id.btnPick).setOnClickListener(v -> launchPicker());
        view.findViewById(R.id.btnRemoveFile).setOnClickListener(v -> clearPicked());
        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> onPrimaryClick());
        view.findViewById(R.id.btnDeleteSubmission).setOnClickListener(v -> confirmDelete());

        loadAssignment();
        return view;
    }

    // ===================== Trạng thái =====================

    private boolean hasSubmission() {
        return assignment != null && assignment.getMySubmission() != null;
    }

    /** Bài đã chấm thì khóa hẳn, không nộp lại hay xóa được. */
    private boolean canChange() {
        return assignment != null && !assignment.isGraded();
    }

    private boolean uploadEnabled() {
        return canChange() && !submitting && (!hasSubmission() || editing);
    }

    /** Vẽ lại các nút theo trạng thái hiện tại. */
    private void render() {
        View v = getView();
        if (v == null || assignment == null) return;

        boolean canChange = canChange();
        boolean upload = uploadEnabled();

        v.findViewById(R.id.llUpload).setVisibility(canChange ? View.VISIBLE : View.GONE);
        View boxPick = v.findViewById(R.id.boxPick);
        boxPick.setEnabled(upload);
        boxPick.setAlpha(upload ? 1f : 0.5f);
        View btnPick = v.findViewById(R.id.btnPick);
        btnPick.setEnabled(upload);
        btnPick.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(
                upload ? R.color.color_primary : R.color.color_disable)));
        v.findViewById(R.id.btnRemoveFile).setEnabled(upload);

        View delete = v.findViewById(R.id.btnDeleteSubmission);
        delete.setVisibility(canChange && hasSubmission() && editing ? View.VISIBLE : View.GONE);
        delete.setEnabled(!submitting);

        TextView button = text(v, R.id.btnSubmit);
        button.setVisibility(canChange ? View.VISIBLE : View.GONE);

        String label;
        boolean enabled;
        if (submitting) {
            label = busyLabel;
            enabled = false;
        } else if (hasSubmission() && !editing) {
            label = "Sửa bài nộp";
            enabled = true;
        } else {
            label = hasSubmission() ? "Nộp lại" : "Nộp bài";
            enabled = picked != null;
        }
        button.setText(label);
        button.setEnabled(enabled);
        button.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(
                enabled ? R.color.color_primary : R.color.color_disable)));
    }

    private void setBusy(boolean value, String label) {
        submitting = value;
        if (label != null) busyLabel = label;
        render();
    }

    private void onPrimaryClick() {
        if (submitting || assignment == null) return;
        if (hasSubmission() && !editing) {   // "Sửa bài nộp": mở khóa chọn tệp và xóa
            editing = true;
            render();
            return;
        }
        submit();
    }

    // ===================== Tải bài tập =====================

    private void loadAssignment() {
        ApiClient.get(requireContext()).getAssignment(assignmentId)
                .enqueue(new Callback<ApiResponse<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                           Response<ApiResponse<AssignmentItem>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            bind(res.body().getData());
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Không tải được bài tập"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentItem>> call, Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void bind(AssignmentItem a) {
        View v = getView();
        if (v == null) return;
        assignment = a;

        if (firstBind) {
            editing = requireArguments().getBoolean(ARG_EDIT, false)
                    && a.getMySubmission() != null && !a.isGraded();
            firstBind = false;
        }

        ((TextView) v.findViewById(R.id.tvHeaderTitle))
                .setText(a.getMySubmission() != null ? "Bài nộp của tôi" : "Nộp bài tập");

        text(v, R.id.tvTitle).setText(a.getTitle());
        StatusChip.forAssignment(text(v, R.id.tvStatusChip), a);
        text(v, R.id.tvCourse).setText(a.getCourseName());
        text(v, R.id.tvDeadline).setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));

        v.findViewById(R.id.tvOverdueWarning).setVisibility(a.isOverdue() ? View.VISIBLE : View.GONE);
        bindBrief(v, a);
        bindMySubmission(v, a);
        render();
    }

    /** Mô tả và tệp đề bài do giảng viên đính kèm (nếu có). */
    private void bindBrief(View v, AssignmentItem a) {
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
    }

    /** Bài đã nộp trước đó, kèm điểm và nhận xét nếu đã được chấm. */
    private void bindMySubmission(View v, AssignmentItem a) {
        SubmissionItem s = a.getMySubmission();
        View card = v.findViewById(R.id.cardMySubmission);
        if (s == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        TextView file = text(v, R.id.tvMyFile);
        file.setText(FileUtils.nameFromUrl(s.getFileUrl()));
        file.setOnClickListener(x -> FileUtils.open(requireContext(), s.getFileUrl()));

        text(v, R.id.tvMySubmittedAt).setText("Nộp: " + HomeFormat.dateTime(s.getSubmittedAt())
                + (s.isLate() ? " • Nộp muộn" : ""));

        TextView score = text(v, R.id.tvScore);
        TextView feedback = text(v, R.id.tvFeedback);
        if (s.getScore() != null) {
            score.setText("Điểm: " + HomeFormat.score(s.getScore())
                    + "/" + HomeFormat.score(a.getMaxScoreOrDefault()));
            score.setVisibility(View.VISIBLE);
        } else {
            score.setVisibility(View.GONE);
        }
        boolean hasFeedback = s.getFeedback() != null && !s.getFeedback().trim().isEmpty();
        feedback.setVisibility(hasFeedback ? View.VISIBLE : View.GONE);
        if (hasFeedback) feedback.setText("Nhận xét: " + s.getFeedback());
    }

    // ===================== Chọn tệp =====================

    private void launchPicker() {
        if (!uploadEnabled()) return;
        picker.launch(FileUtils.SUBMISSION_MIME_TYPES);
    }

    private void onPicked(@Nullable Uri uri) {
        View v = getView();
        if (uri == null || !isAdded() || v == null) return;

        FileUtils.Picked p = FileUtils.query(requireContext().getContentResolver(), uri);

        // Kiểm tra trên app trước để báo lỗi ngay, khỏi chờ server từ chối
        String ext = FileUtils.extension(FileUtils.uploadName(p));
        if (!FileUtils.SUBMISSION_EXTENSIONS.contains(ext)) {
            showFileError("Tệp không hợp lệ. Chỉ nhận PDF, DOC, DOCX hoặc ảnh.");
            return;
        }
        if (p.size > FileUtils.MAX_UPLOAD_BYTES) {
            showFileError("Tệp vượt quá 20 MB. Vui lòng chọn tệp nhỏ hơn.");
            return;
        }

        picked = p;
        v.findViewById(R.id.tvFileError).setVisibility(View.GONE);
        v.findViewById(R.id.rowFile).setVisibility(View.VISIBLE);
        text(v, R.id.tvFileType).setText(FileUtils.typeLabel(p.name));
        text(v, R.id.tvFileName).setText(p.name);
        String size = FileUtils.readableSize(p.size);
        String type = FileUtils.typeLabel(p.name);
        text(v, R.id.tvFileMeta).setText(size.isEmpty() ? type : type + " • " + size);
        render();
    }

    private void clearPicked() {
        View v = getView();
        picked = null;
        if (v != null) v.findViewById(R.id.rowFile).setVisibility(View.GONE);
        render();
    }

    private void showFileError(String message) {
        View v = getView();
        if (v == null) return;
        TextView error = text(v, R.id.tvFileError);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    // ===================== Nộp bài =====================

    private void submit() {
        if (picked == null || assignment == null || submitting) return;
        setBusy(true, "Đang nộp...");

        MultipartBody.Part part = FileUtils.toPart(requireContext().getContentResolver(), picked, "file");
        ApiClient.get(requireContext()).submitAssignment(assignment.getId(), part)
                .enqueue(new Callback<ApiResponse<SubmissionItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SubmissionItem>> call,
                                           Response<ApiResponse<SubmissionItem>> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Nộp bài thành công",
                                    Toast.LENGTH_SHORT).show();
                            // Quay lại màn chi tiết, màn đó tải lại và hiện trạng thái mới
                            getParentFragmentManager().popBackStack();
                        } else {
                            setBusy(false, null);
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Nộp bài thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SubmissionItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        setBusy(false, null);   // giữ nguyên tệp đã chọn để thử lại
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ===================== Xóa bài nộp =====================

    private void confirmDelete() {
        if (submitting || !hasSubmission()) return;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa bài nộp?")
                .setMessage("Bài đã nộp sẽ bị xóa và không thể khôi phục. Bạn có thể nộp bài mới sau đó.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, which) -> deleteSubmission())
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(requireContext().getColor(R.color.color_danger));
    }

    private void deleteSubmission() {
        setBusy(true, "Đang xóa...");
        ApiClient.get(requireContext()).deleteMySubmission(assignmentId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đã xóa bài nộp",
                                    Toast.LENGTH_SHORT).show();
                            // Về trạng thái "chưa nộp": bỏ chế độ sửa, bỏ tệp đang chọn, tải lại bài tập
                            editing = false;
                            clearPicked();
                            setBusy(false, null);
                            loadAssignment();
                        } else {
                            setBusy(false, null);
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Xóa bài nộp thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (!isAdded()) return;
                        setBusy(false, null);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}