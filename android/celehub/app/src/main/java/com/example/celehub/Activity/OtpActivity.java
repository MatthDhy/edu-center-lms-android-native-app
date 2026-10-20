package com.example.celehub.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.celehub.R;
import com.google.android.material.button.MaterialButton;

public class OtpActivity extends AppCompatActivity {

    private static final int OTP_LENGTH = 6;
    private final EditText[] boxes = new EditText[OTP_LENGTH];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

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

        // TODO: gửi code.toString() lên server để xác thực ở đây

        // OTP đúng -> sang màn đặt mật khẩu mới
        startActivity(new Intent(this, ResetPasswordActivity.class));
        finish();
    }
}