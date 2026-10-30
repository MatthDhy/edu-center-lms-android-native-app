package com.example.celehub.Activity;

import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.ForgotPasswordRequest;
import com.example.celehub.R;
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
            requestOtp(email, btnContinue, tilEmail);
        });

    }
    private void requestOtp(String email, MaterialButton button, TextInputLayout tilEmail) {
        button.setEnabled(false);
        ApiClient.get(this).forgotRequest(new ForgotPasswordRequest(email))
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> res) {
                        if (isFinishing() || isDestroyed()) return;
                        button.setEnabled(true);
                        if (res.isSuccessful()) {
                            Intent intent = new Intent(ForgotPasswordActivity.this, OtpActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        } else {
                            tilEmail.setError(ApiErrors.messageOf(res, "Không gửi được mã, vui lòng thử lại"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        button.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, ApiErrors.NETWORK_ERROR,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}