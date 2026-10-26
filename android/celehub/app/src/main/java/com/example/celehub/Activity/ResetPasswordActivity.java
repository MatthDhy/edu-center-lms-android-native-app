package com.example.celehub.Activity;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Data.SessionManager;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.ForceChangePasswordRequest;
import com.example.celehub.R;
import com.example.celehub.Util.FormErrors;
import com.example.celehub.Util.PasswordRules;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn đặt mật khẩu mới, dùng cho 2 trường hợp:
 *  - MODE_FIRST_LOGIN: học viên vừa đăng nhập bằng mật khẩu tạm do admin cấp, bắt buộc đổi.
 *  - MODE_FORGOT (mặc định): luồng Quên mật khẩu sau khi xác thực OTP (backend chưa có API).
 */
public class ResetPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_FIRST_LOGIN = "first_login";
    public static final String MODE_FORGOT = "forgot";

    private TextInputLayout tilNew, tilConfirm;
    private TextInputEditText etNew, etConfirm;
    private MaterialButton btnReset;
    private boolean firstLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        firstLogin = MODE_FIRST_LOGIN.equals(getIntent().getStringExtra(EXTRA_MODE));

        tilNew = findViewById(R.id.tilNewPassword);
        tilConfirm = findViewById(R.id.tilConfirmPassword);
        etNew = findViewById(R.id.etNewPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);
        btnReset = findViewById(R.id.btnReset);

        // Dòng gợi ý luật mật khẩu ngay dưới ô nhập (tự ẩn khi có lỗi, hiện lại khi hết lỗi)
        tilNew.setHelperText(PasswordRules.HINT);

        etNew.addTextChangedListener(new ClearErrorWatcher(tilNew));
        etConfirm.addTextChangedListener(new ClearErrorWatcher(tilConfirm));

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPass = etNew.getText() == null ? "" : etNew.getText().toString();
        String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

        boolean valid = true;

        // Luật mật khẩu (8 ký tự, chữ hoa, chữ thường) khai báo chung trong PasswordRules
        String ruleError = PasswordRules.errorOf(newPass);
        if (ruleError != null) {
            tilNew.setError(ruleError);
            valid = false;
        }
        if (!newPass.equals(confirm)) {
            tilConfirm.setError("Mật khẩu nhập lại không khớp");
            valid = false;
        }
        if (!valid) return;

        if (firstLogin) {
            submitFirstLogin(newPass);
        } else {
            // TODO: backend chưa có API quên mật khẩu (/api/auth/forgot-password/...). Khi có thì gọi ở đây.
            Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();
            goTo(LoginActivity.class);
        }
    }

    /** Gọi POST /api/auth/force-change-password rồi vào app. */
    private void submitFirstLogin(String newPass) {
        setLoading(true);

        ApiClient.get(this)
                .forceChangePassword(new ForceChangePasswordRequest(newPass))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoading(false);

                        if (response.isSuccessful()) {
                            SessionManager.get(ResetPasswordActivity.this)
                                    .setMustChangePassword(false);
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            goTo(MainActivity.class);
                        } else {
                            String message = ApiErrors.messageOf(response, "Đổi mật khẩu thất bại");
                            if (response.code() == 400) {
                                // Ví dụ: "Mật khẩu mới phải khác mật khẩu hiện tại" (trùng mật khẩu tạm)
                                tilNew.setError(message);
                            } else {
                                Toast.makeText(ResetPasswordActivity.this, message,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoading(false);
                        Toast.makeText(ResetPasswordActivity.this, ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        btnReset.setEnabled(!loading);
    }

    /** Mở màn đích và xóa toàn bộ các màn trước đó. */
    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /** Gõ lại thì xóa thông báo lỗi (và thu hẹp luôn dòng lỗi) của ô tương ứng. */
    private static class ClearErrorWatcher implements TextWatcher {
        private final TextInputLayout layout;

        ClearErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
            FormErrors.clear(layout);
        }
        @Override public void afterTextChanged(Editable s) { }
    }
}