package com.example.celehub.Fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
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

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AssignmentItem;
import com.example.celehub.Model.UpdateAssignmentBody;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;
import com.example.celehub.Util.HomeFormat;

/**
 * Giảng viên sửa tiêu đề, mô tả, điểm tối đa, hạn nộp và tệp đề bài của một bài tập.
 * Mọi thay đổi (kể cả thay/xóa tệp) chỉ được gửi khi bấm "Lưu thay đổi".
 */
public class EditAssignmentFragment extends Fragment {

    private static final String ARG_ID = "assignment_id";

    private long assignmentId;
    private Calendar deadline = Calendar.getInstance();
    private boolean deadlineChanged;   // chỉ gửi hạn nộp khi giảng viên thật sự đổi
    private boolean loaded;
    private boolean saving;

    // Tệp đề bài
    private String currentUrl;               // tệp đang có trên server, null nếu chưa có
    private FileUtils.Picked picked;         // tệp mới vừa chọn, sẽ thay tệp cũ khi lưu
    private boolean removeCurrent;           // sẽ xóa tệp hiện tại khi lưu

    // Phải đăng ký trước khi fragment được hiển thị nên khai báo ở đây
    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    public static EditAssignmentFragment newInstance(long assignmentId) {
        EditAssignmentFragment f = new EditAssignmentFragment();
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
        View view = inflater.inflate(R.layout.fragment_edit_assignment, container, false);
        assignmentId = requireArguments().getLong(ARG_ID);
        ChildHeader.bind(this, view, "Sửa bài tập");

        view.findViewById(R.id.boxDate).setOnClickListener(v -> pickDate());
        view.findViewById(R.id.boxTime).setOnClickListener(v -> pickTime());
        view.findViewById(R.id.btnPickFile).setOnClickListener(v -> pickFile());
        view.findViewById(R.id.btnFileAction).setOnClickListener(v -> onFileAction());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> save());

