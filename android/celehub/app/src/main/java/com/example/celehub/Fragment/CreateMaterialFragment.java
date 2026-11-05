package com.example.celehub.Fragment;

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

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.MaterialItem;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FileUtils;

/** Giảng viên đăng tài liệu cho một lớp. */
public class CreateMaterialFragment extends Fragment {

    private static final String ARG_ID = "course_id";
    private static final String ARG_NAME = "course_name";

    private long courseId;
    private FileUtils.Picked picked;
    private boolean submitting;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPicked);

    public static CreateMaterialFragment newInstance(long courseId, String courseName) {
        CreateMaterialFragment f = new CreateMaterialFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, courseId);
        args.putString(ARG_NAME, courseName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_material, container, false);

        courseId = requireArguments().getLong(ARG_ID);
        text(view, R.id.tvClassName).setText(requireArguments().getString(ARG_NAME, ""));

        ChildHeader.bind(this, view, "Đăng tài liệu");

        view.findViewById(R.id.btnPickFile).setOnClickListener(v -> {
            if (!submitting) picker.launch(FileUtils.ALL_MIME_TYPES);
        });
        view.findViewById(R.id.btnRemoveFile).setOnClickListener(v -> clearPicked());
        view.findViewById(R.id.btnPost).setOnClickListener(v -> submit());
        return view;
    }

    // ===================== Chọn tệp =====================

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
        text(v, R.id.tvFileType).setText(FileUtils.typeLabel(p.name));
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

    // ===================== Đăng tài liệu =====================

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
        if (picked == null) {
            showFileError("Vui lòng chọn tệp tài liệu.");
            return;
        }

        String description = ((EditText) v.findViewById(R.id.etDescription)).getText().toString().trim();
        RequestBody titleBody = FileUtils.text(title);
        RequestBody descBody = description.isEmpty() ? null : FileUtils.text(description);
        MultipartBody.Part file = FileUtils.toPart(requireContext().getContentResolver(), picked, "file");

        setSubmitting(true);
        ApiClient.get(requireContext()).uploadMaterial(courseId, titleBody, descBody, file)
                .enqueue(new Callback<ApiResponse<MaterialItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<MaterialItem>> call,
                                           Response<ApiResponse<MaterialItem>> res) {
                        if (!isAdded()) return;
                        if (res.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đã đăng tài liệu", Toast.LENGTH_SHORT).show();
                            // Quay lại chi tiết lớp, màn đó tải lại và hiện tài liệu mới
                            getParentFragmentManager().popBackStack();
                        } else {
                            setSubmitting(false);
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Đăng tài liệu thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<MaterialItem>> call, Throwable t) {
                        if (!isAdded()) return;
                        setSubmitting(false);   // giữ nguyên nội dung đã nhập để thử lại
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        View v = getView();
        if (v == null) return;
        TextView button = text(v, R.id.btnPost);
        button.setText(value ? "Đang đăng..." : "Đăng");
        button.setEnabled(!value);
        v.findViewById(R.id.btnPickFile).setEnabled(!value);
        v.findViewById(R.id.btnRemoveFile).setEnabled(!value);
    }

    private TextView text(View root, int id) {
        return root.findViewById(id);
    }
}