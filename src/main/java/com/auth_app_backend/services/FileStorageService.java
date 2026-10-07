package com.auth_app_backend.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Save an uploaded file under the given subfolder.
     * Returns relative URL like "/uploads/users/{userId}/profile/uuid.jpg".
     * Throws IllegalArgumentException for empty/invalid/oversized files.
     */
    String save(MultipartFile file, String subFolder);

    /**
     * Delete a previously-stored file by its relative URL.
     * Silently ignores if file doesn't exist.
     */
    void delete(String relativeUrl);
}