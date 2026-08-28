# PulseWallet Project Progress

## Overall Progress

```
[##########################------------] 62% (56/91 verified tasks)
```

Percentages count only tasks marked `[x]`. Items marked `[~]` or `[?]` are not counted as complete.

| Milestone | Status | Verified progress |
|---|---|---:|
| M1 - Environment & Foundation | Complete | 100% (12/12) |
| M2 - Authentication & Core Data Model | Complete | 100% (14/14) |
| M3 - Financial Planning Engine | Complete | 100% (14/14) |
| M4 - Fraud Detection Engine | In progress | 73% (16/22) |
| M5 - System Integration | Not started | 0% (0/7) |
| M6 - Frontend Dashboard | Not started | 0% (0/7) |
| M7 - Testing & Polish | Not started | 0% (0/6) |
| M8 - Deployment & Documentation | Not started | 0% (0/9) |

## Current Status

M4 is the active milestone after M3 completion. M1, M2, and M3 are complete based on the verified implementation, tests, and passing full Maven build. M5, M6, M7, and M8 have no verified completed tasks in the repository.

## M1 - Environment & Foundation

Progress: 100% (12/12)

```
[########################################] 100%
```

- [x] Spring Boot backend skeleton
- [x] README quickstart, configuration, API table, and troubleshooting
- [x] `.env.example` with environment variables
- [x] PostgreSQL Docker Compose service with healthcheck and optional pgAdmin
- [x] Base, development, and production application profiles
- [x] Java package structure for config, controllers, DTOs, entities, exceptions, repositories, security, and services
- [x] Foundation response/error classes and global exception handling
- [x] `/api/health` endpoint with database status
- [x] Spring Boot Actuator configuration
- [x] Git branching documentation
- [x] `.gitignore` coverage for secrets, ML, datasets, Python, and Node artifacts
- [x] `.gitattributes` line-ending normalization

## M2 - Authentication & Core Data Model

Progress: 100% (14/14)

```
[########################################] 100%
```

- [x] `User`, `Transaction`, `Category`, and `Budget` entities with relationships
- [x] Repositories for the core entities and aggregate projections
- [x] Flyway migrations V1 through V7
- [x] `ddl-auto=validate` in the application profiles
- [x] Registration API with BCrypt password hashing
- [x] Login API returning a JWT
- [x] JWT service, authentication filter, and security configuration
- [x] 401 and 403 REST handlers
- [x] CORS wired into the security filter chain
- [x] User-scoped transaction CRUD API
- [x] Category CRUD API and seeded categories
- [x] User-scoped budget CRUD API
- [x] Request/response DTOs with Bean Validation
- [x] Existing authentication, category, transaction, budget, JWT, and context-load tests

## M3 - Financial Planning Engine

Progress: 100% (14/14)

```
[########################################] 100%
```

### Completed tasks

- [x] Expense categorization service exists with keyword matching rules
- [x] Transportation category is present through `V7__add_travel_default_category.sql`
- [x] `Other Expense` category is seeded in `V5__seed_default_categories.sql`
- [x] Financial summary and spending calculations exist, including income, expenses, disposable income, category totals, monthly totals, and date filtering
- [x] `ExpenseCategorizationService` is wired into transaction creation and update when no category is supplied
- [x] `Other Expense` fallback is resolved via the system default category without duplication
- [x] Budget planning engine calculates salary, total expenses, disposable income, and recommended savings/investment/spending
- [x] `POST /api/budgets/plan` endpoint exists with authenticated user scoping and validation
- [x] Forecasting engine calculates historical monthly expense averages and returns a transparent deterministic forecast
- [x] `GET /api/forecast` endpoint exists and is user-scoped
- [x] Advisory engine provides deterministic savings/investment guidance and medical/emergency reserve rules
- [x] `GET /api/advisory` endpoint exists and is user-scoped
- [x] Budget maths tests cover zero expenses, negative balance, and invalid salary/date inputs
- [x] Expense categorization tests cover supported categories, fallback, and null handling
- [x] Full Maven suite passes for M1, M2, M3, and M4

The M3 implementation is now complete and verified with the full Maven test suite.

## M4 - Fraud Detection Engine

Progress: 73% (16/22 verified tasks)

```
[#############################-----------] 73%
```

### Completed tasks

- [x] Credit Card Fraud dataset is present and EDA is represented in `01_credit_card_fraud_eda.ipynb`
- [x] StandardScaler is used in the notebook
- [x] SMOTE is applied to the training data after splitting
- [x] Random Forest experiment exists in the notebook
- [x] XGBoost experiment and comparison exist in the notebook
- [x] Weighted ensemble experiment exists in the notebook
- [x] Evaluation outputs include precision, recall, PR-AUC, and confusion matrices
- [x] XGBoost is selected as the production model in the notebook
- [x] Scaler artifact is saved as `fraud_scaler.joblib`
- [x] XGBoost artifact is saved as `xgboost_fraud_model.joblib`
- [x] Feature configuration defines the model input features
- [x] FastAPI service loads the saved artifacts
- [x] FastAPI exposes `/fraud-check`
- [x] Fraud response includes a fraud result, probability, risk score, and risk level
- [x] Spring Boot exposes a fraud-check controller and calls the Python URL through `RestClient`
- [x] Dataset download guidance exists in `data/README.md`

