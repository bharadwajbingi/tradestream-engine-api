# TradeStream Engine (TSE)

An enterprise-grade, high-throughput asynchronous trade ingestion and validation system built with Java 17 and Spring Boot 4.0.3.

TSE processes CSV trade files up to 1GB, validates 21 trade fields per record, detects duplicates, and manages the full lifecycle of trade transactions — from upload through archival and deletion.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             TradeStream Engine                              │
│                                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌─────────────┐   │
│  │  api-module  │   │service-module│   │ batch-module │   │ dao-module  │   │
│  │              │   │              │   │              │   │             │   │
│  │ Controllers  │──▶│ Services     │   │ Batch Config │   │ JPA Repos   │   │
│  │ REST API     │   │ Security     │──▶│ Processor    │──▶│             │   │
│  │ Config YAML  │   │ JWT/OAuth2   │   │ Writer       │   │             │   │
│  │ Flyway SQL   │   │ Validation   │   │ Listener     │   │             │   │
│  └──────────────┘   │ Mappers      │   └──────────────┘   └──────┬──────┘   │
│                      │ Schedulers   │                              │        │
│                      └──────────────┘                              │        │
│                                                                    ▼        │
│  ┌──────────────┐                                        ┌─────────────┐    │
│  │ model-module │◀───────────────────────────────────────│ PostgreSQL  │    │
│  │              │                                        │    15       │    │
│  │ Entities     │                                        └─────────────┘    │
│  │ DTOs         │                                                           │
│  │ Enums        │                                        ┌─────────────┐    │
│  │ Specs        │                                        │   AWS S3    │    │
│  └──────────────┘                                        │  (Exports)  │    │
│                                                          └─────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Module Dependency Graph

```
api-module
├── service-module
│   ├── dao-module
│   │   └── model-module
│   └── model-module
└── batch-module
    ├── service-module
    ├── dao-module
    └── model-module
```

### Data Flow — File Processing Pipeline

