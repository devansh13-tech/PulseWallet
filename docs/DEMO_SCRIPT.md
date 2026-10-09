# PulseWallet: Live Evaluation & Demonstration Script

A step-by-step walkthrough script for evaluators, examiners, and project assessors. Follow this script to present the full end-to-end functionality of PulseWallet in approximately **10 minutes**.

---

## Pre-Flight Checklist (Run 2 Minutes Before Demo)

Ensure all three local microservices (or Docker containers) are running:
1. **Frontend**: Open browser at `http://localhost:5173` (or port `80` in Docker).
2. **Backend API**: Verify health at `http://localhost:8080/actuator/health` (returns `{"status":"UP"}`).
3. **ML Fraud Engine**: Verify health at `http://localhost:8000/health` (returns `{"status":"healthy","model_loaded":true}`).

---

## Walkthrough Breakdown (10 Minutes)

| Scene | Duration | Feature Area | Key Talking Points |
|---|---|---|---|
| **Scene 1** | 1:00 min | System Overview | Indian FinTech focus, 4-tier microservices, decoupled ML |
| **Scene 2** | 1:00 min | Authentication & Security | BCrypt password hashing, stateless JJWT, IDOR protection |
| **Scene 3** | 2:00 min | Ingestion Engine | RBI Account Aggregator flow, Bank SMS parsing, auto-categorization |
| **Scene 4** | 2:00 min | Real-Time Fraud Sentinel | XGBoost ML inference, 3-tier risk score, instant card freeze |
| **Scene 5** | 2:00 min | Conversational AI Assistant | Natural language query, EMI affordability, goal simulator |
| **Scene 6** | 1:30 min | Indian Financial Hub | Net Worth cockpit, SIP compounder, Section 80C tax planning |
| **Scene 7** | 0:30 min | Testing & Production | 372 automated tests (100% pass), Docker multi-stage builds |

---

### Scene 1: Introduction & Architecture Overview (1:00)

**Speaker Action**:
- Open `http://localhost:5173` on the login screen.
- Highlight the modern glassmorphism UI, tailored color accents, and responsive layout.

**Speaking Notes**:
> *"Welcome to PulseWallet. PulseWallet is an enterprise-grade financial management and real-time fraud detection platform tailored for the modern Indian digital payments ecosystem. 
> Unlike traditional budgeting tools that require manual data entry, PulseWallet combines multi-source automated financial ingestion (via RBI Account Aggregator simulation and bank SMS parsing), an XGBoost machine learning fraud sentinel running sub-15ms inference, a conversational AI advisor, and an Indian tax and net worth planning hub.
> The backend is built with Spring Boot 4 on Java 25, communicating with a Python FastAPI ML microservice and a PostgreSQL 18 database, verified by 372 automated tests."*

---

### Scene 2: Secure Onboarding & Authentication (1:00)

**Examiner Action**:
1. On the Login screen, click **"Create one"** or navigate to Sign Up.
2. Register a new user:
   - **Name**: `Rohan Sharma`
   - **Email**: `rohan@pulsewallet.com`
   - **Password**: `SecurePass@2026`
3. Click **"Create Account"**.
4. You are instantly logged in and redirected to the **Dashboard Overview**.

**Speaking Notes**:
> *"Authentication uses BCrypt password hashing with cost factor 10 and stateless JSON Web Tokens (JJWT) with a 256-bit cryptographic signature. Every API request passes through a Spring Security filter chain. The architecture strictly enforces Insecure Direct Object Reference (IDOR) guards: every database query is partitioned by the authenticated user's ID, ensuring total tenant data isolation."*

---

### Scene 3: Bank & Transaction Ingestion Engine (2:00)

