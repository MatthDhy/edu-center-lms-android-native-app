package com.celehub.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.mail.MailService;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

/**
 * Quên mật khẩu 3 bước (khớp 3 màn Android):
 *  1) request : email -> sinh OTP 6 số, gửi mail. LUÔN trả 200 (không lộ email có tồn tại hay không).
 *  2) verify  : email + OTP -> đúng thì cấp resetToken (sai quá 5 lần thì OTP chết).
 *  3) reset   : resetToken + mật khẩu mới -> đổi mật khẩu, must_change_password = false.
 */
@Service
public class PasswordResetService {
    static final int OTP_VALID_MINUTES = 10;
    static final int MAX_ATTEMPTS = 5;
    static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final String INVALID_OTP = "Mã OTP không đúng hoặc đã hết hạn";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public PasswordResetService(UserRepository userRepository, PasswordResetOtpRepository otpRepository,
                                PasswordEncoder passwordEncoder, MailService mailService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @Transactional
    public void requestOtp(String rawEmail) {
        userRepository.findByEmailIgnoreCase(rawEmail.trim())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::issueOtp);
    }

    private void issueOtp(User u) {
        LocalDateTime now = LocalDateTime.now();
        var last = otpRepository.findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(u.getId());
        if (last.isPresent() && last.get().getCreatedAt().isAfter(now.minusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return; // bấm gửi lại quá nhanh: bỏ qua, vẫn trả 200 như bình thường
        }
        otpRepository.invalidateAll(u.getId(), now);

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        PasswordResetOtp rec = new PasswordResetOtp();
        rec.setUserId(u.getId());
        rec.setOtpHash(passwordEncoder.encode(otp));
        rec.setExpiresAt(now.plusMinutes(OTP_VALID_MINUTES));
        otpRepository.save(rec);

        mailService.sendOtp(u.getEmail(), u.getFullName(), otp, OTP_VALID_MINUTES);
    }

    /** noRollbackFor: phải LƯU số lần nhập sai dù ném AppException (mặc định RuntimeException làm rollback). */
    @Transactional(noRollbackFor = AppException.class)
    public String verifyOtp(String rawEmail, String otp) {
        User u = userRepository.findByEmailIgnoreCase(rawEmail.trim())
                .filter(x -> x.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, INVALID_OTP));

        PasswordResetOtp rec = otpRepository.findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(u.getId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, INVALID_OTP));

        LocalDateTime now = LocalDateTime.now();
        if (rec.getVerifiedAt() != null || rec.getExpiresAt().isBefore(now) || rec.getAttempts() >= MAX_ATTEMPTS) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_OTP);
        }
        if (!passwordEncoder.matches(otp, rec.getOtpHash())) {
            rec.setAttempts(rec.getAttempts() + 1);
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_OTP);
        }

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        rec.setResetTokenHash(sha256Hex(resetToken));
        rec.setVerifiedAt(now);
        rec.setExpiresAt(now.plusMinutes(OTP_VALID_MINUTES)); // thời gian để nhập mật khẩu mới
        return resetToken;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        String invalid = "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn, vui lòng thực hiện lại";

        PasswordResetOtp rec = otpRepository.findByResetTokenHashAndUsedAtIsNull(sha256Hex(resetToken))
                .filter(r -> r.getVerifiedAt() != null && r.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, invalid));

        User u = userRepository.findById(rec.getUserId())
                .filter(x -> x.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, invalid));

        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setMustChangePassword(false); // người dùng đã tự chọn mật khẩu
        rec.setUsedAt(now);
    }

    private static String sha256Hex(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
