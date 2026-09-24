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
- **OpenAPI Docs** — Swagger UI integrated
- **Health Check** — Spring Boot Actuator

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security 6.x |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | MySQL |
| JWT | JJWT 0.12.5 |
| OAuth2 | Spring Security OAuth2 Client |
| Docs | Springdoc OpenAPI 2.6.0 |
| Boilerplate | Lombok |

---

## 📋 Prerequisites

- **Java 21** or higher
- **Maven** (or use `./mvnw` wrapper)
- **MySQL 8.x** server running
- **Google Cloud** account (for Google OAuth2 — optional)
- **GitHub** account (for GitHub OAuth2 — optional)

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
      ddl-auto: update
    show-sql: true

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
```

> ⚠️ **JWT Secret** must be **at least 64 bytes**. Generate a new one for each project.

### 4. Build & Run

```bash
# Linux/macOS
./mvnw clean install -DskipTests
./mvnw spring-boot:run

# Windows
mvnw.cmd clean install -DskipTests
mvnw.cmd spring-boot:run
```

### 5. Verify

- **App:** `http://localhost:8080`
- **Swagger:** `http://localhost:8080/swagger-ui.html`
- **Health:** `http://localhost:8080/actuator/health`

On first run, `RbacDataInitializer` seeds:

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

### 👤 User Self-Service

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/users/me` | Authenticated |
| PUT | `/api/v1/users/me` | `profile:write` |
| POST | `/api/v1/users/me/change-password` | `profile:write` |

### 🛡️ Admin — Users

| Method | Endpoint | Required Authority |
|---|---|---|
| GET | `/api/v1/admin/users` | `ROLE_ADMIN` + `user:read` |
| GET | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:read` |
| PUT | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:write` |
| PATCH | `/api/v1/admin/users/{id}/status?enabled=` | `ROLE_ADMIN` + `user:write` |
| DELETE | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` + `user:delete` |

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

## 🎯 RBAC — How It Works

### Structure

```
User ──M:N──> Role ──M:N──> Permission
```

- A **User** can have multiple **Roles**
- A **Role** can have multiple **Permissions**
- **Effective authorities** = union of all roles + all permissions

### Authority Format

| Type | DB Value | Authority String |
|---|---|---|
| Role | `ADMIN` | `ROLE_ADMIN` |
| Role | `USER` | `ROLE_USER` |
| Permission | `user:read` | `user:read` |
| Permission | `profile:write` | `profile:write` |

The `ROLE_` prefix is **added automatically** in `User.getAuthorities()`.

### Controller Usage

```java
// Role check
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(...) {}

// Permission check
@PreAuthorize("hasAuthority('user:read')")
public UserResponse getUser(...) {}

// Combined (recommended for admin endpoints)
@PreAuthorize("hasRole('ADMIN') and hasAuthority('user:read')")
public List<UserResponse> listUsers() {}

// Multiple roles (OR)
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public void moderate(...) {}

// Owner or Admin
@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
public UserResponse getUser(@PathVariable UUID id) {}
```

### Permission Naming Convention

Use `resource:action` format:

| Resource | Read | Write | Delete | Manage |
|---|---|---|---|---|
| `user` | `user:read` | `user:write` | `user:delete` | `user:manage` |
| `profile` | `profile:read` | `profile:write` | — | — |
| `role` | — | — | — | `role:manage` |
| `product` | `product:read` | `product:write` | `product:delete` | `product:manage` |

### Adding Custom Roles (in consuming project)

```java
// In your RbacDataInitializer or a migration
Role shopOwner = Role.builder()
    .roleName("SHOP_OWNER")
    .description("Shop owner role")
    .permissions(Set.of(
        permissionRepository.findByName("user:read").orElseThrow(),
        permissionRepository.findByName("product:write").orElseThrow()
    ))
    .build();
roleRepository.save(shopOwner);
```

---

## 🧪 Testing with Postman

### Import Collection

1. Open Postman
2. **Import** → select `docs/universal-auth.postman_collection.json`
3. **Import** → select `docs/auth-dev.postman_environment.json`
4. Select **"Auth Dev"** environment (top-right)

### Environment Variables

| Variable | Value | Notes |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Change if using different port |
| `accessToken` | *(empty)* | Auto-filled on login |
| `refreshToken` | *(empty)* | Auto-filled on login |
| `userId` | *(empty)* | Auto-filled on register |
| `permissionId` | *(empty)* | Auto-filled on permission create |

### Run Order

1. **Register User** → creates user, saves `userId`
2. **Login** → saves `accessToken`, `refreshToken`
3. **Get My Profile** → uses access token
4. **Admin requests** → require ADMIN role

### Make a User Admin

```sql
USE authdb;

INSERT INTO user_roles (user_id, role_id)
SELECT 
    (SELECT user_id FROM users WHERE user_email = 'your@email.com'),
    (SELECT role_id FROM roles WHERE role_name = 'ADMIN');
```

Logout → login again → new token includes `ROLE_ADMIN`.

---

## 📁 Project Structure

