package com.auth_app_backend.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        logger.info("Authorization header : {}", header);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. get the token from the header
        String token = header.substring(7);

        // 2. Parse the token using JwtService
        try {

            // check for access token
            if (!jwtService.isAccessToken(token)) {

                filterChain.doFilter(request, response);
                return;
            }

            Jws<Claims> parse = jwtService.parseToken(token);

            Claims claims = parse.getPayload();

            String userId = claims.getSubject();
            UUID userUuid = UserHelper.parseId(userId);
            // You can also extract other claims like email, roles, etc. if needed

            userRepository.findById(userUuid).ifPresent(user -> {

                // Check if the user is enabled before setting authentication
                if (user.isEnabled()) {
                    List<GrantedAuthority> authorities = user.getRoles() == null ? List.of()
                            : user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                                    .collect(Collectors.toList());

                    // Create an Authentication object based on the user details and authorities
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Set the authentication in the SecurityContextHolder
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }

            });
        } catch (ExpiredJwtException e) {

            request.setAttribute("error", "Token Expired");
            // Handle expired token exceptions
            // e.printStackTrace();
        } catch (Exception e) {
            request.setAttribute("error", "Token Expired");
            // Handle any other exceptions
            // e.printStackTrace();
        }

        filterChain.doFilter(request, response);

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("api/v1/auth/");
    }

}
