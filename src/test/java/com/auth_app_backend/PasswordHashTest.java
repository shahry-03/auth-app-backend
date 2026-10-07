package com.auth_app_backend;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;


@SpringBootTest
class PasswordHashTest {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test 
    void generatePasswordHash() {

        String hash = passwordEncoder.encode("Shahry@123");
        System.out.println(hash);
    }
}