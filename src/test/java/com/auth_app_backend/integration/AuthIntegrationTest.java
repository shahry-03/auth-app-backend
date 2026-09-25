package com.auth_app_backend.integration;

import com.auth_app_backend.dto.request.LoginRequest;
import com.auth_app_backend.dto.request.RegisterRequest;
import com.auth_app_backend.entity.Role;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.repositories.RoleRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Auth Integration Tests (Full Stack)")
class AuthIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("authdb_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("universal.auth.jwt.secret",
            () -> "integration-test-secret-key-that-is-at-least-64-bytes-long-for-testing-only-x");
        registry.add("universal.auth.jwt.expiration", () -> "3600000");
        registry.add("universal.auth.jwt.refresh-token-expiration", () -> "604800000");
        registry.add("universal.auth.oauth2.enabled", () -> "false");
    }

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @MockBean private EmailService emailService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(
            new HttpComponentsClientHttpRequestFactory());

        baseUrl = "http://localhost:" + port;

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private ResponseEntity<String> register(String email, String password, String name) {
        RegisterRequest body = new RegisterRequest(email, password, name);
        return restTemplate.postForEntity(baseUrl + "/api/v1/auth/register", body, String.class);
    }

    private ResponseEntity<String> login(String email, String password) {
        LoginRequest body = new LoginRequest(email, password);
        return restTemplate.postForEntity(baseUrl + "/api/v1/auth/login", body, String.class);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String extractToken(ResponseEntity<String> response, String field) throws Exception {
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.path("data").path(field).asText();
    }

    /**
     * Mark user as verified directly in DB (simulates email click).
     */
    private void markUserVerified(String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AssertionError("User not found: " + email));
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);
    }

    private void assignRoleToUser(String email, String roleName) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AssertionError("User not found: " + email));
        Role role = roleRepository.findByRoleName(roleName)
            .orElseThrow(() -> new AssertionError("Role not found: " + roleName));
        user.getRoles().add(role);
        userRepository.saveAndFlush(user);
    }

    // ═══════════════════════════════════════════════════════════
    //  TEST 1 — Full Auth Flow
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Full Auth Flow")
    class FullAuthFlow {

        @Test
        @DisplayName("Register → Verify → Login → Access /me")
        void registerLoginAccessMe() throws Exception {
            // 1. Register
            ResponseEntity<String> regRes = register("flow@test.com", "Test@1234", "Flow User");
            assertThat(regRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // 2. Mark verified (simulating email click)
            markUserVerified("flow@test.com");

            // 3. Login — should succeed
            ResponseEntity<String> loginRes = login("flow@test.com", "Test@1234");
            assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);

            String accessToken = extractToken(loginRes, "accessToken");
            assertThat(accessToken).isNotBlank();

            // 4. Access /me
            HttpEntity<Void> request = new HttpEntity<>(bearerHeaders(accessToken));
            ResponseEntity<String> meRes = restTemplate.exchange(
                baseUrl + "/api/v1/users/me", HttpMethod.GET, request, String.class);

            assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode meJson = objectMapper.readTree(meRes.getBody());
            assertThat(meJson.path("data").path("email").asText()).isEqualTo("flow@test.com");
            assertThat(meJson.path("data").path("password").isMissingNode()).isTrue();
        }

        @Test
        @DisplayName("Duplicate registration should return 400")
        void duplicateRegistration() {
            register("dup@test.com", "Test@1234", "User");
            ResponseEntity<String> second = register("dup@test.com", "Test@1234", "User");
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Login with wrong password should return 401")
        void wrongPassword() {
            register("wrong@test.com", "Test@1234", "User");
            // Note: password check happens BEFORE email verification check
            ResponseEntity<String> res = login("wrong@test.com", "Wrong@5678");
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Login without verification should return 403")
        void loginWithoutVerificationReturns403() throws Exception {
            register("unverified@test.com", "Test@1234", "User");
            // Don't verify — try to login
            ResponseEntity<String> res = login("unverified@test.com", "Test@1234");
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            // Check error message
            JsonNode json = parseBody(res);
            assertThat(json.path("error").asText()).isEqualTo("Email Not Verified");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  TEST 2 — RBAC Enforcement
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RBAC Enforcement")
    class RbacEnforcement {

        @Test
        @DisplayName("USER accessing /admin/users should get 403")
        void userCannotAccessAdmin() throws Exception {
            register("user@test.com", "Test@1234", "User");
            markUserVerified("user@test.com");

            ResponseEntity<String> loginRes = login("user@test.com", "Test@1234");
            assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
            String token = extractToken(loginRes, "accessToken");

            HttpEntity<Void> request = new HttpEntity<>(bearerHeaders(token));
            ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/v1/admin/users", HttpMethod.GET, request, String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("ADMIN accessing /admin/users should get 200")
        void adminCanAccessAdmin() throws Exception {
            register("admin@test.com", "Test@1234", "Admin");
            markUserVerified("admin@test.com");
            assignRoleToUser("admin@test.com", "ADMIN");

            ResponseEntity<String> loginRes = login("admin@test.com", "Test@1234");
            assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
            String token = extractToken(loginRes, "accessToken");

            HttpEntity<Void> request = new HttpEntity<>(bearerHeaders(token));
            ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/v1/admin/users", HttpMethod.GET, request, String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("No token → 401 for protected endpoint")
        void noTokenUnauthorized() {
            ResponseEntity<String> res = restTemplate.getForEntity(
                baseUrl + "/api/v1/users/me", String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  TEST 3 — Password Change Flow
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Password Change Flow")
    class PasswordChangeFlow {

        @Test
        @DisplayName("Password change → old fails, new works")
        void passwordChange() throws Exception {
            register("pwd@test.com", "Old@1234", "User");
            markUserVerified("pwd@test.com");

            ResponseEntity<String> loginRes = login("pwd@test.com", "Old@1234");
            assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
            String token = extractToken(loginRes, "accessToken");

            String changeBody = """
                {"currentPassword": "Old@1234", "newPassword": "New@5678"}
                """;
            HttpEntity<String> changeReq = new HttpEntity<>(changeBody, bearerHeaders(token));
            ResponseEntity<String> changeRes = restTemplate.exchange(
                baseUrl + "/api/v1/users/me/change-password",
                HttpMethod.POST, changeReq, String.class);

            assertThat(changeRes.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Old password should fail
            assertThat(login("pwd@test.com", "Old@1234").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

            // New password should work
            assertThat(login("pwd@test.com", "New@5678").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Wrong current password → 400")
        void wrongCurrentPassword() throws Exception {
            register("wrong2@test.com", "Old@1234", "User");
            markUserVerified("wrong2@test.com");

            String token = extractToken(login("wrong2@test.com", "Old@1234"), "accessToken");

            String changeBody = """
                {"currentPassword": "Wrong@9999", "newPassword": "New@5678"}
                """;
            HttpEntity<String> changeReq = new HttpEntity<>(changeBody, bearerHeaders(token));
            ResponseEntity<String> changeRes = restTemplate.exchange(
                baseUrl + "/api/v1/users/me/change-password",
                HttpMethod.POST, changeReq, String.class);

            assertThat(changeRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  TEST 4 — Validation
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Invalid email → 400 with fieldErrors")
    void invalidEmail() throws Exception {
        RegisterRequest bad = new RegisterRequest("not-an-email", "Test@1234", "Name");

        ResponseEntity<String> res = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register", bad, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode json = parseBody(res);
        assertThat(json.path("fieldErrors").has("email")).isTrue();
    }

    @Test
    @DisplayName("Weak password → 400 with fieldErrors")
    void weakPassword() throws Exception {
        RegisterRequest bad = new RegisterRequest("valid@test.com", "123", "Name");

        ResponseEntity<String> res = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/register", bad, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode json = parseBody(res);
        assertThat(json.path("fieldErrors").has("password")).isTrue();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER — Parse JSON safely
    // ═══════════════════════════════════════════════════════════

    private JsonNode parseBody(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }
}