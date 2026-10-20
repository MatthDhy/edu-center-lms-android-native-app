
package com.example.celehub.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.celehub.Data.SessionManager;
import com.example.celehub.R;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable navigateToLogin = new Runnable() {
        @Override
        public void run() {
            SessionManager session = SessionManager.get(SplashActivity.this);
            Intent intent;
            if (!session.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else if (session.isMustChangePassword()) {
                intent = new Intent(SplashActivity.this, ResetPasswordActivity.class);
                intent.putExtra(ResetPasswordActivity.EXTRA_MODE, ResetPasswordActivity.MODE_FIRST_LOGIN);
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // An thanh trang thai
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Hien thi giao dien Splash
        setContentView(R.layout.activity_splash);

        // Lay TextView ten ung dung
        TextView tvAppName = findViewById(R.id.tvAppName);

        // Tao chu CeLeHub
        SpannableString appName = new SpannableString("CeLeHub");

        // Mau cho chu CeLe
        appName.setSpan(
                new ForegroundColorSpan(Color.parseColor("#1F5FBF")),
                0, 4,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Mau cho chu Hub
        appName.setSpan(
                new ForegroundColorSpan(Color.parseColor("#7B61FF")),
                4, 7,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvAppName.setText(appName);

        // Tu dong chuyen sang Login sau 3 giay
        handler.postDelayed(navigateToLogin, 3000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateToLogin);
        super.onDestroy();
    }
}
