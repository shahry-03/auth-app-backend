package com.auth_app_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.auth_app_backend.config.UniversalAuthProperties;

@SpringBootApplication
@EnableConfigurationProperties(UniversalAuthProperties.class)
public class AuthAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthAppBackendApplication.class, args);
    }

}
