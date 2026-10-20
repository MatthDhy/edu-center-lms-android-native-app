package com.example.celehub.Data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.celehub.Model.AuthResponse;
import com.example.celehub.Model.UserDto;
/**
 * Lưu token và thông tin người dùng sau khi đăng nhập, để lần sau mở app khỏi đăng nhập lại.
 * Dữ liệu nằm trong SharedPreferences riêng của app (app khác không đọc được).
 */
public class SessionManager {

    private static final String PREFS_NAME = "celehub_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MUST_CHANGE = "must_change_password";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager get(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /** Gọi ngay sau khi đăng nhập thành công. */
    public void saveLogin(AuthResponse auth) {
        UserDto user = auth.getUser();
        prefs.edit()
                .putString(KEY_TOKEN, auth.getToken())
                .putString(KEY_ROLE, auth.getRole())
                .putBoolean(KEY_MUST_CHANGE, auth.isMustChangePassword())
                .putString(KEY_FULL_NAME, user != null ? user.getFullName() : null)
                .putString(KEY_EMAIL, user != null ? user.getEmail() : null)
                .putString(KEY_PHONE, user != null ? user.getPhone() : null)
                .apply();
    }

    /** Đăng xuất hoặc phiên hết hạn: xóa sạch. */
    public void clear() {
        prefs.edit().clear().apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public boolean isMustChangePassword() {
        return prefs.getBoolean(KEY_MUST_CHANGE, false);
    }

    public void setMustChangePassword(boolean value) {
        prefs.edit().putBoolean(KEY_MUST_CHANGE, value).apply();
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    /** "Gia Huy" -> "GH", "Nguyễn Minh Nhật" -> "NN" (chữ cái đầu của từ đầu và từ cuối). */
    public String getInitials() {
        String name = getFullName().trim();
        if (name.isEmpty()) return "?";
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}