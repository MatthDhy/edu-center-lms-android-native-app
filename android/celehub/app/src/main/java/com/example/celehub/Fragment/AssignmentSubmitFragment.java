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
import androidx.fragment.app.Fragment;

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

/** Học viên xem đề bài và nộp bài làm (hoặc nộp lại nếu chưa được chấm). */
public class AssignmentSubmitFragment extends Fragment {

    private static final String ARG_ID = "assignment_id";

    private long assignmentId;
    private AssignmentItem assignment;
    private FileUtils.Picked picked;
    private boolean submitting;

    // Phải đăng ký trước khi fragment được hiển thị nên khai báo ở đây
    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    public static AssignmentSubmitFragment newInstance(long assignmentId) {
        AssignmentSubmitFragment f = new AssignmentSubmitFragment();
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
        View view = inflater.inflate(R.layout.fragment_assignment_submit, container, false);
        assignmentId = requireArguments().getLong(ARG_ID);

        ChildHeader.bind(this, view, "Nộp bài tập");

        view.findViewById(R.id.boxPick).setOnClickListener(v -> launchPicker());
        view.findViewById(R.id.btnPick).setOnClickListener(v -> launchPicker());
        view.findViewById(R.id.btnRemoveFile).setOnClickListener(v -> clearPicked());
        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> submit());

        loadAssignment();
        return view;
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

        text(v, R.id.tvTitle).setText(a.getTitle());
        StatusChip.forAssignment(text(v, R.id.tvStatusChip), a);
        text(v, R.id.tvCourse).setText(a.getCourseName());
        text(v, R.id.tvDeadline).setText("Hạn nộp: " + HomeFormat.dateTime(a.getDeadline()));

        v.findViewById(R.id.tvOverdueWarning).setVisibility(a.isOverdue() ? View.VISIBLE : View.GONE);
        bindBrief(v, a);
        bindMySubmission(v, a);

        // Đã chấm thì chỉ xem, không nộp lại nữa
        boolean canSubmit = !a.isGraded();
        v.findViewById(R.id.llUpload).setVisibility(canSubmit ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnSubmit).setVisibility(canSubmit ? View.VISIBLE : View.GONE);
        ((TextView) v.findViewById(R.id.btnSubmit)).setText(a.isSubmitted() ? "Nộp lại" : "Nộp bài");
        refreshSubmitButton();
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
        if (submitting) return;
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
        refreshSubmitButton();
    }

    private void clearPicked() {
        View v = getView();
        picked = null;
        if (v != null) v.findViewById(R.id.rowFile).setVisibility(View.GONE);
        refreshSubmitButton();
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
        setSubmitting(true);

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
                            // Quay lại danh sách, danh sách sẽ tải lại và hiện trạng thái mới
                            getParentFragmentManager().popBackStack();
                        } else {
                            setSubmitting(false);
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Nộp bài thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SubmissionItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        setSubmitting(false);   // giữ nguyên tệp đã chọn để thử lại
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        View v = getView();
        if (v == null) return;
        TextView button = text(v, R.id.btnSubmit);
        button.setText(value ? "Đang nộp..."
                : (assignment != null && assignment.isSubmitted() ? "Nộp lại" : "Nộp bài"));
        v.findViewById(R.id.boxPick).setEnabled(!value);
        v.findViewById(R.id.btnPick).setEnabled(!value);
        v.findViewById(R.id.btnRemoveFile).setEnabled(!value);
        refreshSubmitButton();
    }

    /** Nút Nộp bài chỉ bật khi đã có tệp và không đang gửi. */
    private void refreshSubmitButton() {
        View v = getView();
        if (v == null) return;
        v.findViewById(R.id.btnSubmit).setEnabled(picked != null && !submitting);
        v.findViewById(R.id.btnSubmit).setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(picked != null && !submitting
                ? R.color.color_primary : R.color.color_disable)));
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}