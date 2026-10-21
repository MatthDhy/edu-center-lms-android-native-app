package com.celehub.backend.user;

import java.security.SecureRandom;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.mail.MailService;
import com.celehub.backend.user.dto.CreateUserRequest;
import com.celehub.backend.user.dto.UserDto;

@Service
public class UserService {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được tạo tài khoản STUDENT hoặc TEACHER");
        }
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        String tempPassword = generateTempPassword(10);

        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhone(req.phone());
        u.setRole(req.role());
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        userRepository.save(u);

        mailService.sendWelcome(u.getEmail(), u.getFullName(), tempPassword);
        return UserDto.from(u);
    }

    public UserDto getById(Long id) {
        return userRepository.findById(id).map(UserDto::from)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    public java.util.List<UserDto> searchUsers(Role role, UserStatus status, String search) {
        String query = (search != null) ? search.trim().toLowerCase() : "";
        return userRepository.searchUsers(role, status, query).stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatus newStatus) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (u.getRole() == Role.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể thay đổi trạng thái của tài khoản quản trị");
        }
        u.setStatus(newStatus);
        userRepository.save(u);
        return UserDto.from(u);
    }

    @Transactional
    public String resetPassword(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        String tempPassword = generateTempPassword(10);
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        userRepository.save(u);

        mailService.sendWelcome(u.getEmail(), u.getFullName(), tempPassword);
        return tempPassword;
    }

    public java.util.Map<String, Object> getStats() {
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalTeachers = userRepository.countByRole(Role.TEACHER);
        long activeCount = userRepository.countByStatus(UserStatus.ACTIVE);
        long lockedCount = userRepository.countByStatus(UserStatus.LOCKED);
        long totalUsers = userRepository.count();

        return java.util.Map.of(
                "totalStudents", totalStudents,
                "totalTeachers", totalTeachers,
                "activeUsers", activeCount,
                "lockedUsers", lockedCount,
                "totalUsers", totalUsers
        );
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}

