# PulseWallet ⚡

## Enterprise Personal Financial Intelligence & Real-Time Fraud Sentinel

PulseWallet is a production-grade fintech platform that combines **Indian-context personal financial planning** (RBI Account Aggregator, AMFI mutual funds, CIBIL credit tracking, Section 80C tax planning) with an **intelligent transaction fraud detection sentinel** and a **conversational AI financial copilot**.

Built with a high-performance **Spring Boot 4 / Java 25** core, a **Python FastAPI ML engine**, and a sleek **React 18 / Vite** frontend with GPU-accelerated financial ambient themes.

---

## 🌟 Key Capabilities

### 1. 🏛️ Real Bank & Transaction Ingestion
- **RBI Account Aggregator (AA) Framework**: Consent flows modeled after **Setu, Finvu, and Anumati** for consent-driven bank data retrieval (`@okhdfcbank` handles, OTP verification, automated transaction fetch).
- **UPI, SMS & Statement Parsing**: Rule-based & regex statement parser extracting amount, debit/credit type, merchant (Swiggy, Zomato, Uber, Amazon), reference numbers (UTR/RRN), and bank accounts directly from SMS texts.
- **ML & Feedback Auto-Categorization**: Intelligent classifier that automatically learns from user category overrides (e.g., mapping `"SWIGGY"` to `Food & Dining`).

### 2. 🛡️ Advanced Fraud Security Sentinel
- **Anomaly Detection Radar**: Real-time multi-dimensional scoring evaluating amount deviations (e.g. 3x usual spend), unusual transaction times (2:00 AM alerts), geographic hops, and newly flagged merchants.
- **Explainable Risk Scores (0–100)**: Plain-English explanations behind every alert (*"3.4x usual ticket size, unverified international IP, 02:40 AM"*).
- **Instant Defensive Actions**: One-tap *"Freeze Card"*, *"Not Me / Dispute"*, and *"Flag Merchant"*.
- **Phishing & UPI-Scam Scanner**: Natural-language heuristic scanner detecting deceptive collect requests, urgency words, and spoofed KYC links.
- **Downloadable Forensic Fraud Report**: Generates structured forensic audit reports ready for presentation or compliance audits.

### 3. 🤖 Conversational AI Financial Assistant (Pulse AI)
- **Live Financial Context Q&A**: Answers natural language questions directly using personal ledgers:
  - *"Can I afford a bike EMI of ₹6,500?"*
  - *"Why was my spending high in September?"*
  - *"How do I reach my BMW goal sooner?"*
- **Natural-Language Transaction Entry**: Type *"spent 450 on lunch at Haldirams"* or speak into the microphone to automatically draft and stage transactions.
- **Voice & Regional Language Engine**: Web Speech API integration with support for **English, Hindi (हिंदी), and Marathi (मराठी)**.
- **Context-Aware Privacy**: The assistant bubble is exclusively available to authenticated sessions and remains invisible on public screens.

### 4. 💡 Proactive Insights Engine
- **Safe-to-Spend-Today**: Real-time daily cash-flow formula `(Disposable Income - Committed Recurring Bills) ÷ Days Remaining in Month`.
- **Subscription & Recurring Payment Detector**: Auto-detects periodic subscriptions (Netflix, Spotify, AWS, Broadband, Gym).
- **Bill Reminders & Due Date Tracking**: Urgency flags and days-until-due badges.

### 5. 💰 Credit & Net Worth Hub
- **CIBIL / Experian Credit Score**: Real-time gauge (784/900 tier), score factors (On-time payments, Credit utilization < 22%, Credit age).
- **Debt Payoff Planner**: Interactive **Debt Snowball** (lowest balance first) vs. **Debt Avalanche** (highest interest first) schedules with total interest saved.
- **Consolidated Net Worth View**: Aggregates bank balances, mutual funds, gold, and real estate minus active loans and card liabilities.
- **Indian Tax Hub**: **Section 80C tracker** (₹1.5 Lakh limit with EPF, ELSS, PPF), LTCG/STCG capital gains summary, and ITR filing checklist.

### 6. 📈 AMFI Investments & Goal-Based SIPs
- **AMFI Mutual Funds Catalog**: Live NAV and 1Y/3Y returns across benchmark Indian schemes (Parag Parikh Flexi Cap, Nippon Small Cap, Mirae Asset Large Cap, HDFC Top 100).
- **Portfolio Tracker with XIRR**: Tracks invested value, market valuation, and annualized returns.
- **Goal-Based SIP Execution**: Connects goals (e.g. *"Emergency Reserve"*, *"Dream Car"*) to specific mutual funds with an ECS/NACH e-mandate simulation.

### 7. 👥 Gamification, Retention & Social Features
- **Financial Health Score (0–100)**: Holistic 4-pillar index across Budgeting, Emergency Fund, Debt Ratio, and Investments.
- **Streaks & Badges**: 14-day tracking streak, *"Frugal Ninja"*, *"Debt Slayer"*, *"SIP Champion"*.
- **Round-Up Savings**: Automatically rounds up transactions to the nearest ₹10 or ₹50 and diverts spare change into savings goals.
- **Shared Wallets & Bill Splitting**: Shared family budgets with spending limits for dependents, plus bill splitting with friends via shareable UPI payment links and instant QR codes.

