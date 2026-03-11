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
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.max-tokens:800}")
    private int maxTokens;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chatCompletion(String userMessage) throws IOException {
        return chatCompletion("You are a helpful relationship coach.", userMessage);
    }

    public String chatCompletion(String systemPrompt, String userMessage) throws IOException {
        String requestBody = """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": %s},
                {"role": "user",   "content": %s}
              ],
              "max_tokens": %d,
              "temperature": 0.7
            }
            """.formatted(
                model,
                objectMapper.writeValueAsString(systemPrompt),
                objectMapper.writeValueAsString(userMessage),
                maxTokens
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("OpenAI API error {}: {}", response.code(), errorBody);
                throw new IOException("OpenAI API returned: " + response.code());
            }
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }
}
