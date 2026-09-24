package com.auth_app_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.auth_app_backend.config.UniversalAuthProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(UniversalAuthProperties.class)
@EnableAsync
public class AuthAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthAppBackendApplication.class, args);
    }

}