### 8. 🔐 Enterprise Security & DPDP Compliance
- **Authentication**: JWT authentication with BCrypt hashing, 2FA biometric passkey simulation, and phone SMS OTP verification.
- **Audit Logs**: Immutable security audit trail recording IP addresses, device identifiers, and timestamped actions.
- **DPDP Act 2023 Consent Manager**: Granular toggles for bank data access, AI profiling, and transaction parsing with one-tap consent withdrawal.

### 9. 🎨 Trust-First UX & Ambient Canvas
- **Unified Auth Screen**: Centered 420px card with segmented Sign In / Create Account tabs, password show/hide toggle, Google/Phone OTP login, and a 3-step onboarding wizard.
- **Ambient Financial Themes**: Wealth & Gold (🪙), Cyber Stock (📈), Dalal Street NSE (🇮🇳), and Titanium Bank (🏛️).
- **Subtle Particle Trust Mode**: GPU canvas backdrop with soft 0.08–0.18 alpha particles and dynamic intensity toggle (`🛡️ Subtle` / `⚡ Dynamic`).

---

## 🏗️ Architecture & Tech Stack

```text
               +----------------------------------------+
               |        React 18 + Vite Frontend        |
               |      (Recharts, Web Speech, Canvas)    |
               +-------------------+--------------------+
                                   |
                                   | REST + JWT
                                   v
               +----------------------------------------+
               |       Spring Boot 4.1 (Java 25)        |
               | (Security, JPA, Flyway, Actuator, DTOs)|
               +---------+--------------------+---------+
                         |                    |
             JDBC / JPA  |                    | HTTP / JSON
                         v                    v
               +------------------+  +------------------+
               |  PostgreSQL 17+  |  |  Python FastAPI  |
               |  (Flyway V1-V16) |  |  (XGBoost / ML)  |
               +------------------+  +------------------+
```

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, Vite, Recharts, Vanilla CSS Design System, Web Speech API |
| **Backend API** | Java 25, Spring Boot 4.1.0, Spring Security (JWT), Spring Data JPA, Flyway Migrations |
| **Machine Learning** | Python 3.12+, FastAPI, Uvicorn, Scikit-learn, XGBoost, Pandas |
| **Database** | PostgreSQL 17 / 18, Docker Compose |
| **Tooling & Build** | Maven Wrapper (`mvnw`), npm, Docker |

---

## 🚀 Deployment & Quick Start

### Option A: One-Command Full Stack (Docker Compose) Recommended
To spin up all 4 services (PostgreSQL, Python FastAPI Fraud ML, Spring Boot Backend, and React Nginx Frontend) simultaneously:

```bash
# 1. Clone repository and copy environment configuration
cp .env.example .env

# 2. Build and run the entire multi-container stack
docker compose up -d --build

# 3. Access applications:
# - Frontend Web App:     http://localhost (or http://localhost:80)
# - Spring Boot API:      http://localhost:8080/api
# - Actuator Health:      http://localhost:8080/actuator/health
# - Fraud ML Engine:      http://localhost:8000/health
# - Optional pgAdmin:     docker compose --profile tools up -d (http://localhost:5050)
```

To stop containers while preserving persistent database volumes:
```bash
docker compose down
```

---

### Option B: Production Container Orchestration
For hardened staging and production cloud servers (AWS ECS, DigitalOcean, Render, GCP Cloud Run):

```bash
# Copy and customize production secrets (set strong JWT_SECRET and DB_PASSWORD)
cp .env.production.example .env.production

# Launch with resource caps and production profile
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

---

### Option C: Local Development (Bare-Metal)

#### 1. Prerequisites
- **JDK 25** (`java -version`)
- **Node.js 20+** & npm (`node -v`)
- **Docker Desktop** (or a local PostgreSQL instance on port 5432)
- **Python 3.11+** (for ML service)

#### 2. Database Setup
Start PostgreSQL via Docker Compose:
```bash
docker compose up -d postgres
```
*(Or point to your local PostgreSQL database named `pulsewallet_db` on port 5432).*

#### 3. Backend (Spring Boot 4 / Java 25)
Set the required JWT secret and launch the backend:

**Windows PowerShell:**
```powershell
$env:JWT_SECRET = "4rGptm2fdDvHwcFiPlXj3bO3eAaQ0qhc+5aGnU/rIp36QSDiesK7PqvNPgj77jei"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
export JWT_SECRET="4rGptm2fdDvHwcFiPlXj3bO3eAaQ0qhc+5aGnU/rIp36QSDiesK7PqvNPgj77jei"
./mvnw spring-boot:run
```
*The backend starts at `http://localhost:8080` and automatically runs Flyway migrations up to V16.*

#### 4. Python ML Fraud Sentinel
```bash
cd fraud-detection
python -m venv .venv
.\.venv\Scripts\activate   # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
python -m uvicorn api.main:app --host 127.0.0.1 --port 8000
```
*The ML API runs at `http://localhost:8000`.*

