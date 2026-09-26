# Universal Auth Backend Template

A **production-grade, plug-and-play** authentication and authorization backend built with **Spring Boot 3** and **Java 21**. Designed as a foundation for any new project — just configure and you're ready to go.

Click **"Use this template"** on GitHub to generate a fresh project with all authentication boilerplate pre-configured.

---

## ✨ Features

### Authentication
- **Local Auth** — Email/password registration and login
- **Google OAuth2** — Sign in with Google
- **GitHub OAuth2** — Sign in with GitHub
- **JWT Access Tokens** — Stateless, HS512-signed
- **Refresh Token Rotation** — New token on every refresh, old revoked
- **HTTP-only Cookies** — Secure refresh token storage (SameSite=Lax)
- **Password Change** — Auto-revokes all refresh tokens for security

### Email Flows
- **Email Verification** — Strict mode (login blocked until verified)
- **Password Reset** — Token-based secure reset
- **Async Sending** — Non-blocking, Thymeleaf HTML templates
- **Mailtrap Support** — Dev-friendly email testing

### Two-Factor Authentication (2FA)
- **TOTP-based** — Google Authenticator / Authy compatible
- **QR Code Setup** — Scan-to-enable flow
- **10 Backup Codes** — BCrypt-hashed, single-use
- **Temp Token Flow** — Login step 1 → 2FA verify step 2

### Security Hardening
- **Rate Limiting** — Bucket4j, per-IP on sensitive endpoints
- **Account Lockout** — Auto-lock after 5 failed logins (15 min)
- **Admin Unlock** — Manual unlock endpoint
- **Session Management** — List/revoke devices, "Sign out other devices"
- **Metadata Tracking** — IP + User-Agent per session

### Authorization (RBAC)
- **Roles** — `USER`, `ADMIN` (built-in) + custom roles
- **Permissions** — Granular `resource:action` format
- **Many-to-Many** — User ↔ Roles ↔ Permissions
- **Method-Level Security** — `@PreAuthorize("hasRole('ADMIN')")`
- **Dual Authority** — `hasRole()` and `hasAuthority()` both supported
- **Built-in Protection** — System roles cannot be deleted

### Architecture
- **Thin Controllers** — Only HTTP handling
- **Fat Services** — All business logic
- **Manual Mappers** — Type-safe, no reflection (ModelMapper removed)
- **Global Exception Handling** — Consistent error responses
- **DTO Separation** — `request/`, `response/`, `error/`, `internal/`
- **Validation** — On all request DTOs
- **Flyway Migrations** — Versioned schema, production-safe
- **OpenAPI Docs** — Swagger UI integrated
- **Health Check** — Spring Boot Actuator
- **Testcontainers** — Real MySQL in integration tests

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security 6.x |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | MySQL 8.x |
| Migrations | Flyway |
| JWT | JJWT 0.12.5 |
| OAuth2 | Spring Security OAuth2 Client |
| 2FA | dev.samstevens.totp + ZXing |
| Email | Spring Mail + Thymeleaf |
| Rate Limiting | Bucket4j |
| Testing | JUnit 5, Mockito, Testcontainers |
| Docs | Springdoc OpenAPI 2.6.0 |
| Boilerplate | Lombok |

---

## 📋 Prerequisites

- **Java 21** or higher
- **Maven** (or use `./mvnw` wrapper)
- **MySQL 8.x** server running
- **Docker** (for integration tests via Testcontainers)
- **Google Cloud** account (for Google OAuth2 — optional)
- **GitHub** account (for GitHub OAuth2 — optional)
- **Mailtrap** account (for email testing — free)

---

## ⚙️ Quick Start

### 1. Create Repository

