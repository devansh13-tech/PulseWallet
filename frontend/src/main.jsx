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

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<App />} />
            <Route path="/transactions" element={<TransactionsPage />} />
            <Route path="/transactions/new" element={<TransactionPage />} />
            <Route path="/budgets" element={<BudgetsPage />} />
            <Route path="/alerts" element={<AlertsPage />} />
            <Route path="/goals" element={<GoalsPage />} />
            <Route path="/financial-plan" element={<FinancialPlanPage />} />
            <Route path="/credit-cards" element={<CreditCardsPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