        renderFile();
        load();
        return view;
    }

    // ===================== Tải giá trị hiện tại =====================

    private void load() {
        ApiClient.get(requireContext()).getAssignment(assignmentId)
                .enqueue(new Callback<ApiResponse<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                           Response<ApiResponse<AssignmentItem>> res) {
                        if (!isAdded() || getView() == null) return;
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            fill(res.body().getData());
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

    private void fill(AssignmentItem a) {
        View v = getView();
        if (v == null) return;
        ((EditText) v.findViewById(R.id.etTitle)).setText(a.getTitle());
        ((EditText) v.findViewById(R.id.etDescription)).setText(a.getDescription() == null ? "" : a.getDescription());
        ((EditText) v.findViewById(R.id.etMaxScore)).setText(HomeFormat.score(a.getMaxScoreOrDefault()));

        Date d = HomeFormat.parse(a.getDeadline());
        if (d != null) deadline.setTime(d);
        showDeadline(v);

        String url = a.getAttachmentUrl();
        currentUrl = url == null || url.isEmpty() ? null : url;

        loaded = true;
        renderFile();
        refreshSaveButton();
    }

    // ===================== Hạn nộp =====================

    private void showDeadline(View v) {
        ((TextView) v.findViewById(R.id.tvDate)).setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(deadline.getTime()));
        ((TextView) v.findViewById(R.id.tvTime)).setText(
                new SimpleDateFormat("HH:mm", Locale.US).format(deadline.getTime()));
    }

    private void pickDate() {
        if (!loaded || saving) return;
        new DatePickerDialog(requireContext(), (picker, y, m, d) -> {
            deadline.set(y, m, d);
            deadlineChanged = true;
            View v = getView();
            if (v != null) showDeadline(v);
        }, deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        if (!loaded || saving) return;
        new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
            deadline.set(Calendar.HOUR_OF_DAY, hour);
            deadline.set(Calendar.MINUTE, minute);
            deadline.set(Calendar.SECOND, 0);
            deadlineChanged = true;
            View v = getView();
            if (v != null) showDeadline(v);
        }, deadline.get(Calendar.HOUR_OF_DAY), deadline.get(Calendar.MINUTE), true).show();
    }

    // ===================== Tệp đề bài =====================

    private void pickFile() {
        if (!loaded || saving) return;
        picker.launch(FileUtils.ALL_MIME_TYPES);
    }

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
        removeCurrent = false;   // đã có tệp mới thì tệp cũ tự bị thay, không cần xóa riêng
        v.findViewById(R.id.tvFileError).setVisibility(View.GONE);
        renderFile();
    }

    /** Nút phụ đổi theo trạng thái: Bỏ chọn / Hoàn tác / Xóa tệp. */
    private void onFileAction() {
        if (!loaded || saving) return;
        if (picked != null) {
            picked = null;                          // bỏ tệp mới vừa chọn
        } else if (removeCurrent) {
            removeCurrent = false;                  // hoàn tác việc xóa
        } else if (currentUrl != null) {
            removeCurrent = true;                   // đánh dấu xóa tệp hiện tại
        }
        View v = getView();
        if (v != null) v.findViewById(R.id.tvFileError).setVisibility(View.GONE);
        renderFile();
    }

    private void showFileError(String message) {
        View v = getView();
        if (v == null) return;
        TextView error = v.findViewById(R.id.tvFileError);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    /** Vẽ khối "Tệp đề bài" theo 4 trạng thái: có tệp, sẽ xóa, đã chọn tệp mới, chưa có tệp. */
    private void renderFile() {
        View v = getView();
        if (v == null) return;

        TextView state = v.findViewById(R.id.tvFileState);
        MaterialButton pick = v.findViewById(R.id.btnPickFile);
        MaterialButton action = v.findViewById(R.id.btnFileAction);

        boolean hasCurrent = currentUrl != null;
        String currentName = hasCurrent ? FileUtils.nameFromUrl(currentUrl) : "";
        boolean danger = false;

        state.setOnClickListener(null);
        state.setClickable(false);

        if (picked != null) {
            String size = FileUtils.readableSize(picked.size);
            String type = FileUtils.typeLabel(picked.name);
            state.setText("Tệp mới: " + picked.name + " (" + (size.isEmpty() ? type : type + " • " + size) + ")"
                    + (hasCurrent ? "\nTệp cũ sẽ bị thay thế khi lưu." : ""));
            pick.setText("Chọn tệp khác");
            action.setText("Bỏ chọn");
            action.setVisibility(View.VISIBLE);
        } else if (hasCurrent && removeCurrent) {
            state.setText("Tệp \"" + currentName + "\" sẽ bị xóa khi lưu.");
            pick.setText("Chọn tệp mới");
            action.setText("Hoàn tác");
            action.setVisibility(View.VISIBLE);
        } else if (hasCurrent) {
            state.setText("Hiện tại: " + currentName);
            state.setOnClickListener(x -> FileUtils.open(requireContext(), currentUrl));   // bấm để xem tệp
            pick.setText("Thay tệp");
            action.setText("Xóa tệp");
            action.setVisibility(View.VISIBLE);
            danger = true;
        } else {
            state.setText("Chưa có tệp đề bài");
            pick.setText("Chọn tệp");
            action.setVisibility(View.GONE);
        }
        action.setTextColor(requireContext().getColor(danger ? R.color.color_danger : R.color.color_primary));
        pick.setEnabled(loaded && !saving);
        action.setEnabled(loaded && !saving);
    }

    // ===================== Lưu =====================

    private void save() {
        View v = getView();
        if (v == null || !loaded || saving) return;

        EditText etTitle = v.findViewById(R.id.etTitle);
        EditText etMax = v.findViewById(R.id.etMaxScore);
        String title = etTitle.getText().toString().trim();
        String desc = ((EditText) v.findViewById(R.id.etDescription)).getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }
        double max;
        try {
            max = Double.parseDouble(etMax.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            etMax.setError("Điểm tối đa không hợp lệ");
            return;
        }
        if (max <= 0 || max > 999.99) {
            etMax.setError("Điểm tối đa phải lớn hơn 0 và nhỏ hơn 1000");
            return;
        }
        if (deadlineChanged && !deadline.after(Calendar.getInstance())) {
            Toast.makeText(requireContext(), "Hạn nộp mới phải sau thời điểm hiện tại",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String iso = deadlineChanged
                ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(deadline.getTime())
                : null;
        UpdateAssignmentBody body = new UpdateAssignmentBody(title, desc, iso, max);

        saving = true;
        refreshSaveButton();
        renderFile();

        // Bước 1: lưu thông tin. Bước 2 (nếu có): thay hoặc xóa tệp đề bài.
        ApiClient.get(requireContext()).updateAssignment(assignmentId, body)
                .enqueue(new Callback<ApiResponse<AssignmentItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                           Response<ApiResponse<AssignmentItem>> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            applyAttachment();
                        } else {
                            endSaving();
                            // Ví dụ: hạ điểm tối đa thấp hơn điểm đã chấm -> backend trả lỗi 409 kèm lý do
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Cập nhật thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AssignmentItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        endSaving();
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Thay, xóa hoặc giữ nguyên tệp đề bài, rồi kết thúc. */
    private void applyAttachment() {
        Call<ApiResponse<AssignmentItem>> call;
        if (picked != null) {
            MultipartBody.Part part = FileUtils.toPart(requireContext().getContentResolver(), picked, "file");
            call = ApiClient.get(requireContext()).replaceAssignmentAttachment(assignmentId, part);
        } else if (removeCurrent && currentUrl != null) {
            call = ApiClient.get(requireContext()).removeAssignmentAttachment(assignmentId);
        } else {
            finishSaved();
            return;
        }

        call.enqueue(new Callback<ApiResponse<AssignmentItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<AssignmentItem>> call,
                                   Response<ApiResponse<AssignmentItem>> res) {
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    finishSaved();
                } else {
                    endSaving();
                    Toast.makeText(requireContext(),
                            "Đã lưu thông tin nhưng chưa cập nhật được tệp: "
                                    + ApiErrors.messageOf(res, "lỗi không xác định"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AssignmentItem>> call, Throwable t) {
                if (!isAdded()) return;
                endSaving();
                Toast.makeText(requireContext(),
                        "Đã lưu thông tin nhưng chưa cập nhật được tệp. " + ApiErrors.NETWORK_ERROR,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finishSaved() {
        Toast.makeText(requireContext(), "Đã cập nhật bài tập", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();   // về màn chi tiết, màn đó tải lại
    }

    private void endSaving() {
        saving = false;
        refreshSaveButton();
        renderFile();
    }

    private void refreshSaveButton() {
        View v = getView();
        if (v == null) return;
        TextView button = v.findViewById(R.id.btnSave);
        button.setEnabled(loaded && !saving);
        button.setText(saving ? "Đang lưu..." : "Lưu thay đổi");
    }
}