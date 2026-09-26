# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Job Tracker is a Spring Boot 4 REST API (Java 21) for tracking job applications, backed by PostgreSQL with Flyway migrations.

## Commands

Build tool is Gradle (via wrapper — use `gradlew`/`gradlew.bat`, not a system-installed `gradle`).

```bash
# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.jobtracker.JobTrackerApplicationTests"

# Run a single test method
./gradlew test --tests "com.example.jobtracker.JobTrackerApplicationTests.contextLoads"

# Run the app locally (requires the DB, see below)
./gradlew bootRun
```

### Database

The app expects PostgreSQL reachable at the URL in `src/main/resources/application.yml` (`localhost:5433/job_tracker`). It also expects Redis reachable at `localhost:6380` (used to cache `GET /api/job-applications/{id}` lookups for 30 seconds — see `config/CacheConfig.java`). Start both with:

```bash
docker-compose up -d db redis minio
```

Schema is managed exclusively through Flyway migrations in `src/main/resources/db/migration/` (`V1__...sql`, `V2__...sql`, ...). Hibernate DDL is set to `validate` (`spring.jpa.hibernate.ddl-auto=validate`) — it never auto-generates schema, so any entity change requires a corresponding new Flyway migration.

### Full stack via Docker

```bash
docker-compose up --build
```

This builds the app in a multi-stage Docker build (Gradle build inside the `build` stage, then copies the jar into a JRE-only runtime image) and runs it alongside Postgres. The app container talks to the `db` service over the Docker network; the app is exposed on port 8080, Postgres on `5433` (host) by default.

## Architecture

Standard layered Spring MVC structure under `com.example.jobtracker`:

- `controller/` — `@RestController` classes; request/response DTOs live in `controller/dto/` as Java records. Controllers validate input (`@Valid`) and delegate all logic to services — no business logic in controllers.
- `service/` — business logic; converts between entities and DTOs (`toResponse` mapping done manually, not with a mapping library). Not-found cases are signaled via `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` rather than custom exception classes.
- `persistence/` — Spring Data JPA repositories (`JpaRepository` interfaces); `persistence/entity/` holds `@Entity` classes using Lombok (`@Getter`/`@Setter`/`@NoArgsConstructor`/`@AllArgsConstructor`) instead of hand-written boilerplate.

Request flow: `Controller` → `Service` (business logic + entity/DTO mapping) → `Repository` (JPA) → Postgres. Resources: job applications (`/api/job-applications`), users (`/api/users`) and companies (`/api/companies`); follow the same three-layer pattern (controller/dto, service, persistence/entity) when adding new resource types.

### Users and companies

`User` ↔ `Company` is many-to-many through the `user_companies` join table (`User` owns the `@ManyToMany`; `Company` has no back-reference). Business rules enforced in the services, not the DB: users are created without companies (`BUSINESS` users get them afterwards via referral codes, see below), an `INDIVIDUAL` user never has any; a company that is some business user's only company cannot be deleted (409). User email and company name are unique case-insensitively (`lower(...)` unique indexes). Users and companies are not linked to job applications. Neither resource is exposed through the gateway yet.

### Referral codes

An existing user joins a company by code: `POST /api/companies/{id}/referral-codes` issues a random code (`ReferralCodeGenerator`, `SecureRandom`), `POST /api/users/{id}/companies/join` redeems it. Codes are reusable until they expire; `INDIVIDUAL` users cannot join (409). Storage is selected by `referral.storage` in `application.yml` (env `REFERRAL_STORAGE`, restart required) — exactly one `ReferralCodeStore` bean exists, chosen via `@ConditionalOnProperty`:

- `redis` — `RedisReferralCodeStore`: key `referral:code:{CODE}` → companyId, `SET NX EX`, Redis TTL does the expiry.
- `database` — `DatabaseReferralCodeStore`: `referral_codes` table (V5; created regardless of mode), insert via `ON CONFLICT DO NOTHING`; `ReferralCodeCleanupJob` deletes expired rows on `referral.cleanup-cron`, and lookups also filter on `expires_at` since cleanup lags.

Codes are not migrated when switching modes.

### User avatars

`PUT /api/users/{id}/avatar` (multipart, part `file`) uploads an avatar, `GET` returns temporary presigned links to MinIO (the bytes never go through this service), `DELETE` removes it. Limits: 5 MB (`spring.servlet.multipart.max-file-size`), JPEG/PNG/WebP only (detected from file content by ImageIO; WebP via TwelveMonkeys), max 4096 px per side. `AvatarImageProcessor` makes a 256×256 center-cropped thumbnail (Thumbnailator). Files live in MinIO (bucket `avatars`, created on startup, fixed keys `avatars/{userId}/original` and `avatars/{userId}/thumbnail`, so a new upload overwrites the old files; `AvatarStorage` is a plain class, no interface), metadata in the `user_avatars` table (V6). `avatar.s3.endpoint` is used for uploads, `avatar.s3.public-endpoint` for building client links (the host is part of the signature).

### Related service: job-tracker-gateway

Bulk seeding (`POST /api/job-applications/seed`), the public paginated/filtered listing and the web UI (static `index.html`/`app.js`) live in a separate project, `../job-tracker-gateway` (port 8081); this service has no UI of its own. It calls this service over HTTP via a Spring `@HttpExchange` interface (`JobApplicationClient`), so pagination and filtering still execute here at the DB level — keep the `GET /api/job-applications` query parameters and `PageResponse` shape in sync with the gateway's copies of the DTOs. `docker-compose up --build` starts it as the `gateway` service.
