package com.auth_app_backend;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.auth_app_backend.config.UniversalAuthProperties;

@AutoConfiguration
@ComponentScan("com.auth_app_backend")
@EntityScan("com.auth_app_backend.entity")
@EnableJpaRepositories("com.auth_app_backend.repositories")
@EnableConfigurationProperties(UniversalAuthProperties.class)
@ConditionalOnProperty(prefix = "universal.auth", name = "enabled", matchIfMissing = true)
public class UniversalAuthAutoConfiguration {

}
