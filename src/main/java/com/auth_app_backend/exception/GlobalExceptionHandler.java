package com.auth_app_backend.exception;

import com.auth_app_backend.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.auth_app_backend.exception.EmailNotVerifiedException;
import com.auth_app_backend.exception.RateLimitExceededException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ═══════════════════════════════════════════════════════════════
    //  400 — VALIDATION ERRORS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bean Validation failures (@Valid on @RequestBody).
     * Returns field-by-field errors in the response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing
            ));

        ApiError error = ApiError.ofValidation(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            request.getRequestURI(),
            fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Malformed JSON body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Malformed JSON request body",
            request.getRequestURI());

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Illegal arguments — bad input, business validation failures.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI());

        return ResponseEntity.badRequest().body(error);
    }

    // ═══════════════════════════════════════════════════════════════
    //  401 — AUTHENTICATION ERRORS
    // ═══════════════════════════════════════════════════════════════

    /**
     * All authentication failures — bad credentials, disabled, locked, expired.
     * Returns 401 Unauthorized.
     */
    @ExceptionHandler({
        BadCredentialsException.class,
        UsernameNotFoundException.class,
        CredentialsExpiredException.class,
        DisabledException.class,
        LockedException.class,
        AuthenticationException.class
    })
    public ResponseEntity<ApiError> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request) {

        // Don't leak internal details — generic message for auth failures
        String message = "Invalid credentials or account unavailable";
        if (ex instanceof DisabledException) {
            message = "Account is disabled";
        } else if (ex instanceof LockedException) {
            message = "Account is locked";
        } else if (ex instanceof CredentialsExpiredException) {
            message = "Credentials have expired";
        }

        ApiError error = ApiError.of(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            message,
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // ═══════════════════════════════════════════════════════════════
    //  403 — AUTHORIZATION ERRORS (RBAC)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Thrown by @PreAuthorize when user lacks role/permission.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "You don't have permission to access this resource",
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ═══════════════════════════════════════════════════════════════
    //  404 — RESOURCE NOT FOUND
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ═══════════════════════════════════════════════════════════════
    //  409 — CONFLICT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Duplicate email, role, permission — unique constraint violations.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "Resource already exists or violates a constraint",
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ═══════════════════════════════════════════════════════════════
    //  500 — CATCH-ALL
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);

        ApiError error = ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ─── 403 Forbidden (email not verified) ────────────────────────
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(
            EmailNotVerifiedException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.FORBIDDEN.value(),
            "Email Not Verified",
            ex.getMessage(),
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }



    // ─── 429 Too Many Requests ─────────────────────────────────────
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.of(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too Many Requests",
            ex.getMessage(),
            request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(error);
    }


}

