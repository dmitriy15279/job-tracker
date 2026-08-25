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

The app expects PostgreSQL reachable at the URL in `src/main/resources/application.properties` (`localhost:5433/job_tracker`). Start it with:

```bash
docker-compose up -d db
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

Request flow: `Controller` → `Service` (business logic + entity/DTO mapping) → `Repository` (JPA) → Postgres. This is a one-endpoint-group-at-a-time codebase currently covering only job applications (`/api/job-applications`); follow the same three-layer pattern (controller/dto, service, persistence/entity) when adding new resource types.
