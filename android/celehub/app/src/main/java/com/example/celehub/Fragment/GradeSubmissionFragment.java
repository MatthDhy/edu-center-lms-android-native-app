package com.example.celehub.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.GradeBody;
import com.example.celehub.Model.GradeRow;
import com.example.celehub.Model.SubmissionItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;
import com.example.celehub.Util.HomeFormat;

/**
 * Giảng viên chấm một bài nộp.
 * queue = true (mở từ tab Chờ chấm): lưu xong tự chuyển sang bài kế tiếp trong danh sách.
 * queue = false (mở từ tab Đã chấm): lưu xong quay lại danh sách.
 */
public class GradeSubmissionFragment extends Fragment {

    private static final String ARG_ROWS = "rows";
    private static final String ARG_INDEX = "index";
    private static final String ARG_QUEUE = "queue";

    private List<GradeRow> rows;
    private int index;
    private boolean queue;
    private boolean saving;

    public static GradeSubmissionFragment newInstance(List<GradeRow> rows, int index, boolean queue) {
        GradeSubmissionFragment f = new GradeSubmissionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROWS, new Gson().toJson(rows));
        args.putInt(ARG_INDEX, index);
        args.putBoolean(ARG_QUEUE, queue);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grade_submission, container, false);

        Bundle args = requireArguments();
        Type type = new TypeToken<List<GradeRow>>() { }.getType();
        rows = new Gson().fromJson(args.getString(ARG_ROWS), type);
        index = args.getInt(ARG_INDEX);
        queue = args.getBoolean(ARG_QUEUE);

        ChildHeader.bind(this, view, "Chấm bài");

        if (rows == null || rows.isEmpty() || index < 0 || index >= rows.size()) {
            Toast.makeText(requireContext(), "Không có bài để chấm", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }

        view.findViewById(R.id.btnOpenFile).setOnClickListener(v ->
                FileUtils.open(requireContext(), current().getSubmission().getFileUrl()));
        view.findViewById(R.id.btnSave).setOnClickListener(v -> save());

        bindRow(view);
        return view;
    }

    private GradeRow current() {
        return rows.get(index);
    }

    // ===================== Hiển thị =====================

    private void bindRow(View v) {
        GradeRow row = current();
        SubmissionItem s = row.getSubmission();

        text(v, R.id.tvAvatar).setText(HomeFormat.initials(s.getStudentName()));
        text(v, R.id.tvStudent).setText(s.getStudentName() + " • " + HomeFormat.studentCode(s.getStudentId()));
        text(v, R.id.tvCourse).setText(row.getCourseName());
        text(v, R.id.tvAssignment).setText(row.getAssignmentTitle());
        text(v, R.id.tvSubmitted).setText("Đã nộp " + HomeFormat.dateTime(s.getSubmittedAt())
                + " • " + (s.isLate() ? "Nộp trễ" : "Đúng hạn"));

        TextView position = text(v, R.id.tvPosition);
        position.setVisibility(queue ? View.VISIBLE : View.GONE);
        position.setText("Bài " + (index + 1) + "/" + rows.size());

        String fileName = FileUtils.nameFromUrl(s.getFileUrl());
        text(v, R.id.tvFileName).setText(fileName.isEmpty() ? "Bài làm" : fileName);
        text(v, R.id.tvFileType).setText(FileUtils.typeLabel(fileName));

        text(v, R.id.tvScoreLabel).setText("Điểm (tối đa " + HomeFormat.score(row.getMaxScore()) + ")");
        text(v, R.id.tvScoreMax).setText("/ " + HomeFormat.score(row.getMaxScore()));

        EditText etScore = v.findViewById(R.id.etScore);
        etScore.setError(null);
        etScore.setText(s.getScore() == null ? "" : HomeFormat.score(s.getScore()));
        ((EditText) v.findViewById(R.id.etFeedback)).setText(s.getFeedback() == null ? "" : s.getFeedback());

        text(v, R.id.btnSave).setText(saveLabel());
    }

    private String saveLabel() {
        if (!queue) return "Lưu điểm";
        return index + 1 < rows.size() ? "Lưu & chấm bài tiếp" : "Lưu & hoàn tất";
    }

    // ===================== Lưu điểm =====================

    private void save() {
        View v = getView();
        if (v == null || saving) return;

        GradeRow row = current();
        EditText etScore = v.findViewById(R.id.etScore);
        String raw = etScore.getText().toString().trim().replace(',', '.');
        if (raw.isEmpty()) {
            etScore.setError("Vui lòng nhập điểm");
            etScore.requestFocus();
            return;
        }

        double score;
        try {
            score = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            etScore.setError("Điểm không hợp lệ");
            etScore.requestFocus();
            return;
        }
        if (score < 0 || score > row.getMaxScore()) {
            etScore.setError("Điểm phải từ 0 đến " + HomeFormat.score(row.getMaxScore()));
            etScore.requestFocus();
            return;
        }

        String feedback = ((EditText) v.findViewById(R.id.etFeedback)).getText().toString().trim();
        Long submissionId = row.getSubmission().getId();
        if (submissionId == null) return;

        setSaving(true);
        ApiClient.get(requireContext()).gradeSubmission(submissionId, new GradeBody(score, feedback))
                .enqueue(new Callback<ApiResponse<SubmissionItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<SubmissionItem>> call,
                                           Response<ApiResponse<SubmissionItem>> res) {
                        if (!isAdded()) return;
                        setSaving(false);
                        if (!res.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Lưu điểm thất bại"),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Cập nhật bản sao trong danh sách bằng kết quả server vừa trả về
                        if (res.body() != null && res.body().getData() != null) {
                            rows.set(index, new GradeRow(res.body().getData(), row.getAssignmentTitle(),
                                    row.getCourseName(), row.getMaxScore()));
                        }
                        Toast.makeText(requireContext(), "Đã lưu điểm", Toast.LENGTH_SHORT).show();

                        View view = getView();
                        if (queue && index + 1 < rows.size() && view != null) {
                            index++;
                            bindRow(view);
                            ((NestedScrollView) view.findViewById(R.id.scroll)).scrollTo(0, 0);
                        } else {
                            getParentFragmentManager().popBackStack();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<SubmissionItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        setSaving(false);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setSaving(boolean value) {
        saving = value;
        View v = getView();
        if (v == null) return;
        TextView button = text(v, R.id.btnSave);
        button.setText(value ? "Đang lưu..." : saveLabel());
        button.setEnabled(!value);
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}