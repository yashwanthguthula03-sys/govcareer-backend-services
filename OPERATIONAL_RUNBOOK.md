# Operational Runbook - GovCareer Auth Service

## Overview
This runbook provides the necessary operational procedures for the GovCareer Auth Service.

## 1. Metrics & Monitoring
The application exposes Prometheus-compatible metrics via Spring Boot Actuator and Micrometer.
- **Endpoint:** `GET /actuator/prometheus`
- **Key Metrics:**
  - `auth.login.timer`: Latency and throughput of the login endpoint.
  - `auth.register.timer`: Latency and throughput of the registration endpoint.
  - `auth.refresh.timer`: Latency and throughput of the refresh token generation.
  - `auth.logout.timer`: Latency and throughput of the logout process.
  - `user.profile.update.timer`: Latency of profile updates.
  - `user.password.change.timer`: Latency of password changes.
  - `hikaricp.connections.*`: Database connection pool metrics.

## 2. Health & Liveness
The application provides health endpoints for Kubernetes readiness and liveness probes.
- **Endpoint:** `GET /actuator/health`
- **Checks Included:**
  - `db`: Verifies connectivity to the PostgreSQL database.
  - `ping`: Verifies the application is running.

### 3. Application Logging

The Auth Service uses `logstash-logback-encoder` to output 100% structured JSON logs.

#### Correlation IDs
Every incoming HTTP request is assigned a `correlationId`. This ID is injected into the SLF4J MDC and will appear in every log statement generated during that request's lifecycle.

Example Log Entry:
```json
{
  "timestamp": "2023-10-27T10:00:00.000Z",
  "level": "INFO",
  "logger": "com.govcareer.auth.service.AuthService",
  "message": "User authenticated successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "endpoint": "/api/auth/login"
}
```

### 4. Account Lockouts & Verification
Accounts that exceed 5 failed login attempts will be temporarily locked for 15 minutes. This lock state is stored in the `users.locked_until` column.
Accounts must have `users.email_verified = true` to log in. Verification tokens are stored in `email_verification_tokens`.

To manually unlock an account in an emergency:
```sql
UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE email = 'user@example.com';
```

## 5. Audit Logging
Sensitive business operations are durably logged to the `auth_audits` PostgreSQL table.
These are completely isolated from system logs to ensure immutable business records.
- **Events Tracked:** `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `REGISTRATION_SUCCESS`, `REGISTRATION_FAILURE`, `PASSWORD_CHANGED`, `PROFILE_UPDATED`, `TOKEN_REFRESH`, `LOGOUT`.
- **Querying Audits:** Connect to PostgreSQL and query `SELECT * FROM auth_audits WHERE user_id = ? ORDER BY timestamp DESC;`

## 5. Troubleshooting Scenarios

### 5.1 "Database Connection Failure" on Startup
- **Symptoms:** Application fails to start; logs display `HikariPool-1 - Exception during pool initialization`.
- **Action:** 
  1. Verify the `.env` file contains correct `POSTGRES_USER` and `POSTGRES_PASSWORD`.
  2. Check if the database container is healthy: `docker ps`.
  3. Verify port `5432` is not occupied by a local PostgreSQL installation.

### 5.2 High Latency on Login Endpoint
- **Symptoms:** `auth.login.timer` in Prometheus spikes over 500ms.
- **Action:** 
  1. Login relies heavily on BCrypt hashing. High latency typically means CPU exhaustion.
  2. Scale up the CPU allocation for the container or add more replicas.

### 5.3 Missing Logs for a User Report
- **Symptoms:** A user reports an issue but you cannot find it in the logs.
- **Action:** Ask the frontend team to provide the `X-Correlation-ID` from the response headers of the failed request. Search the centralized logging platform (e.g., ELK, Datadog) using `"correlationId": "<ID>"`.
