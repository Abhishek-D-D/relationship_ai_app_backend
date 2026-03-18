package com.couplespace.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MediaService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.upload-preset:ml_default}")
    private String uploadPreset;

    private final OkHttpClient httpClient;

    public MediaService() {
        this.httpClient = new OkHttpClient.Builder().build();
    }

    /**
     * Uploads a file to Cloudinary via unsigned upload preset.
     * Falls back to a data URL if Cloudinary is not configured (dev only).
     */
    public String storeFile(MultipartFile file) {
        if (cloudName == null || cloudName.isBlank()) {
            throw new RuntimeException("Cloudinary cloud-name is not configured. Set CLOUDINARY_CLOUD_NAME env var.");
        }

        try {
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String resourceType = resolveResourceType(mimeType);

            RequestBody fileBody = RequestBody.create(file.getBytes(), MediaType.parse(mimeType));
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build();

            String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload";
            Request request = new Request.Builder().url(url).post(body).build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "unknown";
                    log.error("Cloudinary upload failed: {} — {}", response.code(), errorBody);
                    throw new RuntimeException("Cloudinary upload failed: " + response.code());
                }
                String responseBody = response.body().string();
                // Extract secure_url from JSON response
                Matcher matcher = Pattern.compile("\"secure_url\":\"([^\"]+)\"").matcher(responseBody);
                if (matcher.find()) {
                    String secureUrl = matcher.group(1).replace("\\/", "/");
                    log.info("Cloudinary upload success: {}", secureUrl);
                    return secureUrl;
                }
                throw new RuntimeException("Could not parse Cloudinary response");
            }
        } catch (IOException e) {
            log.error("Failed to upload to Cloudinary", e);
            throw new RuntimeException("Could not upload file: " + e.getMessage());
        }
    }

    private String resolveResourceType(String mimeType) {
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "video"; // Cloudinary treats audio as video resource
        return "raw";
    }
}
