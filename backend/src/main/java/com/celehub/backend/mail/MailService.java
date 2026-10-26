package com.celehub.backend.mail;

import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import com.celehub.backend.config.AppProperties;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final ObjectProvider<JavaMailSender> senderProvider;
    private final AppProperties props;

    public MailService(ObjectProvider<JavaMailSender> senderProvider, AppProperties props) {
        this.senderProvider = senderProvider;
        this.props = props;
    }

    @Async
    public void sendWelcome(String to, String fullName, String tempPassword) {
        String html = """
            <p>Xin chào <b>%s</b>,</p>
            <p>Trung tâm đã tạo tài khoản CeLeHub cho bạn:</p>
            <ul><li>Email đăng nhập: <b>%s</b></li>
                <li>Mật khẩu tạm: <b style="font-family:monospace">%s</b></li></ul>
            <p>Bạn sẽ phải đổi mật khẩu ngay ở lần đăng nhập đầu tiên.</p>""".formatted(esc(fullName), esc(to), esc(tempPassword));
        send(to, "[CeLeHub] Thông tin tài khoản của bạn", html, "tempPassword=" + tempPassword);
    }

    @Async
    public void sendOtp(String to, String fullName, String otp, int validMinutes) {
        String html = """
            <p>Xin chào <b>%s</b>,</p>
            <p>Mã OTP đặt lại mật khẩu của bạn:</p>
            <p style="font-size:28px;letter-spacing:6px"><b>%s</b></p>
            <p>Mã hết hạn sau %d phút. Tuyệt đối không chia sẻ mã này. Nếu không phải bạn yêu cầu, hãy bỏ qua email này.</p>"""
            .formatted(esc(fullName), esc(otp), validMinutes);
        send(to, "[CeLeHub] Mã OTP đặt lại mật khẩu", html, "otp=" + otp);
    }

    private void send(String to, String subject, String html, String devInfo) {
        if (!props.mail().enabled()) {                       // dev: chỉ log
            log.info("[MAIL DEV] To={} | {} | {}", to, subject, devInfo);
            return;
        }
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) { log.error("Chưa cấu hình SMTP"); return; }
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, "UTF-8");
            h.setFrom(props.mail().from());
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            sender.send(msg);
            log.info("Đã gửi mail '{}' tới {}", subject, to);
        } catch (Exception e) {
            log.error("Gửi mail tới {} thất bại: {}", to, e.getMessage());   // KHÔNG log nội dung mail
        }
    }

    private static String esc(String s) { return HtmlUtils.htmlEscape(s); }
}