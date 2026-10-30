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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;

import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.UpdateProfileRequest;
import com.example.celehub.Model.UserDto;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FormErrors;
import com.example.celehub.Util.SimpleTextWatcher;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileFragment extends Fragment {


    private static final boolean UPDATE_SUPPORTED = true;
    private View btnSave;
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
        btnSave = view.findViewById(R.id.btnSave);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etStudentCode = view.findViewById(R.id.etStudentCode);
        TextView tvInitials = view.findViewById(R.id.tvAvatarInitials);
        View tvChangeAvatar = view.findViewById(R.id.tvChangeAvatar);

        // Dữ liệu thật của người dùng đang đăng nhập
        tvInitials.setText(session.getInitials());
        etName.setText(session.getFullName());
        etPhone.setText(session.getPhone());
        etEmail.setText(session.getEmail());
        etStudentCode.setText(session.getDisplayCode());
        ((TextView) view.findViewById(R.id.tvCodeLabel)).setText(session.getCodeLabel());

        if (!UPDATE_SUPPORTED) {
            etName.setEnabled(false);
            etPhone.setEnabled(false);
            btnSave.setVisibility(View.VISIBLE);
            tvChangeAvatar.setVisibility(View.VISIBLE);
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
        // Số điện thoại không bắt buộc, nhưng nếu nhập thì phải đủ 10 chữ số bắt đầu bằng 0
        if (!phone.isEmpty() && !phone.matches("0\\d{9}")) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            valid = false;
        }
        if (!valid) return;

        btnSave.setEnabled(false);
        ApiClient.get(requireContext()).updateProfile(new UpdateProfileRequest(name, phone))
                .enqueue(new Callback<ApiResponse<UserDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserDto>> call,
                                           Response<ApiResponse<UserDto>> res) {
                        if (!isAdded()) return;
                        btnSave.setEnabled(true);
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            SessionManager.get(requireContext()).saveUser(res.body().getData());
                            Toast.makeText(requireContext(), "Đã cập nhật thông tin",
                                    Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            Toast.makeText(requireContext(),
                                    ApiErrors.messageOf(res, "Cập nhật thất bại"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserDto>> call, Throwable t) {
                        if (!isAdded()) return;
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}