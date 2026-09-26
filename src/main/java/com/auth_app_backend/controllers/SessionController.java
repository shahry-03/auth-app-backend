package com.auth_app_backend.controllers;

import com.auth_app_backend.dto.response.ApiResponse;
import com.auth_app_backend.dto.response.SessionResponse;
import com.auth_app_backend.entity.User;
import com.auth_app_backend.services.SessionService;
import com.auth_app_backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final JwtService jwtService;

    /**
     * List all active sessions of the current user.
     * The session matching the current access token is marked as current=true.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        String currentJti = extractCurrentJti(request);
        List<SessionResponse> sessions = sessionService.listSessions(user.getId(), currentJti);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Revoke a specific session by ID.
     * Cannot revoke a session that doesn't belong to the current user.
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            HttpServletRequest request) {

        String currentJti = extractCurrentJti(request);
        sessionService.revokeSession(user.getId(), sessionId, currentJti);
        return ResponseEntity.ok(ApiResponse.success("Session revoked"));
    }

    /**
     * Revoke all sessions EXCEPT the current one.
     * Common UX: "Sign out other devices".
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> revokeAllExceptCurrent(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        String currentJti = extractCurrentJti(request);
        int count = sessionService.revokeAllExceptCurrent(user.getId(), currentJti);
        return ResponseEntity.ok(ApiResponse.success(
            count + " other session(s) revoked"));
    }

    /**
     * Extract the jti from the current access token (Authorization header).
     * Returns null if not present — client wouldn't see "current: true" for any session.
     */
    private String extractCurrentJti(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtService.getRefreshJtiFromToken(header.substring(7));
        } catch (Exception e) {
            return null;
        }
    }
}