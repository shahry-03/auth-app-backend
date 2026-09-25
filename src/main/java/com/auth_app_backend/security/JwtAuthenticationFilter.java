package com.auth_app_backend.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth_app_backend.helper.UserHelper;
import com.auth_app_backend.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // Only access tokens allowed here
            if (!jwtService.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Jws<Claims> parse = jwtService.parseToken(token);
            Claims claims = parse.getPayload();

            String userId = claims.getSubject();
            UUID userUuid = UserHelper.parseId(userId);

            userRepository.findById(userUuid).ifPresent(user -> {

                if (user.isEnabled()) {

                    //  USE User.getAuthorities() — already contains
                    //    "ROLE_ADMIN", "ROLE_USER", "user:read", "user:write" etc.
                    //  FIX: principal is now the User entity (implements UserDetails)
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            user,    // ← User, not user.getEmail()
                            null,
                            user.getAuthorities());   // ← YEH KEY HAI

                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            });
        } catch (ExpiredJwtException e) {
            logger.debug("Access token expired: {}", e.getMessage());
            request.setAttribute("error", "Token Expired");
        } catch (Exception e) {
            logger.debug("JWT filter error: {}", e.getMessage());
            request.setAttribute("error", "Invalid Token");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip JWT processing only for PUBLIC auth endpoints
        // (Note: 2FA setup/enable/disable/status are NOT in this list — they need auth)
        return path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh")
            || path.equals("/api/v1/auth/logout")
            || path.equals("/api/v1/auth/verify-email")
            || path.equals("/api/v1/auth/resend-verification")
            || path.equals("/api/v1/auth/forgot-password")
            || path.equals("/api/v1/auth/reset-password")
            || path.equals("/api/v1/auth/2fa/verify")     // uses temp token, not access token
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }
}