**Examiner Action**:
1. In the sidebar or top navigation, click **"Bank Ingestion"**.
2. **Step 1: Account Aggregator (AA) Simulation**:
   - Click **"Connect Bank (Account Aggregator)"**.
   - Select **HDFC Bank** or **State Bank of India (SBI)**.
   - Click **"Approve Read-Only Consent (90 Days)"**.
   - Notice the simulated accounts populate with live balances and synced transactions.
3. **Step 2: Instant Bank SMS Regex Parser**:
   - In the **"Paste Bank SMS / UPI Alert"** box, paste one of the following authentic Indian bank SMS texts:
     ```text
     Sent Rs. 650.00 from HDFC Bank A/C **4321 to Swiggy on 10-Oct-26 via UPI Ref 4293810294. Bal: Rs. 42,350.00
     ```
   - Click **"Parse & Ingest SMS"**.
   - Notice that the parser automatically extracts:
     - Amount: `₹650.00`
     - Merchant: `Swiggy`
     - Account: `HDFC Bank (4321)`
     - Auto-categorized as: `Food & Dining`
4. Try a travel SMS:
   ```text
   Rs. 320.00 debited from SBI A/c XX8901 on 10-Oct-26 to UBER INDIA UPI ref 998822.
   ```
   - Click **"Parse & Ingest SMS"** $\rightarrow$ Automatically categorized as `Travel & Transport`.

**Speaking Notes**:
> *"Retail banking users do not want to record every coffee or cab manually. PulseWallet implements two ingestion tracks: first, consensual open banking via the RBI Account Aggregator framework, and second, an instant SMS tokenizer that extracts merchant, amount, and account strings across all major Indian banks (SBI, HDFC, ICICI, Axis). The engine maps merchants like Swiggy, Zomato, Uber, and Amazon to categories automatically."*

---

### Scene 4: Real-Time Machine Learning Fraud Sentinel (2:00)

**Examiner Action**:
1. In the sidebar, navigate to **"Fraud Detection"** (or **"Fraud Sentinel"**).
2. Point out the model metrics banner:
   - **Model**: XGBoost Classifier (Trained with SMOTE)
   - **Precision**: 88% | **Recall**: 82% | **PR-AUC**: 0.874
3. **Demo 1: Normal Everyday Transaction**:
   - Click **"Simulate Test Transaction"**.
   - Enter Amount: `₹450`, Time: `14:30`, Merchant: `Local Grocery`.
   - Result: **Risk Score: 4.2 / 100 — Status: LOW (Safe)**.
4. **Demo 2: High-Risk Anomalous Transaction**:
   - Select **"High-Risk Simulation (International / Late Night Outlier)"**.
   - Amount: `₹89,500`, Time: `02:45 AM`, Merchant: `Unknown Offshore Vendor`.
   - Result: **Risk Score: 94.7 / 100 — Status: CRITICAL (Fraud Alert Triggered)**.
   - Observe the alert appear in the **Fraud Alerts Panel** and the top notification bell.
5. **Demo 3: One-Tap Incident Remediation**:
   - In the Fraud Alerts card, click the red **"Freeze / Block Card"** button.
   - Status updates instantly to `CARD BLOCKED` with an audit record in the notification tray.

**Speaking Notes**:
> *"This is PulseWallet's key technical differentiator. When a transaction is submitted, the Spring Boot backend asynchronously or synchronously calls our Python FastAPI microservice over HTTP. The model was trained on 284,807 transactions with extreme class imbalance mitigated by SMOTE. Rather than a binary output, the model returns a calibrated risk score (0-100). If the score exceeds our cost-optimized threshold of 0.50, an alert is persisted in real time, and the user can freeze their card with one tap."*

---

### Scene 5: Conversational AI Financial Assistant (2:00)

**Examiner Action**:
1. Click **"AI Assistant"** in the navigation bar.
2. Select a quick-prompt chip or type directly:
   - Prompt 1: **"Can I afford a new car EMI of ₹25,000 per month with ₹80,000 salary?"**
   - Click **Send**.
   - Review response: Explains Debt-to-Income (DTI) ratio (₹25k is 31.2% of salary), verifies it is within safe banking guidelines ($\le 40\%$), and details impact on disposable income.
