# Universal Auth App - Spring Boot Template

A ready-to-use **Template Repository** for building robust and secure backend services, built with **Spring Boot 3** and **Java 21**. This project is designed to be the foundation for your new applications, providing a complete, out-of-the-box implementation for JWT-based authentication, token refreshment, role-based access control, and OAuth2 readiness.

Simply click **"Use this template"** on GitHub to generate a brand new project with all this boilerplate code pre-configured!

## 🚀 Features

- **User Registration & Login**: Secure user onboarding and authentication.
- **JWT Authentication**: Stateless authentication using JSON Web Tokens.
- **Refresh Tokens**: Secure mechanism to refresh expired access tokens with rotation support.
- **Cookie-based Tokens**: Best practice implementation for storing refresh tokens securely in `HttpOnly` cookies.
- **Role-Based Access Control (RBAC)**: Support for user roles and permissions.
- **OAuth2 Ready**: Configured for OAuth2 client capabilities (Social login).
- **RESTful API**: Clean and well-structured endpoints for user management.
- **API Documentation**: Integrated with Springdoc OpenAPI (Swagger UI).

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot 3.3.5**
  - Spring Web
  - Spring Security
  - Spring Data JPA
- **Database**: MySQL
- **Authentication**: JWT (`io.jsonwebtoken`), OAuth2 Client
- **Utilities**: Lombok, ModelMapper

## 📋 Prerequisites

Before you begin, ensure you have met the following requirements:
- **Java Development Kit (JDK) 21** or higher.
- **Maven** (or use the provided Maven wrapper `mvnw`).
- **MySQL Server** installed and running.

## ⚙️ Getting Started

### 1. Generate Your Project

1. Click the green **"Use this template"** button at the top of the GitHub repository page to create a new repository.
2. Clone your newly created repository:
```bash
git clone <your-new-repository-url>
cd <your-new-repo-name>
```

### 2. Configure the Database

1. Navigate to the resources directory: `src/main/resources/`.
2. Copy the `application.template.yaml` file and rename it to `application-dev.yaml`.
3. Open `application-dev.yaml` and update the database credentials with your local MySQL setup:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db_name
    username: your_mysql_username
    password: your_mysql_password
```

*Note: The `application.yaml` is pre-configured to use the `dev` profile, which automatically loads `application-dev.yaml`.*

### 3. Build the Project

Use Maven to build the project and download all dependencies:

```bash
# On Linux/macOS
./mvnw clean install -DskipTests

# On Windows
mvnw.cmd clean install -DskipTests
```

### 4. Run the Application

You can start the Spring Boot application using the Maven Spring Boot plugin:

```bash
# On Linux/macOS
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

The server will start on port `8080` by default.

## 📚 API Endpoints

Once the application is running, you can access the following REST APIs. 

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/register` | Register a new user |
| `POST` | `/login` | Authenticate a user and get tokens |
| `POST` | `/refresh` | Get a new access token using a refresh token |
| `POST` | `/logout` | Logout user and invalidate tokens |

### Users (`/api/v1/users`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Create a new user (Admin) |
| `GET`  | `/` | Get all users |
| `GET`  | `/{userId}` | Get a user by ID |
| `GET`  | `/email/{email}` | Get a user by Email |
| `PUT`  | `/{userId}` | Update a user by ID |
| `DELETE`| `/{userId}`| Delete a user by ID |

## 📖 API Documentation (Swagger)

Since the project includes `springdoc-openapi`, you can explore and test the APIs directly from your browser once the server is running:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

## 📁 Project Structure

```
src/main/java/com/auth_app_backend/
├── config/         # Application configurations (Security, Beans, etc.)
├── controllers/    # REST API endpoints (AuthController, UserController)
├── dtos/           # Data Transfer Objects for API requests/responses
├── entity/         # JPA Entities (User, Role, RefreshToken, Provider)
├── exception/      # Global exception handling mechanisms
├── helper/         # Utility classes and helpers
├── repositories/   # Spring Data JPA repositories
├── security/       # JWT filters, Cookie services, and custom security logic
└── services/       # Business logic implementations
```
