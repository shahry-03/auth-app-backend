package com.auth_app_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves uploaded files at http://localhost:8080/uploads/**.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir)
                               .toAbsolutePath()
                               .normalize()
                               .toUri()
                               .toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}