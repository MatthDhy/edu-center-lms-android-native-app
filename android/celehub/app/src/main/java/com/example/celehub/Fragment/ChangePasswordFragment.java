package com.example.celehub.Fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.ChangePasswordRequest;
import com.example.celehub.R;
import com.example.celehub.Util.ChildHeader;
import com.example.celehub.Util.FormErrors;
import com.example.celehub.Util.PasswordRules;
import com.example.celehub.Util.SimpleTextWatcher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordFragment extends Fragment {

    // Chữ + màu cho từng mức độ mạnh (chỉ số = số tiêu chí đạt, 0 đến 4)
    private static final String[] STRENGTH_LABELS = {"", "Yếu", "Trung bình", "Khá", "Mạnh"};
    private static final String[] STRENGTH_COLORS =
            {"#E5E7EB", "#EF4444", "#7B61F0", "#2563EB", "#16A34A"};

    private TextInputLayout tilCurrent, tilNew, tilConfirm;
    private TextInputEditText etCurrent, etNew, etConfirm;
    private MaterialButton btnSave;
    private final View[] segments = new View[4];
    private TextView tvStrength, tvReqLength, tvReqUpper, tvReqLower;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);
        ChildHeader.bind(this, view, "Đổi mật khẩu");

        tilCurrent = view.findViewById(R.id.tilCurrent);
        tilNew = view.findViewById(R.id.tilNew);
        tilConfirm = view.findViewById(R.id.tilConfirm);
        etCurrent = view.findViewById(R.id.etCurrent);
        etNew = view.findViewById(R.id.etNew);
        etConfirm = view.findViewById(R.id.etConfirm);
        btnSave = view.findViewById(R.id.btnSave);
        segments[0] = view.findViewById(R.id.seg1);
        segments[1] = view.findViewById(R.id.seg2);
        segments[2] = view.findViewById(R.id.seg3);
        segments[3] = view.findViewById(R.id.seg4);
        tvStrength = view.findViewById(R.id.tvStrength);
        tvReqLength = view.findViewById(R.id.tvReqLength);
        tvReqUpper = view.findViewById(R.id.tvReqUpper);
        tvReqLower = view.findViewById(R.id.tvReqLower);

        etCurrent.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                FormErrors.clear(tilCurrent);
            }
        });
        etNew.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                updateStrength(text);
                updateConfirmState();
                FormErrors.clear(tilNew);
                FormErrors.clear(tilConfirm); // mật khẩu mới đổi thì lỗi "không khớp" cũ không còn đúng
            }
        });
        etConfirm.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                updateConfirmState();
                FormErrors.clear(tilConfirm);
            }
        });

        updateStrength("");
        btnSave.setOnClickListener(v -> save());
        return view;
    }

    // ------------------------------------------------------------------
    // Độ mạnh mật khẩu + yêu cầu
    // ------------------------------------------------------------------

    private void updateStrength(String password) {
        boolean hasLength = PasswordRules.hasMinLength(password);
        boolean hasUpper = PasswordRules.hasUpper(password);
        boolean hasLower = PasswordRules.hasLower(password);
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
                break;
            }
        }

        setRequirement(tvReqLength, "Ít nhất " + PasswordRules.MIN_LENGTH + " ký tự", hasLength);
        setRequirement(tvReqUpper, "Có chữ hoa", hasUpper);
        setRequirement(tvReqLower, "Có chữ thường", hasLower);

        int score = 0;
        if (!password.isEmpty()) {
            if (hasLength) score++;
            if (hasUpper) score++;
            if (hasLower) score++;
            if (hasDigit) score++;
            if (score == 0) score = 1;
        }

        int activeColor = Color.parseColor(STRENGTH_COLORS[score]);
        int emptyColor = Color.parseColor(STRENGTH_COLORS[0]);
        for (int i = 0; i < segments.length; i++) {
            ViewCompat.setBackgroundTintList(segments[i],
                    ColorStateList.valueOf(i < score ? activeColor : emptyColor));
        }

        tvStrength.setVisibility(score == 0 ? View.INVISIBLE : View.VISIBLE);
        tvStrength.setText(STRENGTH_LABELS[score]);
        tvStrength.setTextColor(activeColor);
    }

    private void setRequirement(TextView tv, String label, boolean satisfied) {
        tv.setText((satisfied ? "✓ " : "• ") + label);
        tv.setTextColor(Color.parseColor(satisfied ? "#15803D" : "#92400E"));
    }

    private void updateConfirmState() {
        String newPass = textOf(etNew);
        String confirm = textOf(etConfirm);
        boolean matched = !confirm.isEmpty() && confirm.equals(newPass);
        tilConfirm.setBoxBackgroundColor(Color.parseColor(matched ? "#E6F4EA" : "#F1EDFB"));
    }

    // ------------------------------------------------------------------
    // Lưu: gọi POST /api/auth/change-password
    // ------------------------------------------------------------------

    private void save() {
        String current = textOf(etCurrent);
        String newPass = textOf(etNew);
        String confirm = textOf(etConfirm);

        boolean valid = true;

        if (current.isEmpty()) {
            tilCurrent.setError("Vui lòng nhập mật khẩu hiện tại");
            valid = false;
        }

        String ruleError = PasswordRules.errorOf(newPass);
        if (ruleError != null) {
            tilNew.setError(ruleError);
            valid = false;
        } else if (newPass.equals(current)) {
            tilNew.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            valid = false;
        }

        if (!newPass.equals(confirm)) {
            tilConfirm.setError("Mật khẩu xác nhận không khớp");
            valid = false;
        }

        if (!valid) return;

        setSaving(true);

        ApiClient.get(requireContext())
                .changePassword(new ChangePasswordRequest(current, newPass))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        if (!isAdded()) return;
                        setSaving(false);

                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công",
                                    Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        } else {
                            String message = ApiErrors.messageOf(response, "Đổi mật khẩu thất bại");
                            if (response.code() == 400) {
                                if (message.contains("hiện tại không đúng")) {
                                    tilCurrent.setError(message);   // sai mật khẩu hiện tại
                                } else {
                                    tilNew.setError(message);       // luật mật khẩu mới, hoặc trùng mật khẩu cũ
                                }
                            } else {
                                Toast.makeText(requireContext(), message,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (!isAdded()) return;
                        setSaving(false);
                        Toast.makeText(requireContext(), ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Đang gọi server: khóa nút để không bấm nhiều lần. */
    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        btnSave.setText(saving ? "Đang lưu..." : "Lưu mật khẩu mới");
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}