```
┌──────────┐     ┌──────────────┐     ┌────────────────────┐     ┌──────────────┐
│  Client  │────▶│ POST /upload │────▶│ Save to disk +     │────▶│   PENDING    │
│          │     │  (instant)   │     │ Create metadata    │     │  (database)  │
└──────────┘     └──────────────┘     └────────────────────┘     └──────┬───────┘
                                                                         │
                                                                         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  FileProcessingScheduler (polls every 5s)                                    │
│                                                                              │
│  1. Find PENDING files (one per user)                                        │
│  2. Mark as STARTED                                                          │
│  3. Launch Spring Batch Job                                                  │
│     ┌─────────────────────────────────────────────────────────────────────┐  │
│     │  Spring Batch Job                                                   │  │
│     │                                                                     │  │
│     │  ┌────────────┐    ┌──────────────────┐    ┌──────────────────┐     │  │
│     │  │   Reader   │───▶│    Processor     │───▶│     Writer       │     │  │
│     │  │ (CSV file) │    │ • Validate TXN ID│    │ • Bulk dedup     │     │  │
│     │  │ chunk=1000 │    │ • Batch dedup    │    │ • Save success   │     │  │
│     │  │            │    │ • 21-field valid │    │ • Save errors    │     │  │
│     │  │            │    │ • Map to entity  │    │ • Update registry│     │  │
│     │  └────────────┘    └──────────────────┘    └──────────────────┘     │  │
│     └─────────────────────────────────────────────────────────────────────┘  │
│  4. JobListener updates final status (COMPLETED/COMPLETED_WITH_ERROR/FAILED) │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Deployment Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                          AWS EC2 Instance                          │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                        Docker Compose                        │  │
│  │                                                              │  │
│  │  ┌───────────────────────┐    ┌───────────────────────────┐  │  │
│  │  │  trade-backend-service│    │   trade-postgres-db       │  │  │
│  │  │                       │    │                           │  │  │
│  │  │  Java 17 (JRE Alpine) │───▶│  PostgreSQL 15 Alpine     │  │  │
│  │  │  Spring Boot 4.0.3    │    │                           │  │  │
│  │  │  Port: 8080           │    │  Port: 5432               │  │  │
│  │  │                       │    │  Volume: postgres_data    │  │  │
│  │  │  Volume: trade_uploads│    │                           │  │  │
│  │  └───────────────────────┘    └───────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
         │                                          │
         ▼                                          ▼
┌─────────────────┐                    ┌─────────────────────┐
│  AWS CloudFront │                    │      AWS S3         │
│  (Frontend CDN) │                    │  (Export Storage)   │
└─────────────────┘                    └─────────────────────┘

CI/CD Pipeline (GitHub Actions):
  push to main → Run Tests → Build Docker Image → Push to DockerHub → SSH Deploy to EC2
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.3 |
| Batch Processing | Spring Batch (chunk-based, 1000/chunk, multi-threaded) |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Authentication | JWT (jjwt) + Google OAuth2 + TOTP 2FA |
| Object Mapping | MapStruct 1.5.5 |
| Cloud Storage | AWS S3 SDK 2.25 |
| CSV Parsing | Apache Commons CSV 1.11 |
| API Docs | SpringDoc OpenAPI 3.0 |
| Testing | JUnit 5, Mockito, Testcontainers |
| Coverage | JaCoCo |
| Containerization | Docker (multi-stage build) |
| CI/CD | GitHub Actions |

---

## Prerequisites

- Java 17 (Temurin recommended)
- Maven 3.9+ (wrapper included)
- Docker & Docker Compose
- PostgreSQL 15 (or use Docker Compose)
- AWS S3 bucket (for export functionality)
- Google OAuth2 credentials (for social login)

---

## Quick Start (Local Development)

### 1. Clone and configure

```bash
git clone <repository-url>
cd tradestream-engine-api
cp .env.example .env
```

Edit `.env` and set required values:
```properties
# REQUIRED - Application will NOT start without these
JWT_SECRET=<generate-a-random-64-char-secret>
TOTP_ENCRYPTION_KEY=<generate-a-random-32-char-key>

# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Google OAuth (optional for local dev without OAuth)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# AWS S3 (optional for local dev without exports)
AWS_S3_ACCESS_KEY=your-key
AWS_S3_SECRET_KEY=your-secret
AWS_S3_REGION=ap-southeast-2
AWS_S3_BUCKET_NAME=tradestream-exports
```

### 2. Start with Docker Compose

```bash
docker compose up -d
```

This starts PostgreSQL and the backend. The API will be available at `http://localhost:8080`.

### 3. Start without Docker (development)

```bash
# Start PostgreSQL separately (or use Docker for just the DB)
docker compose up -d db

# Set environment variables or use dev profile
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET=dev-trade-stream-engine-secret-change-me-in-prod
export TOTP_ENCRYPTION_KEY=dev-totp-encryption-key-32-chars-change

# Run the application
./mvnw spring-boot:run -pl api-module
```

### 4. Run tests

```bash
./mvnw test
```

> Note: The integration test (`TradeStreamIntegrationTest`) requires Docker for Testcontainers. It will be skipped automatically if Docker is not available.

---

## API Documentation

Once the application is running, access the Swagger UI at:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive JWT token |
| POST | `/file/upload` | Upload a CSV trade file (async processing) |
| GET | `/file/getAll` | List all active file uploads |
| GET | `/file/metrics` | Dashboard metrics |
| GET | `/transactions/export` | Export transactions to S3 (async) |
| POST | `/auth/totp/setup` | Setup TOTP 2FA |

### Authentication

All endpoints except `/auth/**` require a Bearer token:

```
Authorization: Bearer <jwt-token>
```

Export endpoints additionally require a TOTP-verified export token (if 2FA is enabled):

```
X-Export-Token: <export-token>
```

---

## Database Schema