```
src/main/java/com/auth_app_backend/
├── config/
│   ├── APIDocConfig.java              # Swagger/OpenAPI config
│   ├── AppConstants.java              # Public URLs constants
│   ├── ProjectConfig.java             # General beans
│   ├── RbacDataInitializer.java       # Seeds roles + permissions
│   ├── SecurityConfig.java            # Security filter chain
│   └── UniversalAuthProperties.java   # @ConfigurationProperties
│
├── controllers/
│   ├── AuthController.java            # Public auth endpoints
│   ├── UserController.java            # Self-service endpoints
│   ├── AdminUserController.java       # Admin user CRUD
│   ├── AdminRoleController.java       # Admin role CRUD + assignment
│   └── AdminPermissionController.java # Admin permission CRUD
│
├── dto/
│   ├── request/                       # Input DTOs (validated)
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── UpdateUserRequest.java
│   ├── response/                      # Output DTOs
│   │   ├── UserResponse.java
│   │   ├── RoleResponse.java
│   │   ├── PermissionResponse.java
│   │   ├── TokenResponse.java
│   │   └── ApiResponse.java
│   ├── error/
│   │   └── ApiError.java              # Standard error response
│   └── internal/
│       └── InternalUserResponse.java  # Service-to-service only
│
├── entity/
│   ├── User.java                      # Implements UserDetails
│   ├── Role.java
│   ├── Permission.java
│   ├── RefreshToken.java
│   └── Provider.java                  # enum: LOCAL, GOOGLE, GITHUB, FACEBOOK
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
│
├── helper/
│   └── UserHelper.java
│
├── mapper/                            # Manual mappers (type-safe)
│   ├── UserMapper.java
│   ├── RoleMapper.java
│   └── PermissionMapper.java
│
├── repositories/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   └── RefreshTokenRepository.java
│
├── security/
│   ├── CookieService.java             # HTTP-only cookie management
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java                # Token generation/validation
│   ├── Oauth2SuccessHandler.java      # Google/GitHub callback
│   └── RefreshTokenExtractor.java     # Multi-source token extractor
│
└── services/
    ├── AuthService.java               # register/login/refresh/logout
    ├── UserService.java               # user CRUD
    ├── RoleService.java               # role CRUD + assignment
    ├── PermissionService.java         # permission CRUD
    ├── RefreshTokenService.java       # token lifecycle
    └── impl/                          # Implementations
```

---

## 🔒 Security Features

| Feature | Implementation |
|---|---|
| Password Hashing | BCrypt (strength 10) |
| JWT Algorithm | HS512 (512-bit) |
| Access Token Expiry | 1 hour (configurable) |
| Refresh Token Expiry | 7 days (configurable) |
| Refresh Token Rotation | On every refresh, old revoked |
| Password Change | Revokes all refresh tokens |
| Cookie Flags | HttpOnly, SameSite=Lax, Secure (prod) |
| Session Policy | STATELESS (no server sessions) |
| CSRF | Disabled (stateless JWT) |
| Error Messages | Generic (no info leak) |
| Input Validation | On all request DTOs |
| Built-in Roles | Protected from deletion |
| Logout | Clears cookie + revokes token |

---

## 🚀 Production Checklist

Before deploying to production:

- [ ] **JWT Secret**: Generate a strong 64+ byte random value
  ```bash
  openssl rand -base64 64
  ```
- [ ] **Environment Variables**: Move secrets to env vars (not in YAML)
- [ ] **Database**: Set `ddl-auto: validate` (use Flyway/Liquibase for migrations)
- [ ] **Cookies**: Set `universal.auth.cookie.secure: true`
- [ ] **CORS**: Configure for your frontend domain
- [ ] **HTTPS**: Enforce HTTPS with valid certificate
- [ ] **Rate Limiting**: Add on `/auth/login`, `/auth/refresh`
- [ ] **Logging**: Structured JSON logs, appropriate levels
- [ ] **Monitoring**: Enable Actuator endpoints + Micrometer
- [ ] **OAuth2 Credentials**: Use production redirect URIs
- [ ] **Email Verification**: Add for registration (optional)
- [ ] **Password Reset**: Implement forgot-password flow (optional)
- [ ] **Backup**: Database backup strategy
- [ ] **Docker**: Containerize for deployment (optional)

---

## 🧭 Common Tasks

### Add a New Permission

```bash
POST /api/v1/admin/permissions
Authorization: Bearer <admin-token>
{
  "name": "order:read",
  "description": "Read order details"
}
```

### Add a New Role

```bash
POST /api/v1/admin/roles
Authorization: Bearer <admin-token>
{
  "roleName": "ORDER_MANAGER",
  "description": "Manages orders",
  "permissions": ["order:read", "user:read"]
}
```

### Assign Role to User

```bash
POST /api/v1/admin/roles/users/{userId}/assign/{roleId}
Authorization: Bearer <admin-token>
```

### Protect a New Endpoint

```java
@GetMapping("/api/v1/orders")
@PreAuthorize("hasAuthority('order:read')")
public List<OrderResponse> listOrders() { ... }
```

---

## 📖 API Documentation

Interactive Swagger UI:
- **UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Click **Authorize** (top-right) → paste `Bearer <accessToken>` → test protected endpoints.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m "Add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

---

## 📝 License

MIT License — free to use in personal and commercial projects.

---

## 🙋 Author

**Shahrayar Ali**
- 🌐 [shahrayarali.me](https://www.shahrayarali.me/)
- 📧 [shahrayarsahito7@gmail.com](mailto:shahrayarsahito7@gmail.com)

---

**Built with ❤️ as a reusable template for the Spring Boot community.**