Click **"Use this template"** → create your new repo → clone it:

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
```

### 2. Database Setup

```sql
CREATE DATABASE authdb;
```

### 3. Configure `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/authdb?useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: YOUR_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate       # Flyway controls schema
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

  mail:
    host: sandbox.smtp.mailtrap.io
    port: 587                    # 587 works when 2525 is blocked
    username: ${MAILTRAP_USERNAME}
    password: ${MAILTRAP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

server:
  port: 8080

universal:
  auth:
    jwt:
      secret: YOUR_64_BYTE_MINIMUM_SECRET_KEY_HERE
      expiration: 3600000                    # 1 hour
      refresh-token-expiration: 604800000    # 7 days
      issuer: universal-auth
    oauth2:
      enabled: false                          # true to enable OAuth2
    cookie:
      refresh-token-name: refresh_token
      secure: false                           # true in production (HTTPS)
      http-only: true
      same-site: Lax
      domain: ""
    frontend:
      success-redirect: http://localhost:3000/dashboard

app:
  rate-limit:
    enabled: true
    cleanup-interval-minutes: 10
  security:
    account-lockout:
      enabled: true
      max-failed-attempts: 5
      lockout-duration-minutes: 15
      reset-counter-after-minutes: 30
      notify-on-lock: true
  2fa:
    issuer: Universal Auth
  mail:
    from: noreply@universal-auth.com
    from-name: Universal Auth
```

> ⚠️ **JWT Secret** must be **at least 64 bytes**. Generate a new one for each project.

### 4. Set Environment Variables

**Option A — `.env` file** (git-ignored):

```bash
MAILTRAP_USERNAME=your-username
MAILTRAP_PASSWORD=your-password
```

**Option B — Terminal export:**

```bash
export MAILTRAP_USERNAME=your-username
export MAILTRAP_PASSWORD=your-password
```

### 5. Build & Run

```bash
# Linux/macOS
./mvnw clean install -DskipTests
./mvnw spring-boot:run

# Windows
mvnw.cmd clean install -DskipTests
mvnw.cmd spring-boot:run
```

### 6. Verify

- **App:** `http://localhost:8080`
- **Swagger:** `http://localhost:8080/swagger-ui.html`
- **Health:** `http://localhost:8080/actuator/health`

**On first run**, Flyway migrations V1-V6 apply and seed:

| Type | Values |
|---|---|
| Roles | `USER`, `ADMIN` |
| Permissions | `user:read`, `user:write`, `user:delete`, `profile:read`, `profile:write`, `role:manage` |

---

## 📚 API Endpoints

### 🔓 Authentication (Public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register new user (auto `USER` role) |
| POST | `/api/v1/auth/login` | Login — returns access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Rotate refresh token |
| POST | `/api/v1/auth/logout` | Logout — clears refresh cookie |
| POST | `/api/v1/auth/verify-email` | Verify email via token |
| POST | `/api/v1/auth/resend-verification` | Resend verification email |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |
| POST | `/api/v1/auth/2fa/verify` | Complete 2FA login (step 2) |

### 👤 User Self-Service

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/users/me` | Authenticated |
| PUT | `/api/v1/users/me` | `profile:write` |
| POST | `/api/v1/users/me/change-password` | `profile:write` |
| GET | `/api/v1/users/me/sessions` | Authenticated |
| DELETE | `/api/v1/users/me/sessions/{id}` | Authenticated |
| DELETE | `/api/v1/users/me/sessions` | Authenticated |

### 🔐 2FA Management

| Method | Endpoint | Required Authority |
|---|---|---|
| POST | `/api/v1/auth/2fa/setup` | Authenticated |
| POST | `/api/v1/auth/2fa/enable` | Authenticated |
| POST | `/api/v1/auth/2fa/disable` | Authenticated |
| GET | `/api/v1/auth/2fa/status` | Authenticated |

### 🛡️ Admin — Users

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/admin/users` | `ROLE_ADMIN` + `user:read` |
| GET | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:read` |
| PUT | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:write` |
| PATCH | `/api/v1/admin/users/{id}/status?enabled=` | `ROLE_ADMIN` + `user:write` |
| DELETE | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:delete` |
| POST | `/api/v1/admin/users/{id}/unlock` | `ROLE_ADMIN` + `user:write` |

### 🛡️ Admin — Roles

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/admin/roles` | `ROLE_ADMIN` + `role:manage` |
| POST | `/api/v1/admin/roles` | `ROLE_ADMIN` + `role:manage` |
| GET | `/api/v1/admin/roles/{id}` | `ROLE_ADMIN` + `role:manage` |
| PUT | `/api/v1/admin/roles/{id}` | `ROLE_ADMIN` + `role:manage` |
| DELETE | `/api/v1/admin/roles/{id}` | `ROLE_ADMIN` + `role:manage` |
| POST | `/api/v1/admin/roles/users/{uid}/assign/{rid}` | `ROLE_ADMIN` + `role:manage` |
| DELETE | `/api/v1/admin/roles/users/{uid}/remove/{rid}` | `ROLE_ADMIN` + `role:manage` |
| GET | `/api/v1/admin/roles/users/{uid}` | `ROLE_ADMIN` + `role:manage` |

### 🛡️ Admin — Permissions

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/admin/permissions` | `ROLE_ADMIN` + `role:manage` |
| POST | `/api/v1/admin/permissions` | `ROLE_ADMIN` + `role:manage` |
| GET | `/api/v1/admin/permissions/{id}` | `ROLE_ADMIN` + `role:manage` |
| PUT | `/api/v1/admin/permissions/{id}` | `ROLE_ADMIN` + `role:manage` |
| DELETE | `/api/v1/admin/permissions/{id}` | `ROLE_ADMIN` + `role:manage` |

---

## 🔐 OAuth2 Setup (Optional)

### Google

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services → Credentials**
2. **Create OAuth 2.0 Client ID** → Web application
3. **Authorized redirect URI:** `http://localhost:8080/login/oauth2/code/google`
4. Copy Client ID + Secret → paste in `application-dev.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_CLIENT_ID.apps.googleusercontent.com
            client-secret: YOUR_CLIENT_SECRET
            scope: [email, profile]
```

### GitHub

1. Go to [GitHub Developer Settings](https://github.com/settings/developers) → **New OAuth App**
2. **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`
3. Copy Client ID + Secret:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
            scope: [read:user, user:email]
```

### Enable OAuth2

```yaml
universal:
  auth:
    oauth2:
      enabled: true
```

**Test:**
- Google: `http://localhost:8080/oauth2/authorization/google`
- GitHub: `http://localhost:8080/oauth2/authorization/github`

---

## 🔐 2FA (TOTP) Setup

### Enable 2FA

1. **Setup** — `POST /api/v1/auth/2fa/setup`
   - Returns: `secret`, `qrCodeDataUri`, `otpAuthUrl`
2. **Add to Authenticator** — Scan QR with Google Authenticator
3. **Verify** — `POST /api/v1/auth/2fa/enable` with the 6-digit code
   - Returns: 10 backup codes (save them securely!)

### Login with 2FA

1. **Step 1** — `POST /api/v1/auth/login` with email/password
   - Returns: `{requiresTwoFactor: true, tempToken}`
2. **Step 2** — `POST /api/v1/auth/2fa/verify` with `tempToken` + 6-digit code
   - Returns full tokens

### Backup Codes

- 10 single-use codes, format `XXXX-XXXX-XXXX`
- BCrypt-hashed in DB
- Can be used in place of TOTP code
- Consumed on use, tracked with `used_at` timestamp

---

## 🚦 Rate Limiting

Per-IP rate limiting using **Bucket4j** (Token Bucket algorithm).

| Endpoint | Limit |
|---|---|
| `POST /register` | 3/hour |
| `POST /login` | 5/15min |
| `POST /refresh` | 30/min |
| `POST /forgot-password` | 5/hour |
| `POST /resend-verification` | 3/hour |
| `POST /2fa/verify` | 10/15min |

**Response on limit exceeded:** `429 Too Many Requests` with `Retry-After` header.

**Disable in tests:**
```yaml
app:
  rate-limit:
    enabled: false
```

---

## 🔒 Account Lockout

Automatic account lockout after **5 failed login attempts** (configurable).

| Config | Default |
|---|---|
| `max-failed-attempts` | 5 |
| `lockout-duration-minutes` | 15 |
| `reset-counter-after-minutes` | 30 |

**Response:** `423 Locked` with `X-Account-Locked-Until` header.

**Reset on:** successful login, password change, password reset.

**Admin unlock:** `POST /api/v1/admin/users/{id}/unlock`

---

## 📱 Session Management

Users can view and manage their active sessions (devices).

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/users/me/sessions` | List all active sessions |
| DELETE | `/api/v1/users/me/sessions/{id}` | Revoke a specific session |
| DELETE | `/api/v1/users/me/sessions` | Sign out other devices |

**Session metadata tracked:** IP address, User-Agent, last used timestamp.

**Current session** is identified via `refreshJti` claim in the access token.

---

## 🎯 RBAC — How It Works

### Structure

```
User ──M:N──> Role ──M:N──> Permission
```

### Authority Format

| Type | DB Value | Authority String |
|---|---|---|
| Role | `ADMIN` | `ROLE_ADMIN` |
| Role | `USER` | `ROLE_USER` |
| Permission | `user:read` | `user:read` |

### Controller Usage

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(...) {}

@PreAuthorize("hasAuthority('user:read')")
public UserResponse getUser(...) {}

@PreAuthorize("hasRole('ADMIN') and hasAuthority('user:read')")
public List<UserResponse> listUsers() {}
```

### Permission Naming Convention

Use `resource:action` format:

| Resource | Read | Write | Delete | Manage |
|---|---|---|---|---|
| `user` | `user:read` | `user:write` | `user:delete` | `user:manage` |
| `profile` | `profile:read` | `profile:write` | — | — |
| `role` | — | — | — | `role:manage` |

---

## 🗄️ Database Migrations (Flyway)

| Version | Description |
|---|---|
| `V1` | Initial schema — users, roles, permissions, refresh_tokens |
| `V2` | Seed RBAC base data |
| `V3` | Email verification (email_verified, verification_tokens) |
| `V4` | 2FA support (totp_secret, backup_codes) |
| `V5` | Account lockout (failed_login_attempts, locked_until) |
| `V6` | Session metadata (ip_address, user_agent, last_used_at) |

**Migrations are immutable** — never modify a migration after it's applied to production.

---

## 🧪 Testing

### Run All Tests

```bash
./mvnw test
```

**Expected:** `Tests run: 266, Failures: 0, Errors: 0`

### Test Coverage

```bash
./mvnw jacoco:report
firefox target/site/jacoco/index.html
```

### Integration Tests (Testcontainers)

```bash
# Requires Docker running
./mvnw test -Dtest=AuthIntegrationTest
```

**Note:** Test profile disables rate limiting + account lockout to prevent interference.

---

## 📁 Project Structure

```
src/main/java/com/auth_app_backend/
├── config/                            # Security, Flyway, Swagger, RBAC seeding
├── controllers/                       # REST controllers (thin)
├── dto/
│   ├── request/                       # Input DTOs (validated)
│   ├── response/                      # Output DTOs
│   ├── error/                         # ApiError
│   └── internal/                      # Service-to-service DTOs
├── entity/                            # JPA entities
├── exception/                         # Custom exceptions + handler
├── helper/                            # Utilities
├── mapper/                            # Manual entity ↔ DTO mappers
├── ratelimit/                         # Bucket4j rate limiting
├── repositories/                      # Spring Data JPA
├── security/                          # JWT, cookies, filters, OAuth2
└── services/                          # Business logic
```

---

## 🔒 Security Features Summary

| Feature | Implementation |
|---|---|
| Password Hashing | BCrypt (strength 10) |
| JWT Algorithm | HS512 (512-bit) |
| Access Token Expiry | 1 hour |
| Refresh Token Expiry | 7 days |
| Refresh Token Rotation | Every refresh, old revoked |
| Password Change | Revokes all refresh tokens |
| Cookie Flags | HttpOnly, SameSite=Lax, Secure (prod) |
| Session Policy | STATELESS |
| CSRF | Disabled (stateless JWT) |
| Error Messages | Generic (no info leak) |
| Input Validation | On all request DTOs |
| Built-in Roles | Protected from deletion |
| 2FA | TOTP + 10 backup codes |
| Rate Limiting | Bucket4j per-IP |
| Account Lockout | 5 attempts → 15 min lock |
| Session Management | Per-device tracking + revocation |

---

## 🚀 Production Checklist

Before deploying to production:

- [ ] **JWT Secret**: Generate a strong 64+ byte random value
  ```bash
  openssl rand -base64 64
  ```
- [ ] **Environment Variables**: Move secrets to env vars (not in YAML)
- [ ] **Database**: `ddl-auto: validate` (Flyway controls schema)
- [ ] **Cookies**: `universal.auth.cookie.secure: true`
- [ ] **CORS**: Configure for your frontend domain
- [ ] **HTTPS**: Enforce HTTPS with valid certificate
- [ ] **Rate Limiting**: `app.rate-limit.enabled: true`
- [ ] **Account Lockout**: `app.security.account-lockout.enabled: true`
- [ ] **Email Provider**: Switch from Mailtrap to SendGrid/Mailgun/SES
- [ ] **Logging**: Structured JSON logs
- [ ] **Monitoring**: Actuator + Micrometer + Prometheus
- [ ] **Backup**: Database backup strategy
- [ ] **Session Cleanup**: Scheduled job for expired tokens

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m "feat: add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

**Conventional Commits** preferred: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`

---

## 📝 License

MIT License — free to use in personal and commercial projects.

---

## 🙋 Author

**Shahrayar Ali**
- 🌐 [shahrayarali.me](https://www.shahrayarali.me/)
- 💼 [LinkedIn](https://www.linkedin.com/in/shahrayar-sahito-269117360/)
- 🐙 [GitHub](https://github.com/shahry-03)
- 📧 [shahrayarsahito7@gmail.com](mailto:shahrayarsahito7@gmail.com)

---

**Built with ❤️ as a reusable template for the Spring Boot community.**