#### 5. Frontend (React 19 / Vite)
```bash
cd frontend
npm install
npm run dev
```
*The web dashboard is live at `http://localhost:5173`.*

---

### Option D: Standalone Production JAR Execution
```powershell
# 1. Package the production JAR
.\mvnw.cmd package -DskipTests -DargLine="-Xmx512m"

# 2. Run with production profile
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/pulsewallet_db"
$env:DB_USERNAME = "pulsewallet"
$env:DB_PASSWORD = "pulsewallet"
$env:JWT_SECRET = "4rGptm2fdDvHwcFiPlXj3bO3eAaQ0qhc+5aGnU/rIp36QSDiesK7PqvNPgj77jei"
java -jar target/pulsewallet-0.0.1-SNAPSHOT.jar
```

---

## 📡 API Reference

All protected endpoints require an `Authorization: Bearer <JWT>` header obtained via `/api/auth/login` or `/api/auth/register`.

### Authentication & Core
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new user and issue JWT |
| `POST` | `/api/auth/login` | Authenticate user credentials |
| `GET` | `/api/dashboard-summary` | Aggregated financial metrics and budget progress |
| `GET` | `/actuator/health` | Application health and database status |

### Bank Ingestion & Account Aggregator
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/bank-ingestion/parse-sms` | Parse SMS / UPI debit notifications into transaction drafts |
| `POST` | `/api/bank-ingestion/account-aggregator/consent` | Simulate RBI Account Aggregator consent flow (Setu/Finvu) |
| `POST` | `/api/bank-ingestion/category-feedback` | Submit merchant-to-category feedback for model learning |
| `POST` | `/api/bank-ingestion/import-transactions` | Batch import verified parsed transactions into user ledger |

### Fraud Security Center
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/fraud-center/security-center` | Real-time security posture, active anomalies, risk scores |
| `POST` | `/api/fraud-center/scan-phishing` | Scan suspicious messages / UPI collect requests for scam heuristics |
| `POST` | `/api/fraud-center/action` | Perform defense actions (`FREEZE_CARD`, `DISPUTE_TRANSACTION`, `FLAG_MERCHANT`) |
| `GET` | `/api/fraud-center/investigation-report` | Download structured forensic fraud audit report |

### Conversational AI Assistant
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ai/assistant/chat` | Contextual Q&A using user financial data, transaction staging, and language translation |

### Financial Hub (Insights, Credit, Taxes)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/financial-hub/proactive-insights` | Safe-to-spend-today, recurring subscriptions, bill reminders |
| `GET` | `/api/financial-hub/cibil-score` | CIBIL credit score gauge, payment history, utilization |
| `GET` | `/api/financial-hub/debt-payoff` | Debt Snowball vs. Avalanche comparative payoff schedules |
| `GET` | `/api/financial-hub/net-worth` | Consolidated net worth breakdown across liquid, equity, and physical assets |
| `GET` | `/api/financial-hub/tax-tracker` | Indian Section 80C deduction tracker, capital gains, ITR guide |

### Core Financial Entities
| Method | Endpoint | Description |
|---|---|---|
| `GET` / `POST` | `/api/transactions` | Filter, paginate, and record transactions |
| `GET` / `POST` | `/api/budgets` | Category budget tracking and period allocations |
| `GET` / `POST` | `/api/goals` | Savings goals, target dates, and monthly contribution plans |
| `GET` / `POST` | `/api/linked-accounts` | Linked bank accounts and liquid balance tracking |
| `GET` | `/api/credit-cards/intelligence` | Card utilization, bill dates, and reward benefits |

---

## 🧪 Testing

### Running Unit & Integration Tests
```powershell
# Maven tests (using 512MB memory boundary)
.\mvnw.cmd test -DargLine="-Xmx512m"
```

### Verifying Frontend Build
```bash
cd frontend
npm run build
```
*(Transpiles and validates 630+ modules with 0 errors).*

---

## 📚 Technical Documentation Suite

| Document | Purpose | Description |
|---|---|---|
| [System Architecture](file:///docs/ARCHITECTURE.md) | Technical Deep Dive | Microservices topology, component relationships, data flow diagrams (Mermaid), and security boundary models. |
| [Comprehensive Project Report](file:///docs/PROJECT_REPORT.md) | Engineering Report | Problem statement, Indian FinTech market context, ML model training & metrics, and quality assurance summary. |
| [Live Evaluator Demo Script](file:///docs/DEMO_SCRIPT.md) | Presentation Guide | Minute-by-minute walkthrough script with click paths, copy-paste inputs, and examiner talking points. |
| [Fraud ML Model Evaluation](file:///docs/FRAUD_MODEL_EVALUATION.md) | Data Science Report | Precision-recall curves, confusion matrices, cost-weighted loss function, and concept drift mitigation strategy. |
| [Branching & Contributing](file:///docs/BRANCHING.md) | Engineering Standards | Git branching strategy, commit conventions, and pull request workflows. |
| [Project Progress Tracker](file:///PROJECT_PROGRESS.md) | Audit Trail | 92-task verified progress record documenting Milestones 1 through 8 at 100% completion. |

---

## 📄 License
This project is open-source under the MIT License.

