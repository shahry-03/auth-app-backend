package com.auth_app_backend.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.User.UserBuilder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth_app_backend.security.JwtAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // .authorizeHttpRequests(auth -> auth
                                // .requestMatchers("/api/v1/auth/register").permitAll()
                                // .requestMatchers("/api/v1/auth/login").permitAll()
                                // .anyRequest().authenticated())
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
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
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
