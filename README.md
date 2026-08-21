# FlexGuard

## Personal Financial Planning and Transaction Fraud Detection System

FlexGuard is a full-stack financial management system that combines personal financial planning with transaction fraud detection.

The system helps users:

- Track and categorize expenses
- Forecast future spending
- Calculate disposable income
- Generate personalized budget recommendations
- Recommend savings and investment allocation based on financial goals and risk appetite
- Detect potentially fraudulent transactions
- Generate fraud risk scores and security alerts
- View financial planning and fraud information through a unified dashboard

> **Status:** Milestone 1 complete (environment and foundation). The backend runs and connects to PostgreSQL; the data model, authentication, ML engines and frontend are not built yet. See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the milestone-by-milestone plan.

> **Note on naming:** the product is *FlexGuard*, but the Maven artifact, Java package (`com.pulsewallet.pulsewallet`) and Git repository are still named *PulseWallet*. This is deliberate for now — renaming touches every package declaration. Decide before Milestone 2 adds real classes, because the cost only goes up.

---

## Quick start

Goal: a clean clone running in under five minutes.

### Prerequisites

| Tool | Version | Check with |
|---|---|---|
| JDK | 25 | `java -version` |
| Docker Desktop | any recent | `docker --version` |
| Git | any recent | `git --version` |

Maven is **not** required — the repository ships the Maven wrapper (`mvnw`).

### Steps

```bash
# 1. Clone
git clone https://github.com/devansh13-tech/PulseWallet.git
cd PulseWallet

# 2. Create your local environment file
cp .env.example .env          # Windows cmd:        copy .env.example .env
                              # Windows PowerShell: Copy-Item .env.example .env

# 3. Start PostgreSQL (waits until the database actually accepts connections)
docker compose up -d --wait

# 4. Run the backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 5. Verify
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "success": true,
  "message": "PulseWallet is running",
  "data": {
    "application": "pulsewallet",
    "status": "UP",
    "activeProfiles": ["dev"],
    "database": "UP",
    "checkedAt": "2026-08-21T12:00:00Z"
  },
  "timestamp": "2026-08-21T12:00:00Z"
}
```

