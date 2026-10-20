package com.example.celehub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgotPassword);

        // Gõ lại thì xóa thông báo lỗi
        etEmail.addTextChangedListener(new SimpleWatcher(tilEmail));
        etPassword.addTextChangedListener(new SimpleWatcher(tilPassword));

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgot.setOnClickListener(v -> {
            // TODO: mở màn hình quên mật khẩu
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            valid = false;
        }
        if (!valid) return;

        // TODO: gọi API / Firebase để đăng nhập thật ở đây
        startActivity(new Intent(this, MainActivity.class));
        finish();
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