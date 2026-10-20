package com.example.celehub;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilNew, tilConfirm;
    private TextInputEditText etNew, etConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        tilNew = findViewById(R.id.tilNewPassword);
        tilConfirm = findViewById(R.id.tilConfirmPassword);
        etNew = findViewById(R.id.etNewPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);

        etNew.addTextChangedListener(new ClearErrorWatcher(tilNew));
        etConfirm.addTextChangedListener(new ClearErrorWatcher(tilConfirm));

        findViewById(R.id.btnReset).setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPass = etNew.getText() == null ? "" : etNew.getText().toString();
        String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

        boolean valid = true;
        if (newPass.length() < 6) {
            tilNew.setError("Mật khẩu tối thiểu 6 ký tự");
            valid = false;
        }
        if (!newPass.equals(confirm)) {
            tilConfirm.setError("Mật khẩu nhập lại không khớp");
            valid = false;
        }
        if (!valid) return;

        // TODO: gọi API đặt lại mật khẩu ở đây (gửi newPass lên server)
        Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();

        // Quay về Login và xóa toàn bộ các màn của luồng quên mật khẩu
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /** Gõ lại thì xóa thông báo lỗi của ô tương ứng. */
    private static class ClearErrorWatcher implements TextWatcher {
        private final TextInputLayout layout;

        ClearErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
            layout.setError(null);
        }
        @Override public void afterTextChanged(Editable s) { }
    }
}