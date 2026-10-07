package com.auth_app_backend.controllers;

import com.auth_app_backend.entity.User;
import com.auth_app_backend.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    // ═══════════════════════════════════════════════════════════
    //  UPLOAD
    // ═══════════════════════════════════════════════════════════

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "profile") String type,
            Authentication authentication) {

        // Principal = User entity (JwtAuthenticationFilter sets it)
        User currentUser = (User) authentication.getPrincipal();

        // Folder: /uploads/users/{uuid}/{type}/uuid.ext
        String userFolder = "users/" + currentUser.getId() + "/" + sanitizeType(type);

        String url = fileStorageService.save(file, userFolder);

        log.info("File uploaded by user {}: {}", currentUser.getId(), url);

        return ResponseEntity.ok(Map.of(
            "url", url,
            "message", "File uploaded successfully"
        ));
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════

    private String sanitizeType(String type) {
        if (type == null || !type.matches("[a-zA-Z0-9_-]+")) {
            return "profile";
        }
        return type.toLowerCase();
    }
}