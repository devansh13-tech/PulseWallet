# FlexGuard — Milestone Roadmap

Twelve weeks, eight gradeable checkpoints. Each milestone has a single deliverable you can demonstrate; tick tasks as they land so progress is visible at a glance.

**Legend:** `[ ]` not started · `[~]` in progress · `[x]` done

| # | Milestone | Weeks | Status |
|---|---|---|---|
| 1 | Environment & Foundation | 1–2 | ✅ Complete |
| 2 | Authentication & Core Data Model | 3–4 | Not started |
| 3 | Financial Planning Engine | 5–7 | Not started |
| 4 | Fraud Detection Engine | 6–8 | Not started |
| 5 | System Integration | 9 | Not started |
| 6 | Frontend Dashboard | 8–10 | Not started |
| 7 | Testing & Polish | 11 | Not started |
| 8 | Deployment & Documentation | 12 | Not started |

Milestones 3 and 4 run in parallel, as do 4/5 and 6. That parallelism is what makes twelve weeks feasible, and it is also the main scheduling risk — see [Risks](#risks).

---

## Milestone 1 — Environment & Foundation

**Weeks 1–2 · Deliverable: any teammate can clone the repo and run it in under 5 minutes.**

- [x] Spring Boot backend skeleton
- [x] `README.md` with prerequisites, quickstart, configuration, API table, troubleshooting
- [x] `.env.example` documenting every environment variable
- [x] `docker-compose.yml` for PostgreSQL 18, with healthcheck and optional pgAdmin
- [x] Environment profiles: `application.properties` base + `dev` + `prod`
- [x] Base package structure — `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `security`, `service`, each with a `package-info.java` stating what belongs there
- [x] Foundation classes — `ApiResponse`, `ApiError`, `GlobalExceptionHandler`, `ResourceNotFoundException`, `CorsConfig`
- [x] `/api/health` reports active profile and live database connectivity
- [x] Spring Boot Actuator wired, exposed fully in `dev` and restricted in `prod`
- [x] Git branching strategy documented in `docs/BRANCHING.md`
- [x] `.gitignore` hardened — secrets, ML artifacts, datasets, Python, Node
- [x] `.gitattributes` normalises line endings, ending phantom CRLF diffs

**Evidence to show your guide:** a clean `git clone` on a machine that has never run this project, followed by `docker compose up -d --wait`, `./mvnw spring-boot:run`, and a `/api/health` response with `"database": "UP"`. Time it in front of them.

**Open decisions before Milestone 2 — resolved:**

- **Project name.** Still unresolved. Product is *FlexGuard*; artifact, package (`com.pulsewallet.pulsewallet`) and repository are *PulseWallet*. Milestone 2 went ahead under the existing `com.pulsewallet.pulsewallet` package rather than block on this, per "the cost only rises as classes accumulate" — it just rose. If you want the rename, do it now (IDE rename-refactor, not find-and-replace) before Milestone 3 adds more classes on top.
- **Migration tool.** Resolved: Flyway. Plain SQL migrations under `src/main/resources/db/migration`, `V1`–`V5`.
- **API documentation.** Still unresolved / not added. No Swagger/springdoc-openapi wired up yet — worth revisiting once the API surface is more final.

---

## Milestone 2 — Authentication & Core Data Model

**Weeks 3–4 · Deliverable: a user can register, log in, and add/view transactions via API.**

- [x] Design entities on paper first: `User`, `Transaction`, `Category`, `Budget` — settle fields, types and relationships before any code
- [x] Implement the four entities
- [x] Repositories for each entity
- [x] Add Flyway (or Liquibase) and write the baseline migration
- [x] **Switch `ddl-auto` to `validate` in both profiles** once migrations exist
- [x] `POST /api/auth/register` with BCrypt password hashing
- [x] `POST /api/auth/login` returning a JWT
- [x] `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`
- [x] Add `AuthenticationException` (401) and `AccessDeniedException` (403) handlers **above** the catch-all in `GlobalExceptionHandler`
- [x] Wire `CorsConfig` into the security filter chain via `http.cors(...)`
- [x] Transaction CRUD, scoped so a user can only ever see their own rows
- [x] Category CRUD with sensible seed categories
- [x] Budget CRUD (limit + optional category + date range only — the recommendation engine is Milestone 3)
- [x] Request/response DTOs with Bean Validation

**Evidence:** a Postman collection or shell script that registers, logs in, creates a transaction with the token, and gets 401 without it. (Not run in the sandbox that implemented this — no Docker/Maven Central access there. Run `./mvnw clean verify` plus a manual `curl` pass locally to confirm; see the Testing section of the implementation report.)

**Watch out:** Spring Boot 4 ships Spring Security 7. Most JWT tutorials online target Security 6 and will not compile. Work from the current reference docs, not blog posts. Budget real time for this — it is the most likely place to lose three days.

---

## Milestone 3 — Financial Planning Engine

**Weeks 5–7 · Deliverable: API takes salary + expenses and returns a budget breakdown plus savings/investment suggestions.**

- [ ] Expense categorization logic — rules first, model later
- [ ] Budget engine: salary − expenses → disposable income
- [ ] Forecasting model (ARIMA and/or LSTM) over transaction history
- [ ] Savings and investment advisory: emergency fund, medical fund, SIP suggestions
- [ ] `POST /api/budget/plan`, `GET /api/forecast`, `GET /api/advisory`
- [ ] Unit tests for the budget maths — zero expenses, negative balance, missing salary

**Evidence:** one request with a realistic salary and expense list, returning a breakdown a non-technical person can read.

**Sequencing advice:** build the rule-based version of every piece first and ship the endpoints. A working deterministic budget engine beats a half-finished LSTM at demo time, and forecasting needs 12+ months of history per user to produce anything meaningful — plan how you will generate that synthetic history early.

**Advisory caveat:** frame outputs as illustrative allocations based on standard rules of thumb, not personalised financial advice. Say so in the UI and the report.

---

## Milestone 4 — Fraud Detection Engine

**Weeks 6–8, parallel with Milestone 3 · Deliverable: submit a transaction, get back a fraud-risk score.**

- [ ] EDA on the Kaggle Credit Card Fraud dataset
- [ ] **Resolve the feature-mismatch problem below before training anything**
- [ ] Preprocessing pipeline, with SMOTE applied to the training fold only
- [ ] Train Random Forest and XGBoost; compare properly
- [ ] Evaluate on precision, recall, PR-AUC and the confusion matrix — never accuracy alone
- [ ] Serialise the model and the full preprocessing pipeline together
- [ ] FastAPI (or Flask) service exposing `POST /predict`
- [ ] Add the Python service to `docker-compose.yml` on `pulsewallet-net`
- [ ] Spring Boot `POST /api/fraud-check` calling through to it
- [ ] Document the dataset download step — never commit the CSV

**Evidence:** a confusion matrix and a precision/recall table, plus a live `curl` returning a score for a transaction you invent on the spot.

### The feature-mismatch problem — read this before Week 6

The Kaggle dataset's features are `V1`–`V28`: anonymised PCA components, plus `Time` and `Amount`. Your application's transactions have amount, category, timestamp, and user history. **There is no mapping between the two.** A model trained on `V1`–`V28` mathematically cannot score a real FlexGuard transaction, so the two halves of this project will not connect at Milestone 5 unless you plan for it.

Three honest ways out:

1. **Two-track approach (recommended).** Use the Kaggle dataset to demonstrate the full ML methodology — EDA, SMOTE, model comparison, evaluation — as a self-contained result. Then build the live `/fraud-check` scorer on features you actually have: amount versus the user's rolling mean, time-of-day anomaly, spending velocity, new-merchant flag, category deviation. Train it on synthetic labelled data you generate. Present them as two deliverables: rigorous methodology plus a working integrated scorer.
2. **Synthetic-only.** Generate a labelled transaction dataset with realistic fraud patterns and train solely on that. Fully integrated, but weaker as an ML result.
3. **Kaggle-only.** Keep the model standalone with a demo form that accepts `V1`–`V28`. Strong ML story, but Milestone 5's "auto-trigger on every transaction" deliverable does not hold.

Pick one and write down why. A guide will respect a documented trade-off far more than a demo that quietly feeds fabricated `V1`–`V28` values into the model.

**SMOTE warning:** apply it after the train/test split, to the training set only. Oversampling before splitting leaks synthetic copies into the test set and produces impressive metrics that mean nothing.

---

## Milestone 5 — System Integration

**Week 9 · Deliverable: one transaction submission updates the budget dashboard and raises a fraud alert if flagged.**

- [ ] Fraud check triggers automatically on transaction creation
- [ ] Decide sync versus async: if the Python service is slow or down, does the transaction still save? (It should — degrade gracefully, mark the check as pending.)
- [ ] Timeout, retry and fallback behaviour for the fraud call
- [ ] `FraudAlert` entity plus notification table
- [ ] `GET /api/dashboard-summary` aggregating budget and fraud data
- [ ] Alert delivery via WebSocket, or polling the notification table
- [ ] Threshold configurable through `FRAUD_SCORE_THRESHOLD`, tuned from your PR curve

**Evidence:** one API call, then show the dashboard summary changing and the alert appearing.

---

## Milestone 6 — Frontend Dashboard

**Weeks 8–10, parallel with Milestones 4–5 · Deliverable: clickable UI demoing the full flow end to end.**

- [ ] React app scaffolded (Vite), routing, project structure
- [ ] Login and signup screens with token storage and refresh handling
- [ ] Add-transaction form with client-side validation matching server rules
- [ ] Dashboard: spending analytics and budget breakdown charts
- [ ] Fraud alerts panel
- [ ] API client centralising auth headers and `ApiError` handling
- [ ] Loading, empty and error states on every screen

**Evidence:** screen recording of signup → add transaction → dashboard updates → fraud alert appears.

**Start early, with mocks.** Do not wait for Milestone 5. Build against hardcoded JSON matching the documented `ApiResponse` shape, then swap in real calls. Blocking the frontend on backend completion is the most common way a twelve-week plan turns into a fourteen-week one.

---

## Milestone 7 — Testing & Polish

**Week 11 · Deliverable: stable, demo-ready build with no major bugs.**

- [ ] Unit tests for budget logic, including edge cases
- [ ] Fraud model evaluation written up properly — precision, recall, PR-AUC, confusion matrix, and what the threshold choice costs in false positives
- [ ] API integration tests using `@SpringBootTest` with Testcontainers or an H2 profile
- [ ] Auth tests: expired token, malformed token, accessing another user's data
- [ ] UI bug fixes, loading and error states verified
- [ ] Manual end-to-end pass through every screen

**Evidence:** `./mvnw clean verify` green, a coverage figure, and the model evaluation table.

---

## Milestone 8 — Deployment & Documentation

**Week 12 · Deliverable: live hosted project plus submission-ready documentation.**

- [ ] Deploy PostgreSQL (managed free tier)
- [ ] Deploy Spring Boot backend with the `prod` profile and real environment variables
- [ ] Deploy the Python fraud service
- [ ] Deploy the React frontend, pointed at the deployed API
- [ ] Update `CORS_ALLOWED_ORIGINS` to the deployed frontend origin
- [ ] Generate a strong `JWT_SECRET` for production — never the `.env.example` placeholder
- [ ] Architecture diagram, screenshots, final README pass
- [ ] Project report and demo script
- [ ] Tag the release and verify the tagged build actually runs

**Evidence:** a URL your guide can open, plus the report.

**Free-tier reality check:** free instances sleep after inactivity, so the first request can take 30–60 seconds. Open every service five minutes before a live demo, and keep a recorded video as a fallback.

---

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Kaggle features cannot score real transactions | Milestones 4 and 5 do not connect | Decide the two-track approach in Week 5, not Week 9 |
| Spring Boot 4 / Java 25 is very new | Tutorials do not apply; libraries may lag | Verify library support before adding it; work from reference docs. If a blocker appears, downgrading to Boot 3.x LTS + Java 21 is a legitimate call — make it early, not in Week 10 |
| Forecasting needs long history | LSTM/ARIMA produce noise on sparse data | Generate synthetic multi-month history early; ship rule-based forecasting first |
| Parallel milestones assume no blocking | Schedule slips | Frontend works against mocked responses; agree DTO shapes in Week 4 |
| Four moving parts to deploy at once | Week 12 crunch | Deploy a hello-world version of each service in Week 9 to find platform problems early |
| SMOTE applied before the split | Inflated metrics, unfair questions in the viva | Pipeline it so oversampling cannot touch the test fold |
