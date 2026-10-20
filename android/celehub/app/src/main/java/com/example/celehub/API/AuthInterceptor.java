package com.example.celehub.API;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import com.example.celehub.Data.SessionManager;
import com.example.celehub.Activity.LoginActivity;
/**
 * Chạy trên MỌI lệnh gọi API:
 *  1) Tự gắn "Authorization: Bearer <token>" nếu đã đăng nhập.
 *  2) Nếu server trả 401 (token hết hạn / tài khoản bị khóa) thì xóa phiên và đưa về màn Login.
 */
public class AuthInterceptor implements Interceptor {

    private final Context appContext;
    private final SessionManager session;

    public AuthInterceptor(Context appContext, SessionManager session) {
        this.appContext = appContext;
        this.session = session;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        // Chính API đăng nhập thì không cần token, và 401 ở đây chỉ là "sai mật khẩu"
        if (original.url().encodedPath().endsWith("/api/auth/login")) {
            return chain.proceed(original);
        }

        String token = session.getToken();
        Request.Builder builder = original.newBuilder();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(builder.build());

        if (response.code() == 401 && token != null) {
            session.clear();
            Intent intent = new Intent(appContext, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true);
            appContext.startActivity(intent);
        }
        return response;
    }
}