package com.couplespace.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Sends Firebase Cloud Messaging (FCM) v1 API push notifications.
 * Uses a legacy server key for simplicity. To use FCM v1 API, replace with
 * OAuth2 service account.
 */
@Service
@Slf4j
public class FcmNotificationService {

    @Value("${fcm.server-key:REPLACE_WITH_YOUR_FCM_SERVER_KEY}")
    private String fcmServerKey;

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public void sendNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()
                || fcmServerKey == null || fcmServerKey.startsWith("REPLACE")) {
            log.warn("FCM not configured or token missing — skipping notification. Title: {}", title);
            return;
        }

        String payload = """
                {
                  "to": "%s",
                  "notification": {
                    "title": "%s",
                    "body": "%s",
                    "sound": "default"
                  },
                  "data": {
                    "type": "MOOD_UPDATE",
                    "click_action": "FLUTTER_NOTIFICATION_CLICK"
                  },
                  "priority": "high"
                }
                """.formatted(
                escapeJson(fcmToken),
                escapeJson(title),
                escapeJson(body));

        Request request = new Request.Builder()
                .url(FCM_URL)
                .header("Authorization", "key=" + fcmServerKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("FCM notification sent: {}", title);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.warn("FCM send failed ({}): {}", response.code(), errorBody);
            }
        } catch (IOException e) {
            log.error("FCM IOException: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
