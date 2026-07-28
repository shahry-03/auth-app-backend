package com.auth_app_backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Auth Application Backend API", description = "Universal auth app that can be used with any application.", version = "1.0", summary = "This app is very useful if you don't want to create auth app from scratch.", contact = @Contact(name = "Shahrayar Ali", url = "https://www.shahrayarali.me/", email = "shahrayarsahito7@gmail.com")), security = {
        @SecurityRequirement(name = "bearerScheme")
})
@SecurityScheme(name = "bearerScheme", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "Enter JWT Bearer Token to access protected endpoints")
public class APIDocConfig {
    // Configuration class to customize OpenAPI / Swagger documentation
}