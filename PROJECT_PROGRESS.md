# PulseWallet Project Progress

## Overall Progress

```
[###############################------] 78% (72/92 verified tasks)
```

Percentages count only tasks marked `[x]`. Items marked `[~]` or `[?]` are not counted as complete.

| Milestone | Status | Verified progress |
|---|---|---:|
| M1 - Environment & Foundation | Complete | 100% (12/12) |
| M2 - Authentication & Core Data Model | Complete | 100% (14/14) |
| M3 - Financial Planning Engine | Complete | 100% (14/14) |
| M4 - Fraud Detection Engine | In progress | 77% (17/22) |
| M5 - System Integration | Complete | 100% (8/8) |
| M6 - Frontend Dashboard | Complete | 100% (7/7) |
| M7 - Testing & Polish | Not started | 0% (0/6) |
| M8 - Deployment & Documentation | Not started | 0% (0/9) |

## Current Status

M1–M3 are complete based on the verified implementation, tests, and passing full Maven build, and M4 remains in progress with model productionization and container work still pending. M5 is now complete: the dashboard summary and polling-based alert delivery are implemented in the backend, and the fraud + notification flow remains user-scoped and resilient. M6 is now complete: the React + Vite frontend is wired to the authenticated backend APIs, the dashboard renders live financial and alert data, the remaining dashboard sections and routes are implemented, and validation confirms the dashboard flows are working without changing backend behavior. M7–M8 remain untouched.

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

Progress: 77% (17/22 verified tasks)

```
[###############################---------] 77%
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
- [x] Verify a real end-to-end Spring Boot to FastAPI request with the model running

The live verification used FastAPI on `127.0.0.1:8000` with the saved model/scaler and the authenticated Spring endpoint on `127.0.0.1:8080`; the response preserved the four fraud fields. The roadmap's original `/predict` name is not implemented; the repository uses `/fraud-check`.

## M5 - System Integration

Progress: 100% (8/8)

```
[########################################] 100%
```

- [x] Trigger fraud checking automatically on transaction creation
- [x] Implement graceful degradation when the Python service is unavailable
- [x] Add timeout, retry, and fallback behavior
- [x] Persist `FraudAlert` records for fraudulent expense transactions
- [x] Add notification persistence
- [x] Add `GET /api/dashboard-summary` combining budget and fraud data
- [x] Deliver alerts through polling via authenticated notification endpoints
- [x] Configure `FRAUD_SCORE_THRESHOLD`

The M5 integration is complete: expense transactions still call the fraud checker through the service layer, the validation and fallback behavior remain intact, and the backend now exposes both the dashboard summary and the user-scoped polling-based notifications path without broadening scope beyond the authenticated user.

## M6 - Frontend Dashboard

Progress: 100% (7/7)

```
[########################################] 100%
```

- [x] Scaffold a React/Vite application and set up the frontend workspace
- [x] Add login and signup screens with token and refresh handling
- [x] Add a validated transaction form
- [x] Add dashboard analytics and budget charts
- [x] Add a fraud alerts panel
- [x] Add a centralized API client with auth/error handling
- [x] Add loading, empty, and error states

The frontend dashboard is now fully wired to the authenticated backend APIs for dashboard summary, transaction summaries, budgets, forecast, advisory, and notifications. The app preserves the JWT auth flow, protects routes, renders responsive charts, exposes the required routes, and includes empty/loading/error states without inventing mock financial data.

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

M3 is complete and verified. M4 remains the active milestone and still needs productionization, feature-mismatch resolution, container wiring, and live integration proof. M5 is complete and verified in the backend, and M6 is complete and verified in the frontend. The immediate next task is to remain within M6 only until verification and then stop before M7 begins.

## Next Task

No further work is required under M6; the milestone is complete and verified. M7 remains untouched and should start only after the verified M6 completion is accepted.

## Verification Notes

The tracker was calculated from these repository areas:

- `docs/ROADMAP.md`, `README.md`, `docker-compose.yml`, `pom.xml`, `.env.example`, `.gitignore`, and `.gitattributes`
- Java entities, repositories, DTOs, controllers, services, security/configuration classes, and Flyway migrations under `src/main`
- Java tests under `src/test`, including `AuthServiceTest`, `JwtServiceTest`, `CategoryServiceTest`, `TransactionServiceTest`, `BudgetServiceTest`, `FraudDetectionServiceTest`, `FraudDetectionControllerTest`, and `PulsewalletApplicationTests`
- `fraud-detection/api/main.py`, `fraud-detection/config/features.py`, `fraud-detection/requirements.txt`, `fraud-detection/notebooks/01_credit_card_fraud_eda.ipynb`, `fraud-detection/models/`, `fraud-detection/data/raw/`, and `fraud-detection/src/`
- `data/README.md`, application profile files under `src/main/resources`, and the React frontend app under `frontend/`

Backend validation remained green for the verified M1–M5 paths. Frontend validation on 2026-09-02: `cd "c:/Users/devan/Desktop/PulseWallet/frontend" ; npm run build` completed successfully and generated a production bundle with `✓ built in 7.24s`. The M6 dashboard, routes, alerts, budget views, and auth flow are now verified against the live API contracts.

## Last Updated

2026-09-02

## How to Update This File

1. Inspect the implementation, tests, migrations, configuration, and artifacts before changing a checkbox.
2. Mark `[x]` only when the repository contains direct evidence that the task is implemented and usable.
3. Use `[~]` for partially implemented work and `[?] Needs verification` when evidence is insufficient; neither counts as complete.
4. Recalculate each milestone as `verified completed tasks / total listed tasks`, then recalculate the overall percentage from all milestone task counts.
5. Update `Current Status`, `Next Task`, `Verification Notes`, and `Last Updated` whenever the repository changes.