`"database": "UP"` is the part that matters — it proves the app reached PostgreSQL. If it reads `DOWN`, jump to [Troubleshooting](#troubleshooting).

You do **not** need to edit `.env` to get started. The `dev` profile ships defaults that match `docker-compose.yml` exactly.

### Useful commands

```bash
docker compose up -d --wait              # start PostgreSQL, block until ready
docker compose --profile tools up -d     # also start pgAdmin at localhost:5050
docker compose down                      # stop containers, keep data
docker compose down -v                   # stop AND delete the database volume
docker compose logs -f postgres          # tail database logs

./mvnw spring-boot:run                                        # run (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod        # run with prod profile
./mvnw clean verify                                           # compile + run tests
./mvnw clean package                                          # build the jar
```

---

## Configuration

Configuration is split so the same build runs on a laptop and on free-tier hosting without code changes.

| File | Purpose |
|---|---|
| `src/main/resources/application.properties` | Shared base. Every value overridable by an environment variable. |
| `src/main/resources/application-dev.properties` | Default profile. Verbose SQL, full Actuator, DevTools on, working local defaults. |
| `src/main/resources/application-prod.properties` | Milestone 8. No secret defaults, `ddl-auto=validate`, quiet logs, no stack traces in responses. |
| `.env.example` | Template listing every variable. Committed. |
| `.env` | Your real values. Gitignored — never commit it. |

**Who reads `.env`:** Docker Compose reads it automatically. Spring Boot does **not** read `.env` files. If you change a database value in `.env`, also export it to your shell or set it in your IDE run configuration, otherwise the app and the container will disagree.

Selecting a profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
SPRING_PROFILES_ACTIVE=prod java -jar target/pulsewallet-0.0.1-SNAPSHOT.jar
```

---

## API endpoints

Only the health endpoint exists today. The rest arrive with their milestones.

| Method | Path | Milestone | Description |
|---|---|---|---|
| `GET` | `/api/health` | 1 ✅ | Liveness plus live database check |
| `GET` | `/actuator/health` | 1 ✅ | Actuator health (full detail in `dev` only) |
| `POST` | `/api/auth/register` | 2 | Create an account |
| `POST` | `/api/auth/login` | 2 | Obtain a JWT |
| `GET`/`POST` | `/api/transactions` | 2 | List / create transactions |
| `GET`/`POST` | `/api/categories` | 2 | List / create categories |
| `POST` | `/api/budget/plan` | 3 | Salary + expenses → budget breakdown |
| `GET` | `/api/forecast` | 3 | Spending forecast from history |
| `GET` | `/api/advisory` | 3 | Savings and investment suggestions |
| `POST` | `/api/fraud-check` | 4 | Transaction → fraud risk score |
| `GET` | `/api/dashboard-summary` | 5 | Combined budgeting + fraud view |

### Response contract

Every success is wrapped in the same envelope:

```json
{ "success": true, "message": "OK", "data": { }, "timestamp": "2026-08-21T12:00:00Z" }
```

Every failure returns the same error shape, produced by a single `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-08-21T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/transactions",
  "fieldErrors": { "amount": "must be greater than 0" }
}
```

The frontend can therefore be written against exactly one error format.

---

## Core Modules

### 1. Financial Planning Engine

The planning engine follows:

Transaction Data
→ Expense Categorization
→ Spending Forecast
→ Disposable Income
→ Budget Recommendation
→ Savings & Investment Advisory

The advisory component considers goals and risk appetite and can recommend allocations toward:

- Emergency Fund
- Medical Fund
- SIP / Mutual Funds
- Retirement

### 2. Fraud Detection Engine

The fraud engine processes transaction and behavioral information to identify suspicious activity.

Planned machine-learning models:

- Random Forest
- XGBoost

Planned fraud-processing techniques:

- Feature engineering
- Class-imbalance handling using SMOTE
- Fraud risk scoring
- Threat / anomaly identification

Evaluation uses **precision, recall, PR-AUC and the confusion matrix** — not accuracy. The reference dataset is roughly 0.17% fraud, so a model that predicts "never fraud" scores 99.8% accuracy while catching nothing.

### 3. Unified Dashboard

The dashboard will combine:

- Financial planning information
- Spending analytics
- Budget recommendations
- Savings and investment recommendations
- Fraud risk information
- Security alerts

---

## Technology Stack

### Backend
- Java 25
- Spring Boot 4.1.0
- Spring Data JPA
- Spring Validation
- Spring Boot Actuator
- REST APIs

### Database
- PostgreSQL 18 (via Docker Compose)

### Machine Learning
- Python
- Scikit-learn
- XGBoost
- Random Forest
- ARIMA
- LSTM
- SMOTE

### Frontend
- React

### Development
- Git
- GitHub
- VS Code / Antigravity
- Maven (wrapper included)
- Docker Compose

---

## Project Architecture

```text
                    FlexGuard
                        |
              +---------+---------+
              |                   |
              v                   v
       Fraud Detection      Financial Planning
              |                   |
              |             Expense Categorization
              |                   |
              |             Spending Forecast
              |                   |
              |            Disposable Income
              |                   |
              |            Budget Recommendation
              |                   |
              |        Savings & Investment Advisory
              |                   |
              +---------+---------+
                        |
                        v
                 Unified Dashboard
```

### Deployment topology

```text
  React SPA            Spring Boot API              Python service
 (Milestone 6)          (Milestones 1-5)             (Milestone 4)
      |                        |                           |
      |  REST + JWT            |   POST /predict           |
      +----------------------->+-------------------------->+
                               |                    Random Forest /
                               v                     XGBoost model
                        PostgreSQL 18
                     users, transactions,
                    categories, budgets,
                        fraud alerts
