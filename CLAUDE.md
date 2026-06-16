# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (HTTPS on port 8443)
./mvnw spring-boot:run

# Build (produces target/skillswap-0.0.1-SNAPSHOT.jar)
./mvnw clean package

# Run tests
./mvnw test

# Compile only
./mvnw compile
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Architecture

**SkillSwap** is a Spring Boot 3.4.3 REST API (Java 17) for a skill-sharing/mentorship platform. It also serves static HTML pages as its frontend via `src/main/resources/static/`.

### Package structure (flat, under `com.example.skillswap`)

All classes live in a single flat package — no sub-packages by feature:
- `*Controller.java` — 20 REST controllers, all under `/api/**`
- `*Service.java` — 18 business logic services
- `*Repository.java` — 9 Spring Data JPA repositories
- Model/entity classes, DTOs, enums, config, validators all at the same level

### Core data model

| Entity | Key fields |
|---|---|
| `User` | roles (`LEARNER`/`MENTOR`/`SPONSOR`), `credits`, `reputation`, `mfaEnabled`/`mfaSecret`, `resetCode` |
| `Session` | `mentor`, `learner`, `skill`, `status` (`OPEN→PENDING→SCHEDULED→COMPLETED/CANCELLED`), `aiSummary` |
| `UserSkill` | bridges `User↔Skill` with `type` (`LEARN`/`MENTOR`) and `level` (`BEGINNER`→`EXPERT`) |
| `MentorAvailability` | weekly recurring or specific-date slots |
| `OtpRequest` | email OTP for signup and password reset |
| `AuditLog` | security audit trail |

### Security & Auth

- **JWT** (24-hour expiry, HMAC-SHA256) validated in `AuthTokenFilter` on every request
- **HTTPS** only — port 8443, PKCS12 keystore at `src/main/resources/keystore.p12`
- **MFA** — TOTP via `dev.samstevens.totp` + `com.warrenstrange:googleauth`; fields on `User`
- **CORS** — configured in `SecurityConfig` for `https://localhost:*`
- **Input sanitization** — custom `InputValidationConfig` using OWASP HTML Sanitizer; all user-supplied HTML is stripped before persistence
- **Password hashing** — BCrypt via Spring Security

### Configuration

All runtime config lives in `src/main/resources/application.properties`:
- **Database:** MySQL at `localhost:3306/skillswapuae`, user `capstone_user`, `ddl-auto=update`
- **JWT secret** and expiry properties read by `JwtProperties`
- **Mail:** Gmail SMTP (credentials currently hardcoded — should be externalized)
- **Zoom / OpenAI keys** present but integrations are partially stubbed

### Third-party integrations

- **OpenAI** (`AiSummaryService`) — session summarization; key is a placeholder in current config
- **Zoom** (`ZoomService`) — meeting link generation; credentials are placeholders
- **Gmail SMTP** (`EmailService`) — OTP emails, session confirmations, password reset

### Frontend

Static HTML files in `src/main/resources/static/` (login, register, dashboard, browse-skills, sessions, etc.) are served directly by Spring Boot. There is no separate frontend build step or bundler.

### Key patterns

- **`@Transactional`** on service methods; repositories are plain `JpaRepository` extensions
- **`@PrePersist`/`@PreUpdate`** on entities for `createdAt`/`updatedAt` timestamps
- **DTOs** are plain POJOs passed between controllers and services (no MapStruct)
- **Exceptions** — `ResourceNotFoundException` (custom) is the primary domain exception
- **Logging** — SLF4J via `@Slf4j` (Lombok)
