package com.example.celehub.Activity;
import com.example.celehub.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.AuthResponse;
import com.example.celehub.Model.LoginRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.Data.SessionManager;
import com.example.celehub.API.ApiClient;

public class LoginActivity extends AppCompatActivity {

    /** AuthInterceptor gắn cờ này khi đưa người dùng về Login vì phiên hết hạn. */
    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgotPassword);

        // Gõ lại thì xóa thông báo lỗi
        etEmail.addTextChangedListener(new SimpleWatcher(tilEmail));
        etPassword.addTextChangedListener(new SimpleWatcher(tilPassword));

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            valid = false;
        }
        if (!valid) return;

        setLoading(true);

        ApiClient.get(this)
                .login(new LoginRequest(email, password))
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                           Response<ApiResponse<AuthResponse>> response) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            onLoginSuccess(response.body().getData());
                        } else {
                            String message = ApiErrors.messageOf(response,
                                    "Đăng nhập thất bại, vui lòng thử lại");
                            if (response.code() == 401) {
                                // "Email hoặc mật khẩu không chính xác"
                                tilPassword.setError(message);
                            } else {
                                // 403: tài khoản bị khóa, 500: lỗi hệ thống...
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onLoginSuccess(AuthResponse auth) {
        // Ứng dụng này dành cho học viên. Tài khoản giáo viên / admin dùng web admin.
        if (!"STUDENT".equals(auth.getRole())) {
            Toast.makeText(this, "Tài khoản này không dùng trên ứng dụng học viên",
                    Toast.LENGTH_LONG).show();
            return;
        }

        SessionManager.get(this).saveLogin(auth);

        Intent intent;
        if (auth.isMustChangePassword()) {
            // Tài khoản do admin cấp: lần đầu bắt buộc đổi mật khẩu tạm
            intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_FIRST_LOGIN);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    /** Đang gọi server: khóa nút để không bấm nhiều lần. */
    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    /** Xóa lỗi của TextInputLayout mỗi khi người dùng gõ lại. */
    private static class SimpleWatcher implements TextWatcher {
        private final TextInputLayout layout;

        SimpleWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            layout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) { }
    }
}