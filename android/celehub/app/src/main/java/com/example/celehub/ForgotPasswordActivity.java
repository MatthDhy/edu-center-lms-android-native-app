package com.example.celehub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        MaterialButton btnContinue = findViewById(R.id.btnContinue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Gõ lại thì xóa thông báo lỗi
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                tilEmail.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Email không hợp lệ");
                return;
            }
            // TODO: gọi API gửi mã OTP về email ở đây
            Intent intent = new Intent(this, OtpActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
        });
    }
}