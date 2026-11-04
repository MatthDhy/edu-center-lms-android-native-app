package com.example.celehub.Fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;

/** Giảng viên giao bài tập mới cho một lớp. */
public class CreateAssignmentFragment extends Fragment {

    private static final String ARG_IDS = "course_ids";
    private static final String ARG_NAMES = "course_names";
    private static final String ARG_SELECTED = "selected";

    private long[] courseIds;
    private String[] courseNames;
    private int selected;

    private Calendar deadline;          // hạn nộp, mặc định ngày mai 23:59
    private FileUtils.Picked picked;    // tệp đề bài (không bắt buộc)
    private boolean submitting;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    /**
     * Một lớp (mở từ chi tiết lớp) thì khóa ô chọn lớp.
     * Nhiều lớp (mở từ tab Bài tập) thì cho đổi lớp.
     */
    public static CreateAssignmentFragment newInstance(long[] ids, String[] names, int selected) {
        CreateAssignmentFragment f = new CreateAssignmentFragment();
        Bundle args = new Bundle();
        args.putLongArray(ARG_IDS, ids);
        args.putStringArray(ARG_NAMES, names);
        args.putInt(ARG_SELECTED, selected);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_assignment, container, false);

        Bundle args = requireArguments();
        courseIds = args.getLongArray(ARG_IDS);
        courseNames = args.getStringArray(ARG_NAMES);
        selected = args.getInt(ARG_SELECTED, 0);

        ChildHeader.bind(this, view, "Tạo bài tập");

        // Hạn mặc định: ngày mai lúc 23:59
        deadline = Calendar.getInstance();
        deadline.add(Calendar.DAY_OF_MONTH, 1);
        deadline.set(Calendar.HOUR_OF_DAY, 23);
        deadline.set(Calendar.MINUTE, 59);
        deadline.set(Calendar.SECOND, 0);
        deadline.set(Calendar.MILLISECOND, 0);

        showClass(view);
        showDeadline(view);

        if (courseIds != null && courseIds.length > 1) {
            view.findViewById(R.id.ivClassChevron).setVisibility(View.VISIBLE);
            view.findViewById(R.id.boxClass).setOnClickListener(v -> pickClass());
        }
        view.findViewById(R.id.boxDate).setOnClickListener(v -> pickDate());
        view.findViewById(R.id.boxTime).setOnClickListener(v -> pickTime());
        view.findViewById(R.id.btnPickFile).setOnClickListener(v -> {
            if (!submitting) picker.launch(FileUtils.ALL_MIME_TYPES);
        });
        view.findViewById(R.id.btnRemoveFile).setOnClickListener(v -> clearPicked());
        view.findViewById(R.id.btnCreate).setOnClickListener(v -> submit());
        return view;
    }

    // ===================== Lớp, ngày giờ =====================

    private void showClass(View view) {
        String name = courseNames != null && selected < courseNames.length ? courseNames[selected] : "";
        text(view, R.id.tvClassName).setText(name);
    }

    private void pickClass() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn lớp học")
                .setSingleChoiceItems(courseNames, selected, (dialog, which) -> {
                    dialog.dismiss();
                    selected = which;
                    View v = getView();
                    if (v != null) showClass(v);
                })
                .show();
    }

    private void showDeadline(View view) {
        text(view, R.id.tvDate).setText(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(deadline.getTime()));
        text(view, R.id.tvTime).setText(new SimpleDateFormat("HH:mm", Locale.US).format(deadline.getTime()));
    }

    private void pickDate() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, y, m, d) -> {
            deadline.set(y, m, d);
            View v = getView();
            if (v != null) showDeadline(v);
        }, deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);   // không chọn ngày đã qua
        dialog.show();
    }

    private void pickTime() {
        new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
            deadline.set(Calendar.HOUR_OF_DAY, hour);
            deadline.set(Calendar.MINUTE, minute);
            View v = getView();
            if (v != null) showDeadline(v);
        }, deadline.get(Calendar.HOUR_OF_DAY), deadline.get(Calendar.MINUTE), true).show();
    }

    // ===================== Tệp đề bài =====================

    private void onPicked(@Nullable Uri uri) {
        View v = getView();
        if (uri == null || !isAdded() || v == null) return;

        FileUtils.Picked p = FileUtils.query(requireContext().getContentResolver(), uri);
        String ext = FileUtils.extension(FileUtils.uploadName(p));
        if (!FileUtils.DOCUMENT_EXTENSIONS.contains(ext)) {
            showFileError("Loại tệp này chưa được hỗ trợ.");
            return;
        }
        if (p.size > FileUtils.MAX_UPLOAD_BYTES) {
            showFileError("Tệp vượt quá 20 MB. Vui lòng chọn tệp nhỏ hơn.");
            return;
        }

        picked = p;
        v.findViewById(R.id.tvFileError).setVisibility(View.GONE);
        v.findViewById(R.id.rowFile).setVisibility(View.VISIBLE);
        text(v, R.id.tvFileName).setText(p.name);
        String size = FileUtils.readableSize(p.size);
        String type = FileUtils.typeLabel(p.name);
        text(v, R.id.tvFileMeta).setText(size.isEmpty() ? type : type + " • " + size);
    }

    private void clearPicked() {
        if (submitting) return;
        picked = null;
        View v = getView();
        if (v != null) v.findViewById(R.id.rowFile).setVisibility(View.GONE);
    }

    private void showFileError(String message) {
        View v = getView();
        if (v == null) return;
        TextView error = text(v, R.id.tvFileError);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    // ===================== Giao bài =====================

    private void submit() {
        View v = getView();
        if (v == null || submitting) return;

        EditText etTitle = v.findViewById(R.id.etTitle);
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            etTitle.requestFocus();
            return;
        }
        if (!deadline.after(Calendar.getInstance())) {
            Toast.makeText(requireContext(), "Hạn nộp phải ở sau thời điểm hiện tại",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (courseIds == null || courseIds[selected] < 0) {
            Toast.makeText(requireContext(), "Không xác định được lớp học", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = ((EditText) v.findViewById(R.id.etDescription)).getText().toString().trim();
        // Gửi đúng giờ trên màn hình, không đổi múi giờ, vì server lưu giờ theo kiểu "2026-10-16T23:59:00"
        String iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(deadline.getTime());

        RequestBody titleBody = FileUtils.text(title);
        RequestBody descBody = description.isEmpty() ? null : FileUtils.text(description);
        RequestBody deadlineBody = FileUtils.text(iso);
        MultipartBody.Part file = picked == null ? null
                : FileUtils.toPart(requireContext().getContentResolver(), picked, "file");

        setSubmitting(true);
        ApiClient.get(requireContext())
                .createAssignment(courseIds[selected], titleBody, descBody, deadlineBody, file)
                .enqueue(new Callback<ApiResponse<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                           Response<ApiResponse<AssignmentItem>> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đã giao bài tập", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            setSubmitting(false);
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Giao bài thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        setSubmitting(false);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        View v = getView();
        if (v == null) return;
        TextView button = text(v, R.id.btnCreate);
        button.setText(value ? "Đang giao bài..." : "Giao bài");
        button.setEnabled(!value);
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}