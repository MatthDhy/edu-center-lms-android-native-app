package com.celehub.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.auth.dto.AuthResponse;
import com.celehub.backend.auth.dto.LoginRequest;
import com.celehub.backend.common.AppException;
import com.celehub.backend.security.JwtService;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;
import com.celehub.backend.user.dto.UserDto;

@Service
public class AuthService {
    private static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không chính xác";
    private static final String SAME_PASSWORD = "Mật khẩu mới phải khác mật khẩu hiện tại";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa, vui lòng liên hệ trung tâm");
        }
        return new AuthResponse(jwtService.generateToken(u), u.getRole(), u.isMustChangePassword(), UserDto.from(u));
    }

    /** Đổi mật khẩu lần đầu: chỉ hợp lệ khi must_change_password = true, và không được trùng mật khẩu tạm. */
    @Transactional
    public void forceChangePassword(Long userId, String newPassword) {
        User u = load(userId);
        if (!u.isMustChangePassword()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản đã đổi mật khẩu, hãy dùng chức năng Đổi mật khẩu");
        }
        if (passwordEncoder.matches(newPassword, u.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, SAME_PASSWORD);
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setMustChangePassword(false);
    }

    /** Đổi mật khẩu chủ động (màn Cá nhân) — phải nhập mật khẩu cũ. */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User u = load(userId);
        if (!passwordEncoder.matches(oldPassword, u.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(newPassword, u.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, SAME_PASSWORD);
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        // /api/auth/** được miễn chặn must_change_password, nên đổi ở đây cũng phải gỡ cờ
        u.setMustChangePassword(false);
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }
}
