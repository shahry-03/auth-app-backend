package com.auth_app_backend.services.impl;

import com.auth_app_backend.services.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final String URL_PREFIX = "/uploads/";

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size-bytes:2097152}") // 2 MB default
    private long maxSizeBytes;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize file storage", e);
        }
    }

    @Override
    public String save(MultipartFile file, String subFolder) {
        // 1. Validate empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // 2. Size check
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                "File size exceeds limit of " + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        // 3. Content-Type validation — relaxed
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean validContentType = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean validExtension = hasAllowedExtension(originalFilename);

        // ⚡ Accept if EITHER content-type OR extension is valid
        // (browsers send correct content-type; some tools/clients don't)
        if (!validContentType && !validExtension) {
            throw new IllegalArgumentException(
                "Only JPEG, PNG, WEBP, GIF images are allowed");
        }

        try {
            String safeSubFolder = sanitizeSubFolder(subFolder);
            Path targetDir = rootLocation.resolve(safeSubFolder).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new IllegalArgumentException("Invalid upload path");
            }
            Files.createDirectories(targetDir);

            // ⚡ Extension resolve — prefer filename, fallback to content-type
            String extension = resolveExtension(originalFilename, contentType);
            String filename = UUID.randomUUID() + extension;
            Path targetFile = targetDir.resolve(filename);

            Files.copy(file.getInputStream(), targetFile,
                StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = URL_PREFIX + safeSubFolder + "/" + filename;
            log.info("File saved: {} ({} bytes, contentType={})",
                relativeUrl, file.getSize(), contentType);

            return relativeUrl;
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  NEW helper
    // ─────────────────────────────────────────────────────────

    private boolean hasAllowedExtension(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.matches(".*\\.(jpg|jpeg|png|webp|gif)$");
    }

    @Override
    public void delete(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith(URL_PREFIX)) return;

        try {
            String relativePath = relativeUrl.substring(URL_PREFIX.length());
            Path targetFile = rootLocation.resolve(relativePath).normalize();

            if (!targetFile.startsWith(rootLocation)) {
                log.warn("Attempted to delete file outside upload dir: {}", relativeUrl);
                return;
            }
            boolean deleted = Files.deleteIfExists(targetFile);
            if (deleted) {
                log.info("File deleted: {}", relativeUrl);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", relativeUrl, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────

    private String sanitizeSubFolder(String subFolder) {
        if (subFolder == null || subFolder.isBlank()) {
            return "misc";
        }
        // Allow letters, digits, dash, underscore, and forward slashes only
        String cleaned = subFolder.replaceAll("[^a-zA-Z0-9_\\-/]", "");
        // Prevent leading/trailing slashes and double slashes
        cleaned = cleaned.replaceAll("^/+", "").replaceAll("/+$", "");
        cleaned = cleaned.replaceAll("/{2,}", "/");
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        // 1. Prefer extension from filename (most reliable)
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".jpeg")) return ".jpeg";
            if (lower.endsWith(".jpg"))  return ".jpg";
            if (lower.endsWith(".png"))  return ".png";
            if (lower.endsWith(".webp")) return ".webp";
            if (lower.endsWith(".gif"))  return ".gif";
        }

        // 2. Fallback by content-type
        if (contentType != null) {
            return switch (contentType.toLowerCase()) {
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif"  -> ".gif";
                case "image/jpeg" -> ".jpg";
                default           -> ".jpg";
            };
        }
        return ".jpg";
    }
}