```

The fraud model is served as a separate Python process rather than embedded in the JVM, because the training and serving stack (scikit-learn, XGBoost, SMOTE) is Python-native. The two services communicate over HTTP.

---

## Repository layout

```text
PulseWallet/
├── docker-compose.yml          # PostgreSQL 18 + optional pgAdmin
├── .env.example                # every environment variable, documented
├── pom.xml                     # Maven build
├── mvnw / mvnw.cmd             # Maven wrapper - no local Maven needed
├── docs/
│   ├── ROADMAP.md              # 8 milestones, tasks, deliverables, checkboxes
│   └── BRANCHING.md            # git workflow for the team
└── src/
    ├── main/
    │   ├── java/com/pulsewallet/pulsewallet/
    │   │   ├── PulsewalletApplication.java
    │   │   ├── config/         # CorsConfig; later SecurityConfig, WebSocketConfig
    │   │   ├── controller/     # HTTP boundary - DTOs in, DTOs out
    │   │   ├── dto/            # ApiResponse, ApiError; request/response records
    │   │   ├── entity/         # JPA entities (Milestone 2)
    │   │   ├── exception/      # GlobalExceptionHandler, custom exceptions
    │   │   ├── repository/     # Spring Data JPA repositories (Milestone 2)
    │   │   ├── security/       # JWT filter, UserDetailsService (Milestone 2)
    │   │   └── service/        # business logic
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       └── application-prod.properties
    └── test/
        └── java/com/pulsewallet/pulsewallet/
```

Each package carries a `package-info.java` documenting what belongs in it and what does not. Read those before adding classes — they exist to stop the layering from eroding once four people are committing in parallel.

---

## Layering rules

```text
Controller  →  Service  →  Repository  →  Database
   (DTO)      (business)     (JPA)
```

- Controllers never touch repositories directly.
- Entities never appear in a controller signature; map to a DTO.
- Services contain no HTTP types, so they stay unit-testable without a web context.
- Money is `BigDecimal`, never `double`.

---

## Troubleshooting

**`"database": "DOWN"` in the health response, or the app fails to start with a connection error**

The database is not reachable. Check the container is up and healthy:

```bash
docker compose ps
docker compose logs postgres
```

**Port 5432 already allocated**

You already have PostgreSQL running locally. Either stop it, or move the container to another port — set `DB_PORT=5433` in `.env`, then restart and tell the app about it too:

```bash
docker compose down && docker compose up -d --wait
DB_PORT=5433 ./mvnw spring-boot:run
```

**Port 8080 already in use**

Set `SERVER_PORT=8081` in `.env` and run with `SERVER_PORT=8081 ./mvnw spring-boot:run`.

**`Permission denied: ./mvnw`** (macOS/Linux)

```bash
chmod +x mvnw
```

**Authentication failed for user**

The volume was created with different credentials than the ones you are now passing. Compose only applies `POSTGRES_USER`/`POSTGRES_PASSWORD` when initialising an empty volume. Reset it:

```bash
docker compose down -v && docker compose up -d --wait
```

This deletes all local data.

**`git status` shows files as modified that you never touched**

Line-ending normalisation. `.gitattributes` now enforces LF in the repository; run once:

```bash
git add --renormalize .
git commit -m "chore: normalise line endings"
```

**Schema looks stale after changing an entity**

In `dev`, `spring.jpa.hibernate.ddl-auto=update` adds columns and tables but never drops or alters existing ones. During Milestone 2, the fastest reset is `docker compose down -v && docker compose up -d --wait`. Once Flyway lands, use migrations instead.

---

## Contributing

Read [`docs/BRANCHING.md`](docs/BRANCHING.md) before your first commit. Short version: branch from `develop`, name it `feature/<milestone>-<slug>`, open a pull request, never push to `main`.
