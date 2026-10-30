package com.example.celehub.Model;

/** OTP đúng thì server trả resetToken, app giữ lại để gửi kèm mật khẩu mới ở bước cuối. */
public class VerifyOtpResponse {
    private String resetToken;
    public String getResetToken() { return resetToken; }
}