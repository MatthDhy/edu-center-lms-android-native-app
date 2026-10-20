package com.celehub.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 1 filter làm 3 việc:
 *  1) đọc JWT  2) nạp user từ DB (bắt được tài khoản vừa bị khóa)
 *  3) chặn mọi API nghiệp vụ nếu must_change_password = true (trừ /api/auth/**)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JsonErrorWriter errorWriter;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, JsonErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.equals("/api/auth/login") || p.startsWith("/api/auth/forgot-password")
                || p.startsWith("/files/") || p.startsWith("/swagger-ui") || p.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        Long userId;
        try {
            userId = jwtService.extractUserId(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            errorWriter.write(res, 401, "Token không hợp lệ hoặc đã hết hạn");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            errorWriter.write(res, 401, "Tài khoản không tồn tại hoặc đã bị khóa");
            return;
        }

        if (user.isMustChangePassword() && !req.getRequestURI().startsWith("/api/auth/")) {
            errorWriter.write(res, 403, "Bắt buộc đổi mật khẩu trước khi sử dụng hệ thống");
            return;
        }

        var principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }
}
