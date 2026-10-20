package com.celehub.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;

/** Tạo sẵn 1 tài khoản ADMIN ở lần chạy đầu để có người tạo học viên/giảng viên. */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) return;

        User admin = new User();
        admin.setFullName("Administrator");
        admin.setEmail(props.bootstrap().adminEmail().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(props.bootstrap().adminPassword()));
        admin.setRole(Role.ADMIN);
        admin.setMustChangePassword(false);
        userRepository.save(admin);
        log.warn("Đã tạo admin mặc định: {} — hãy đổi mật khẩu khi triển khai thật!", admin.getEmail());
    }
}
