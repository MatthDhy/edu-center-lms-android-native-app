package com.example.celehub.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.celehub.Data.SessionManager;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FormErrors;
import com.example.celehub.Util.SimpleTextWatcher;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileFragment extends Fragment {

    /**
     * Backend hiện chỉ có GET /api/users/me, CHƯA có API cập nhật họ tên / số điện thoại.
     * Nên tạm thời màn này chỉ XEM thông tin. Khi backend thêm API (đề xuất: PATCH /api/users/me
     * nhận { fullName, phone }) thì đổi thành true và viết nốt phần gọi API trong save().
     */
    private static final boolean UPDATE_SUPPORTED = false;

    private TextInputLayout tilName, tilPhone;
    private TextInputEditText etName, etPhone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        ChildHeader.bind(this, view, UPDATE_SUPPORTED ? "Sửa thông tin" : "Thông tin cá nhân");

        SessionManager session = SessionManager.get(requireContext());

        tilName = view.findViewById(R.id.tilName);
        tilPhone = view.findViewById(R.id.tilPhone);
        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etStudentCode = view.findViewById(R.id.etStudentCode);
        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        View btnSave = view.findViewById(R.id.btnSave);
        View tvChangeAvatar = view.findViewById(R.id.tvChangeAvatar);

        // Dữ liệu thật của người dùng đang đăng nhập
        tvInitials.setText(session.getInitials());
        etName.setText(session.getFullName());
        etPhone.setText(session.getPhone());
        etEmail.setText(session.getEmail());
        etStudentCode.setText(session.getDisplayCode());

        if (!UPDATE_SUPPORTED) {
            etName.setEnabled(false);
            etPhone.setEnabled(false);
            btnSave.setVisibility(View.GONE);
            tvChangeAvatar.setVisibility(View.GONE);
            return view;
        }

        etName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                FormErrors.clear(tilName);
            }
        });
        etPhone.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                FormErrors.clear(tilPhone);
            }
        });
        tvChangeAvatar.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Đổi ảnh đại diện", Toast.LENGTH_SHORT).show());
        btnSave.setOnClickListener(v -> save());

        return view;
    }

    private void save() {
        String name = textOf(etName).trim();
        String phone = textOf(etPhone).replace(" ", "");

        boolean valid = true;
        if (name.length() < 2) {
            tilName.setError("Vui lòng nhập họ và tên");
            valid = false;
        }
        // Số điện thoại Việt Nam: 10 chữ số, bắt đầu bằng 0
        if (!phone.matches("0\\d{9}")) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            valid = false;
        }
        if (!valid) return;

        // TODO: khi backend có API cập nhật hồ sơ, gọi ở đây (gửi name và phone),
        // thành công thì SessionManager.get(...).saveUser(userMoi) rồi popBackStack().
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}