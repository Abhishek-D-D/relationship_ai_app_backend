package com.couplespace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MediaService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MediaService.class);

    public MediaService() {
        super();
    }

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:https://relationshipaiappbackend-production.up.railway.app}")
    private String baseUrl;

    public String storeFile(MultipartFile file) {
        try {
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), root.resolve(filename));

            log.info("Stored file: {}", filename);
            return baseUrl + "/api/v1/media/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new RuntimeException("Could not store file", e);
        }
    }
}
