package com.gamematcher.service;

import com.gamematcher.config.FirebasePushProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    private static final String APP_NAME = "gamematcher-fcm";

    private final FirebasePushProperties firebasePushProperties;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public boolean isEnabled() {
        return firebasePushProperties.isEnabled();
    }

    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!isEnabled() || token == null || token.isBlank()) return;
        if (!ensureInitialized()) return;

        Map<String, String> payloadData = data != null ? data : new HashMap<>();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(payloadData)
                .build();

        try {
            FirebaseMessaging.getInstance(getApp()).send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed: token={}, code={}, message={}", maskToken(token), e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("FCM send failed: token={}", maskToken(token), e);
        }
    }

    private boolean ensureInitialized() {
        if (!isEnabled()) return false;
        if (initialized.get()) return true;

        synchronized (this) {
            if (initialized.get()) return true;
            String serviceAccountPath = firebasePushProperties.getServiceAccountPath();
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                log.warn("FCM disabled: service account path is empty");
                return false;
            }

            try (FileInputStream in = new FileInputStream(serviceAccountPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                if (FirebaseApp.getApps().stream().noneMatch(a -> APP_NAME.equals(a.getName()))) {
                    FirebaseApp.initializeApp(options, APP_NAME);
                }
                initialized.set(true);
                log.info("FCM initialized");
                return true;
            } catch (IOException e) {
                log.warn("FCM initialization failed: cannot read service account file ({})", serviceAccountPath, e);
                return false;
            } catch (Exception e) {
                log.warn("FCM initialization failed", e);
                return false;
            }
        }
    }

    private FirebaseApp getApp() {
        return FirebaseApp.getApps().stream()
                .filter(a -> APP_NAME.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("FCM app is not initialized"));
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}

