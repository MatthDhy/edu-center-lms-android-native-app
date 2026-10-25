package com.celehub.backend.user;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.AppException;
import com.celehub.backend.mail.MailService;
import com.celehub.backend.storage.StorageService;
import com.celehub.backend.user.dto.CreateUserRequest;
import com.celehub.backend.user.dto.UpdateProfileRequest;
import com.celehub.backend.user.dto.UserDto;

@Service
public class UserService {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final StorageService storageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       MailService mailService, StorageService storageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.storageService = storageService;
    }

    // ===================== Admin =====================

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
        u.setPhone(blankToNull(req.phone()));
        u.setRole(req.role());
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        userRepository.save(u);

        mailService.sendWelcome(u.getEmail(), u.getFullName(), tempPassword);
        return UserDto.from(u);
    }

    public List<UserDto> searchUsers(Role role, UserStatus status, String search) {
        String query = (search != null) ? search.trim().toLowerCase() : "";
        return userRepository.searchUsers(role, status, query).stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatus newStatus) {
        User u = load(id);
        if (u.getRole() == Role.ADMIN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể thay đổi trạng thái của tài khoản quản trị");
        }
        u.setStatus(newStatus);
        userRepository.save(u);
        return UserDto.from(u);
    }

    @Transactional
    public String resetPassword(Long id) {
        User u = load(id);
        String tempPassword = generateTempPassword(10);
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setMustChangePassword(true);
        userRepository.save(u);

        mailService.sendWelcome(u.getEmail(), u.getFullName(), tempPassword);
        return tempPassword;
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "totalStudents", userRepository.countByRole(Role.STUDENT),
                "totalTeachers", userRepository.countByRole(Role.TEACHER),
                "activeUsers", userRepository.countByStatus(UserStatus.ACTIVE),
                "lockedUsers", userRepository.countByStatus(UserStatus.LOCKED),
                "totalUsers", userRepository.count());
    }

    // ===================== Chính chủ (tab Cá nhân) =====================

    public UserDto getById(Long id) {
        return UserDto.from(load(id));
    }

    @Transactional
    public UserDto updateProfile(Long id, UpdateProfileRequest req) {
        User u = load(id);
        if (req.fullName() != null) {
            String name = req.fullName().trim();
            if (name.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống");
            u.setFullName(name);
        }
        if (req.phone() != null) {
            u.setPhone(blankToNull(req.phone()));
        }
        return UserDto.from(u);
    }

    @Transactional
    public UserDto updateAvatar(Long id, MultipartFile file) {
        validateAvatar(file);
        User u = load(id);
        String oldUrl = u.getAvatarUrl();
        u.setAvatarUrl(storageService.upload(file, "avatars"));
        if (oldUrl != null) storageService.delete(oldUrl);
        return UserDto.from(u);
    }

    @Transactional
    public UserDto deleteAvatar(Long id) {
        User u = load(id);
        if (u.getAvatarUrl() != null) {
            storageService.delete(u.getAvatarUrl());
            u.setAvatarUrl(null);
        }
        return UserDto.from(u);
    }

    // ===================== helpers =====================

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện tối đa 5MB");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext != null && !IMAGE_EXT.contains(ext.toLowerCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
        }
        if (!looksLikeImage(file)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tệp không phải ảnh JPG, PNG hoặc WEBP hợp lệ");
        }
    }

    /** Kiểm tra "magic bytes" — không tin tên file / Content-Type do client gửi. */
    private static boolean looksLikeImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length < 12) return false;
            boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            return jpeg || png || webp;
        } catch (IOException e) {
            return false;
        }
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
