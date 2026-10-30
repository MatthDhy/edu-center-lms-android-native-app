package com.example.celehub.Activity;

import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.celehub.API.ApiClient;
import com.example.celehub.API.ApiErrors;
import com.example.celehub.Model.ApiResponse;
import com.example.celehub.Model.VerifyOtpRequest;
import com.example.celehub.Model.VerifyOtpResponse;

import com.example.celehub.R;
import com.google.android.material.button.MaterialButton;

public class OtpActivity extends AppCompatActivity {
    private String email;
    private static final int OTP_LENGTH = 6;
    private final EditText[] boxes = new EditText[OTP_LENGTH];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        email = getIntent().getStringExtra("email");

        int[] ids = {R.id.otp1, R.id.otp2, R.id.otp3, R.id.otp4, R.id.otp5, R.id.otp6};
        for (int i = 0; i < OTP_LENGTH; i++) {
            boxes[i] = findViewById(ids[i]);
            setupBox(i);
        }
        boxes[0].requestFocus();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButton btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> verifyOtp());
    }

    /** Gõ xong 1 số thì tự nhảy sang ô kế tiếp; bấm xóa ở ô trống thì lùi về ô trước. */
    private void setupBox(final int index) {
        boxes[index].addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 1 && index < OTP_LENGTH - 1) {
                    boxes[index + 1].requestFocus();
                }
            }
        });

        boxes[index].setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && boxes[index].getText().length() == 0
                    && index > 0) {
                boxes[index - 1].setText("");
                boxes[index - 1].requestFocus();
                return true;
            }
            return false;
        });
    }

    private void verifyOtp() {
        StringBuilder code = new StringBuilder();
        for (EditText box : boxes) {
            code.append(box.getText().toString());
        }
        if (code.length() < OTP_LENGTH) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setEnabled(false);

        ApiClient.get(this).forgotVerify(new VerifyOtpRequest(email, code.toString()))
                .enqueue(new Callback<ApiResponse<VerifyOtpResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<VerifyOtpResponse>> call,
                                           Response<ApiResponse<VerifyOtpResponse>> res) {
                        if (isFinishing() || isDestroyed()) return;
                        btnContinue.setEnabled(true);
                        if (res.isSuccessful() && res.body() != null && res.body().getData() != null) {
                            Intent intent = new Intent(OtpActivity.this, ResetPasswordActivity.class);
                            intent.putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_FORGOT);
                            intent.putExtra(ResetPasswordActivity.EXTRA_RESET_TOKEN,
                                    res.body().getData().getResetToken());
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(OtpActivity.this,
                                    ApiErrors.messageOf(res, "Mã OTP không đúng hoặc đã hết hạn"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<VerifyOtpResponse>> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        btnContinue.setEnabled(true);
                        Toast.makeText(OtpActivity.this, ApiErrors.NETWORK_ERROR, Toast.LENGTH_LONG).show();
                    }
                });

    }
}