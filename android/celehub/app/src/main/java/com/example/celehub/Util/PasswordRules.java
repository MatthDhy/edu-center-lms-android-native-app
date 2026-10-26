package com.example.celehub.Util;

/** Luật đặt mật khẩu dùng chung cho cả app: sửa ở đây là mọi màn đều đổi theo. */
public final class PasswordRules {

    public static final int MIN_LENGTH = 8;

    /** Dòng gợi ý hiện dưới ô nhập mật khẩu mới. */
    public static final String HINT = "Ít nhất " + MIN_LENGTH + " ký tự, có chữ hoa và chữ thường";

    private PasswordRules() { }

    public static boolean hasMinLength(String password) {
        return password.length() >= MIN_LENGTH;
    }

    public static boolean hasUpper(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) return true;
        }
        return false;
    }

    public static boolean hasLower(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) return true;
        }
        return false;
    }

    public static boolean isValid(String password) {
        return hasMinLength(password) && hasUpper(password) && hasLower(password);
    }

    /** Trả về câu báo lỗi cụ thể cho điều kiện đầu tiên chưa đạt, hoặc null nếu mật khẩu hợp lệ. */
    public static String errorOf(String password) {
        if (!hasMinLength(password)) return "Mật khẩu tối thiểu " + MIN_LENGTH + " ký tự";
        if (!hasUpper(password)) return "Mật khẩu cần có ít nhất 1 chữ hoa";
        if (!hasLower(password)) return "Mật khẩu cần có ít nhất 1 chữ thường";
        return null;
    }
}