Managed by Flyway with `ddl-auto: validate`. Migrations are in `api-module/src/main/resources/db/migration/`.

| Migration | Description |
|-----------|-------------|
| V1 | Initial schema (9 tables + performance indexes) |

Spring Batch metadata tables are created automatically (`initialize-schema: always`).

### Tables

`users`, `file_meta_data`, `trade_transaction`, `trade_archive`, `transaction_error`, `export_job`, `deleted_trade_transaction`, `deleted_transaction_error`, `transaction_registry`

---

## Configuration

All configuration is via environment variables. See `.env.example` for the full list.

### Required Variables (no defaults)

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HMAC-SHA256 signing key for JWT tokens. Must be at least 32 characters. |
| `TOTP_ENCRYPTION_KEY` | AES-256-GCM encryption key for TOTP secrets. Must be at least 32 characters. |

### Optional Variables (with defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/trade_db` | JDBC URL |
| `SERVER_PORT` | `8080` | Server port |
| `BATCH_TRADE_CHUNK_SIZE` | `1000` | Records per batch chunk |
| `JWT_EXPIRATION_MS` | `1800000` | JWT token lifetime (30 min) |
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | `120` | Rate limit per IP |
| `FILE_UPLOAD_TEMP_DIR` | `./data/uploads/` | Temp upload directory |

---

## Performance

| Metric | Result (EC2 t2.micro, 2 vCPU, 908MB) |
|--------|---------------------------------------|
| 100K fresh records | ~55 seconds |
| 100K duplicate records | ~32 seconds |
| 1M records (estimated) | ~550-650 seconds |

### Tuning (current settings for t2.micro)

| Setting | Value | Purpose |
|---------|-------|---------|
| Chunk size | 1000 | Balance: fewer DB commits vs memory |
| stepTaskExecutor | core=2, max=4 | Matches 2 vCPU — parallel read/process |
| Hibernate batch_size | 1000 | Batched INSERTs matching chunk size |
| JVM | `-Xmx384m -XX:+UseG1GC` | Fits alongside PostgreSQL in 908MB |

### Resilience

- **Multi-threaded processing** — `SynchronizedItemStreamReader` + `synchronized write()` ensure thread safety
- **2-level deduplication** — in-batch (ConcurrentHashMap) → bulk DB query (registry table)
- **Auto-recovery on startup** — reverts stuck STARTED/PROCESSING files to PENDING
- **Periodic recovery (every 30 min)** — catches files stuck due to queue rejection or silent failures (only files stuck >30 min)
- **Idempotent re-processing** — safe to re-process any file; dedup prevents duplicates
- **Temp file preservation** — only deleted on successful completion; kept for recovery on failure

---

## Security

- **JWT Authentication** — Stateless, 30-minute expiration, HMAC-SHA256
- **Google OAuth2** — Social login with auto-registration
- **TOTP 2FA** — Required for export operations; secrets encrypted with AES-256-GCM
- **Rate Limiting** — 120 requests/minute per IP (ConcurrentHashMap + AtomicInteger)
- **Ownership Isolation** — Users can only access their own files and errors (JPA Specifications)
- **Flyway + validate** — Schema drift detected immediately on startup
- **Fail-fast secrets** — Application refuses to start if JWT_SECRET or TOTP_ENCRYPTION_KEY are missing

---

## Project Structure

```
tradestream-engine-api/
├── api-module/              # REST controllers, main app, config, migrations
├── service-module/          # Business logic, security, validation, mappers
├── batch-module/            # Spring Batch processing pipeline
├── dao-module/              # JPA repository interfaces
├── model-module/            # Entities, DTOs, enums, specifications
├── docker/                  # Docker support files
├── .github/workflows/       # CI/CD pipeline
├── docker-compose.yml       # Local development stack
├── Dockerfile               # Multi-stage production build
├── .env.example             # Environment variable template
└── pom.xml                  # Parent POM (multi-module)
```

---

## License

MIT — Bharadwaj Bingi.