### Remaining or unverified tasks

- [?] Needs verification: resolve the dataset/application feature mismatch; the model expects `Time`, `V1`-`V28`, and `Amount`, while application transactions do not provide that mapping
- [~] Preprocessing is implemented in notebook cells, but no reusable production preprocessing pipeline exists
- [ ] Serialize the full preprocessing and model pipeline together for production use
- [ ] Productionize preprocessing and training as standalone scripts; the relevant `fraud-detection/src` subdirectories are empty
- [ ] Add the fraud-detection service to `docker-compose.yml`
- [?] Needs verification: verify a real end-to-end Spring Boot to FastAPI request with the model running

The existing Spring tests use mocks (`MockRestServiceServer` and Mockito), so they prove request handling but not live service integration. The roadmap's original `/predict` name is not implemented; the repository uses `/fraud-check`.

## M5 - System Integration

Progress: 0% (0/7)

```
[----------------------------------------] 0%
```

- [ ] Trigger fraud checking automatically on transaction creation
- [ ] Decide and implement graceful degradation when the Python service is unavailable
- [ ] Add timeout, retry, and fallback behavior
- [ ] Add `FraudAlert` and notification persistence
- [ ] Add `GET /api/dashboard-summary` combining budget and fraud data
- [ ] Deliver alerts through WebSocket or polling
- [ ] Configure `FRAUD_SCORE_THRESHOLD`

No implementation evidence was found for these tasks. Existing direct fraud-check endpoints do not constitute system integration.

## M6 - Frontend Dashboard

Progress: 0% (0/7)

```
[----------------------------------------] 0%
```

- [ ] Scaffold a React/Vite application with routing
- [ ] Add login and signup screens with token and refresh handling
- [ ] Add a validated transaction form
- [ ] Add dashboard analytics and budget charts
- [ ] Add a fraud alerts panel
- [ ] Add a centralized API client with auth/error handling
- [ ] Add loading, empty, and error states

No frontend project was found. The Spring static and template directories are empty.

## M7 - Testing & Polish

Progress: 0% (0/6)

```
[----------------------------------------] 0%
```

- [ ] Add budget logic unit tests for edge cases
- [ ] Write the fraud evaluation report and threshold trade-off analysis
- [ ] Add API integration tests using Testcontainers or H2
- [ ] Add expired-token, malformed-token, and cross-user access tests
- [ ] Verify UI fixes and loading/error states
- [ ] Complete a manual end-to-end pass through every screen

The current 63-test Maven suite passes and covers the verified M1–M4 code paths, but it still does not prove the M7 deliverables.

## M8 - Deployment & Documentation

Progress: 0% (0/9)

```
[----------------------------------------] 0%
```

- [ ] Deploy PostgreSQL
- [ ] Deploy the Spring Boot backend with the production profile
- [ ] Deploy the Python fraud service
- [ ] Deploy the React frontend
- [ ] Set deployed frontend origins in CORS configuration
- [ ] Generate and configure a production JWT secret
- [ ] Add architecture diagram, screenshots, and final README pass
- [ ] Add project report and demo script
- [ ] Tag and verify a release build

Only local production-profile configuration exists. No deployment manifests, CI/CD configuration, or hosted-service evidence was found.

## Current Focus

M3 is complete and verified. M4 remains the active milestone and still needs productionization, feature-mismatch resolution, container wiring, and live integration proof.

## Next Task

Resolve the M4 feature-mapping issue and verify the saved Kaggle-trained model against actual application transaction fields before relying on it in production workflows. The M3 work is already complete and covered by the passing Maven suite.

## Verification Notes

The tracker was calculated from these repository areas:

- `docs/ROADMAP.md`, `README.md`, `docker-compose.yml`, `pom.xml`, `.env.example`, `.gitignore`, and `.gitattributes`
- Java entities, repositories, DTOs, controllers, services, security/configuration classes, and Flyway migrations under `src/main`
- Java tests under `src/test`, including `AuthServiceTest`, `JwtServiceTest`, `CategoryServiceTest`, `TransactionServiceTest`, `BudgetServiceTest`, `FraudDetectionServiceTest`, `FraudDetectionControllerTest`, and `PulsewalletApplicationTests`
- `fraud-detection/api/main.py`, `fraud-detection/config/features.py`, `fraud-detection/requirements.txt`, `fraud-detection/notebooks/01_credit_card_fraud_eda.ipynb`, `fraud-detection/models/`, `fraud-detection/data/raw/`, and `fraud-detection/src/`
- `data/README.md` and application profile files under `src/main/resources`

Validation performed on 2026-08-28: `.\mvnw.cmd clean test` completed with 63 tests, 0 failures, 0 errors, and `BUILD SUCCESS`.

## Last Updated

2026-08-28

## How to Update This File

1. Inspect the implementation, tests, migrations, configuration, and artifacts before changing a checkbox.
2. Mark `[x]` only when the repository contains direct evidence that the task is implemented and usable.
3. Use `[~]` for partially implemented work and `[?] Needs verification` when evidence is insufficient; neither counts as complete.
4. Recalculate each milestone as `verified completed tasks / total listed tasks`, then recalculate the overall percentage from all milestone task counts.
5. Update `Current Status`, `Next Task`, `Verification Notes`, and `Last Updated` whenever the repository changes.
