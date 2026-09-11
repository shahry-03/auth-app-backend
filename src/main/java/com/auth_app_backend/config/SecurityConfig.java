package com.auth_app_backend.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.User.UserBuilder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth_app_backend.security.JwtAuthenticationFilter;

// import tools.jackson.databind.ObjectMapper; // for spring boot version 4.x
import com.fasterxml.jackson.databind.ObjectMapper; // for spring boot version 3.x

@Configuration
public class SecurityConfig {

        private JwtAuthenticationFilter jwtAuthenticationFilter;
        private AuthenticationSuccessHandler authenticationSuccessHandler;
        private UniversalAuthProperties properties;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        AuthenticationSuccessHandler authenticationSuccessHandler,
                        UniversalAuthProperties properties) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.authenticationSuccessHandler = authenticationSuccessHandler;
                this.properties = properties;
        }

        @Bean
        public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(AppConstants.AUTH_PUBLIC_URLS)
                                                .permitAll()
                                                .anyRequest().authenticated());

                if (properties.getOauth2().isEnabled()) {
                        http.oauth2Login(oauth2 -> oauth2
                                        .successHandler(authenticationSuccessHandler)
                                        .failureHandler(null));
                }

                http
                                .logout(AbstractHttpConfigurer::disable)
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // Error Message
                                                        authException.printStackTrace();
                                                        response.setStatus(401);
                                                        response.setContentType("application/json");

                                                        String message = "Unauthorized access: "
                                                                        + authException.getMessage();

                                                        String error = (String) request.getAttribute("error");
                                                        if (error != null) {
                                                                message = error;
                                                        }

                                                        Map<String, Object> errorMap = Map.of("message", message,
                                                                        "status", String.valueOf(401), "Status code",
                                                                        401);
                                                        var objectMapper = new ObjectMapper();
                                                        response.getWriter().write(
                                                                        objectMapper.writeValueAsString(errorMap));
                                                }))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();

        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        // @Bean
        // public UserDetailsService users() {
        // // Implement your user details service here
        // UserBuilder users = User.withDefaultPasswordEncoder();
        // UserDetails user1 = users
        // .username("shahry")
        // .password("abc")
        // .roles("USER")
        // .build();

        // UserDetails user2 = users
        // .username("rizwan")
        // .password("abc")
        // .roles("USER")
        // .build();

        // UserDetails user3 = users
        // .username("yahya")
        // .password("abc")
        // .roles("USER")
        // .build();
        // return new InMemoryUserDetailsManager(user1, user2, user3);
        // }

}
