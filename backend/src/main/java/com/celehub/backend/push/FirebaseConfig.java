package com.celehub.backend.push;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Chỉ bật khi app.firebase.enabled=true (biến môi trường FIREBASE_ENABLED=true).
 * Khóa service account lấy từ FIREBASE_CREDENTIALS_PATH (đường dẫn file JSON trong container)
 * hoặc FIREBASE_CREDENTIALS_BASE64 (nội dung JSON mã hóa base64, tiện cho Render/Railway).
 */
@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${app.firebase.credentials-path:}") String path,
                                               @Value("${app.firebase.credentials-base64:}") String base64) throws IOException {
        try (InputStream in = open(path, base64)) {
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        }
    }

    private static InputStream open(String path, String base64) throws IOException {
        if (base64 != null && !base64.isBlank()) return new ByteArrayInputStream(Base64.getDecoder().decode(base64.trim()));
        if (path != null && !path.isBlank()) return new FileInputStream(path);
        throw new IllegalStateException("Bật Firebase nhưng thiếu FIREBASE_CREDENTIALS_PATH hoặc FIREBASE_CREDENTIALS_BASE64");
    }
}
