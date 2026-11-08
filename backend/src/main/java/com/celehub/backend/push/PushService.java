package com.celehub.backend.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.celehub.backend.notification.DeviceToken;
import com.celehub.backend.notification.DeviceTokenRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

/**
 * Bắn push FCM. Chạy bất đồng bộ và nuốt mọi lỗi: push hỏng không bao giờ được làm hỏng nghiệp vụ
 * (thông báo vẫn nằm trong bảng notifications). Chưa bật Firebase thì không làm gì.
 */
@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final int BATCH = 500;   // giới hạn của FCM mỗi lần gửi

    private final ObjectProvider<FirebaseMessaging> messagingProvider;
    private final DeviceTokenRepository tokenRepository;

    public PushService(ObjectProvider<FirebaseMessaging> messagingProvider, DeviceTokenRepository tokenRepository) {
        this.messagingProvider = messagingProvider;
        this.tokenRepository = tokenRepository;
    }

    @Async
    public void sendToUsers(Collection<Long> userIds, String title, String body, String imageUrl, Map<String, String> data) {
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null || userIds == null || userIds.isEmpty()) return;
        try {
            List<String> tokens = tokenRepository.findByUserIdIn(userIds).stream().map(DeviceToken::getFcmToken).distinct().toList();
            for (int i = 0; i < tokens.size(); i += BATCH) {
                sendBatch(messaging, tokens.subList(i, Math.min(i + BATCH, tokens.size())), title, body, imageUrl, data);
            }
        } catch (Exception e) {
            log.warn("Gửi push lỗi: {}", e.getMessage());
        }
    }

    private void sendBatch(FirebaseMessaging messaging, List<String> tokens, String title, String body,
                           String imageUrl, Map<String, String> data) throws Exception {
        com.google.firebase.messaging.Notification.Builder nb = com.google.firebase.messaging.Notification.builder()
                .setTitle(title == null ? "CeLeHub" : title).setBody(body == null ? "" : body);
        if (imageUrl != null && !imageUrl.isBlank()) nb.setImage(imageUrl);

        MulticastMessage.Builder mb = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(nb.build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder().setChannelId("celehub_default").build())
                        .build());
        if (data != null && !data.isEmpty()) mb.putAllData(data);

        BatchResponse res = messaging.sendEachForMulticast(mb.build());
        if (res.getFailureCount() == 0) return;
        List<String> dead = new ArrayList<>();
        List<SendResponse> list = res.getResponses();
        for (int i = 0; i < list.size(); i++) {
            SendResponse r = list.get(i);
            if (r.isSuccessful() || r.getException() == null) continue;
            MessagingErrorCode code = r.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) dead.add(tokens.get(i));
        }
        if (!dead.isEmpty()) tokenRepository.deleteByTokens(dead);
    }
}
