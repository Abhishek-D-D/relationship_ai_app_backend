package com.couplespace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ModerationService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * Checks if the text violates OpenAI's usage policies.
     * @param text The text to check.
     * @return true if the text is flagged as harmful.
     */
    public boolean isHarmful(String text) {
        if (text == null || text.isBlank()) return false;

        try {
            String requestBody = """
                {
                  "input": %s
                }
                """.formatted(objectMapper.writeValueAsString(text));

            Request request = new Request.Builder()
                    .url(baseUrl + "/moderations")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Moderation API error: {}", response.code());
                    return false; // Fail safe (assume not harmful if API is down, but prompts handle it)
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                return root.path("results").get(0).path("flagged").asBoolean();
            }
        } catch (IOException e) {
            log.error("Moderation check failed: {}", e.getMessage());
            return false;
        }
    }
}
