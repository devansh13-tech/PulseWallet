import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './index.css'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import App from './App.jsx'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import TransactionPage from './pages/TransactionPage'
import TransactionsPage from './pages/TransactionsPage.jsx'
import BudgetsPage from './pages/BudgetsPage.jsx'
import AlertsPage from './pages/AlertsPage.jsx'
import GoalsPage from './pages/GoalsPage.jsx'
import FinancialPlanPage from './pages/FinancialPlanPage.jsx'
import CreditCardsPage from './pages/CreditCardsPage.jsx'
import LinkedPaymentAccountsPage from './pages/LinkedPaymentAccountsPage.jsx'
import BankIngestionPage from './pages/BankIngestionPage.jsx'
import FraudSecurityCenterPage from './pages/FraudSecurityCenterPage.jsx'
import ProactiveInsightsPage from './pages/ProactiveInsightsPage.jsx'
import CreditAndNetWorthPage from './pages/CreditAndNetWorthPage.jsx'
import InvestmentsAmfiPage from './pages/InvestmentsAmfiPage.jsx'
import GamificationAndSocialPage from './pages/GamificationAndSocialPage.jsx'

import { FloatingPageNav } from './components/PageNavigation.jsx'
import PulseAiAssistant from './components/PulseAiAssistant.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<App />} />
            <Route path="/bank-ingestion" element={<BankIngestionPage />} />
            <Route path="/fraud-center" element={<FraudSecurityCenterPage />} />
            <Route path="/insights" element={<ProactiveInsightsPage />} />
            <Route path="/credit-networth" element={<CreditAndNetWorthPage />} />
            <Route path="/investments" element={<InvestmentsAmfiPage />} />
            <Route path="/social-family" element={<GamificationAndSocialPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/transactions/new" element={<TransactionPage />} />
            <Route path="/budgets" element={<BudgetsPage />} />
            <Route path="/alerts" element={<AlertsPage />} />
            <Route path="/goals" element={<GoalsPage />} />
            <Route path="/financial-plan" element={<FinancialPlanPage />} />
            <Route path="/credit-cards" element={<CreditCardsPage />} />
            <Route path="/linked-accounts" element={<LinkedPaymentAccountsPage />} />
          </Route>
        </Routes>
        <PulseAiAssistant />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
