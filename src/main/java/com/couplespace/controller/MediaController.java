package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.service.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File must not be empty"));
        }
        String url = mediaService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", url));
    }
}
