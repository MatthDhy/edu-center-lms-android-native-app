package com.celehub.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Bản sơ khai: chỉ ghi log. Khi cần gửi mail thật:
 * thêm spring-boot-starter-mail + JavaMailSender (Gmail SMTP App Password / Brevo / Resend).
 * Giữ nguyên chữ ký các hàm để khỏi sửa chỗ gọi.
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public void sendWelcome(String to, String fullName, String tempPassword) {
        // CHỈ DÙNG KHI DEV — đừng log mật khẩu ở môi trường thật
        log.info("[MAIL STUB] To={} | Xin chào {} | Mật khẩu tạm: {}", to, fullName, tempPassword);
    }

    public void sendOtp(String to, String fullName, String otp, int validMinutes) {
        // CHỈ DÙNG KHI DEV — đừng log OTP ở môi trường thật
        log.info("[MAIL STUB] To={} | Xin chào {} | Mã OTP đặt lại mật khẩu: {} (hết hạn sau {} phút)",
                to, fullName, otp, validMinutes);
    }
}
