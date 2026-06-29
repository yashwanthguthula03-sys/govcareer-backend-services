# GovCareer - Secure Authentication API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Security-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)

## 🏗️ Architecture Overview

The GovCareer Authentication API is built on a highly secure, zero-trust backend architecture enforcing strict separation of concerns and elite deployment standards.

* **Dual-Token JWT Architecture:** Implements a robust authentication flow utilizing short-lived, stateless **Access Tokens** (15-minute expiry) for high-performance route protection, and long-lived, stateful **Refresh Tokens** (7-day expiry) stored securely in the database to maintain session persistence without compromising security.
* **Cryptographic Security:** Utilizes **BCrypt** hashing for immutable, salted password encryption.
* **Global Exception Handling:** A centralized `@RestControllerAdvice` intercepts all application exceptions (e.g., Validation, Unauthorized, Forbidden), guaranteeing consistent, clean JSON error schemas instead of stack traces.
* **Role-Based Access Control (RBAC):** Strict method-level security (`@PreAuthorize`) mapping specific endpoints to distinct organizational roles.

## 🛠️ Tech Stack

* **Java 21**
* **Spring Boot 3**
* **Spring Security 6**
* **PostgreSQL**
* **Docker & Docker Compose**
* **Swagger (OpenAPI 3.0)**

## 🚀 Quick Start (Docker)

This service is deployed as an immutable containerized artifact. You do not need to install PostgreSQL or configure local environments manually.

1. **Configure the Environment:**
   ```bash
   cp .env.example .env
   ```
2. **Build the Application JAR:**
   ```bash
   ./mvnw clean package -DskipTests
   ```
3. **Spin up the Ecosystem:**
   ```bash
   docker-compose up -d --build
   ```

> [!IMPORTANT]
> The Docker ecosystem **must be actively running** to access the application endpoints, database, or email server locally.

The application will autonomously boot on `http://localhost:8080`, successfully linking to the orchestrated PostgreSQL database.

## 📚 API Reference

Comprehensive, interactive API documentation is available at **[`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html)** once the server is running. You can inject the JWT `Bearer` token directly into the Swagger UI.

Additionally, a local MailHog instance captures all outgoing emails (e.g., verification, password resets) at **[`http://localhost:8025`](http://localhost:8025)**.

| Endpoint | HTTP Method | Required Role | Description |
| :--- | :---: | :---: | :--- |
| `/api/auth/register` | `POST` | **Public** | Register a new user |
| `/api/auth/login` | `POST` | **Public** | Authenticate & retrieve JWTs |
| `/api/auth/refresh` | `POST` | **Public** | Exchange a refresh token for a new access token |
| `/api/auth/resend-verification` | `POST` | **Public** | Resend the email verification link |
| `/api/user/profile` | `GET` | **User** / **Admin** | Access the secure user profile |
| `/api/admin/dashboard`| `GET` | **Admin** | Access the secured administrative dashboard |

## 📁 Project Structure

```text
src/main/java/com/govcareer/auth/
├── config/              # Security config, Swagger config, Password encoders
├── controller/          # REST Endpoints (AuthController, UserController, AdminController)
├── dto/                 # Data Transfer Objects (Requests, Responses, Errors)
├── entity/              # JPA Database Entities (User, RefreshToken, Role)
├── exception/           # GlobalException handler and custom Exceptions
├── repository/          # Spring Data JPA Repositories
├── security/            # JWT Filters, JwtService, RefreshTokenService
└── service/             # Core Business Logic (AuthService)
```