3. Prompt 2: **"How can I save ₹10 Lakhs for a down payment in 3 years?"**
   - Click **Send**.
   - Review response: Shows the exact required monthly SIP (approx. ₹23,200/month assuming 12% equity mutual fund returns).
4. Prompt 3: **"Analyze my spending based on the 50/30/20 rule."**
   - Review response: Breaks down user's Needs vs Wants vs Savings percentages.

**Speaking Notes**:
> *"The Conversational AI Assistant is not a generic hallucinating chatbot. It pairs natural language capabilities with deterministic mathematical engines. It performs Debt-to-Income calculations, SIP compounding projections, and 50/30/20 budget allocations directly tailored to the user's recorded financial ledger."*

---

### Scene 6: Indian Financial Hub (Net Worth, SIP & Tax) (1:30)

**Examiner Action**:
1. Click **"Financial Hub"** (or **"Indian Finance Hub"**).
2. **Net Worth Tab**:
   - View visual asset allocation donut chart: Equity, Mutual Funds, EPF, Gold, Real Estate vs Liabilities.
3. **Compound SIP Calculator Tab**:
   - Adjust slider: Monthly Investment = `₹15,000`, Duration = `10 Years`, Expected Return = `12%`.
   - Instant calculation:
     - Invested Amount: `₹18,00,000`
     - Estimated Wealth Gain: `₹16,85,160`
     - Total Maturity Value: `₹34,85,160`
4. **Section 80C & 80D Tax Planner Tab**:
   - Shows current 80C deductions utilized (e.g. EPF, PPF, ELSS) out of the ₹1,50,000 cap.
   - Highlights ₹50,000 additional NPS Tier 1 tax deduction under Section 80CCD(1B).

**Speaking Notes**:
> *"The Indian Financial Hub provides tools specifically calibrated for Indian investors: Section 80C deduction tracking, Section 80D medical insurance optimization, NPS tax shields, and a monthly SIP compound calculator to plan long-term wealth creation."*

---

### Scene 7: Production Readiness, Tests & Wrap-Up (0:30)

**Examiner Action**:
- Show terminal or mention the test suite:
  - Command: `.\mvnw.cmd test -DargLine="-Xmx512m"`
  - **372 Automated Tests Run: 0 Failures, 0 Errors, 100% Pass Rate**.
  - Multi-container Docker deployment: `docker compose up -d`.

**Speaking Notes**:
> *"To ensure enterprise robustness, PulseWallet features 372 automated tests spanning unit logic, edge cases, IDOR security tests, and WebMvc integration controllers. The entire multi-tier stack is containerized with Docker multi-stage builds and Nginx caching. Thank you, and I am happy to take any questions."*

---

## Frequently Asked Questions (Examiner FAQ)

**Q1: How does the fraud detection model handle the extreme 0.17% class imbalance?**
> *Answer*: We implemented Synthetic Minority Oversampling Technique (SMOTE) strictly on the training partition after a stratified 80:20 split. This prevented data leakage while teaching the XGBoost tree ensemble to identify minority fraud characteristics without memorizing the noise.

**Q2: What happens if the Python fraud detection microservice goes down?**
> *Answer*: We engineered graceful degradation with Spring's RestClient. If the Python service times out or crashes, the backend catches the exception, logs a warning, flags the transaction with a fallback status, and allows the user's transaction to complete without throwing an unhandled 500 error.

**Q3: How is user data isolated in multi-tenant environments?**
> *Answer*: Insecure Direct Object Reference (IDOR) attacks are defended at the Spring Data JPA repository layer. Every query requires the authenticated user's ID extracted from the validated JWT token (`WHERE t.user.id = :userId`), ensuring no user can view or mutate another user's financial data.
