import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
} from 'recharts'
import { useAuth } from './context/AuthContext'
import {
  advisoryApi,
  creditCardApi,
  dashboardApi,
  exchangeRateApi,
  forecastApi,
  financialPlanApi,
  goalApi,
  linkedPaymentAccountApi,
  notificationApi,
  recommendationApi,
  summaryApi,
  transactionApi,
} from './lib/api'
import CurrencySelector from './components/CurrencySelector'
import LiveMarketWidget from './components/LiveMarketWidget'
import IncomeAdvisoryColumn from './components/IncomeAdvisoryColumn'
import MoneyCanvasAnimation from './components/MoneyCanvasAnimation'
import FinancialTickerTape from './components/FinancialTickerTape'
import ThemeSwitcher from './components/ThemeSwitcher'
import { PageNavHeader, PageNavFooter } from './components/PageNavigation.jsx'
import OnboardingWizardModal from './components/OnboardingWizardModal.jsx'
import SecurityAndComplianceModal from './components/SecurityAndComplianceModal.jsx'
import {
  formatMoneyAmount,
  formatNumber,
  setStoredExchangeRates,
  useSelectedCurrency,
} from './utils/currency'
import './App.css'

const DASHBOARD_INDIAN_BANKS = [
  { name: 'HDFC Bank', code: 'HDFC' },
  { name: 'State Bank of India', code: 'SBIN' },
  { name: 'ICICI Bank', code: 'ICIC' },
  { name: 'Axis Bank', code: 'UTIB' },
  { name: 'Kotak Mahindra Bank', code: 'KKBK' },
  { name: 'Punjab National Bank', code: 'PUNB' },
  { name: 'Bank of Baroda', code: 'BARB' },
]

const navItems = [
  { label: 'Dashboard', path: '/' },
  { label: '🏛️ Bank Ingestion', path: '/bank-ingestion' },
  { label: '🛡️ Fraud Security', path: '/fraud-center' },
  { label: '💡 Proactive Insights', path: '/insights' },
  { label: '💰 Credit & Net Worth', path: '/credit-networth' },
  { label: '📈 AMFI Investments', path: '/investments' },
  { label: '👥 Gamification & Social', path: '/social-family' },
  { label: 'Transactions', path: '/transactions' },
  { label: 'Budgets', path: '/budgets' },
  { label: 'Goals', path: '/goals' },
  { label: 'Financial Plan', path: '/financial-plan' },
  { label: 'Credit Cards', path: '/credit-cards' },
  { label: 'Linked Accounts', path: '/linked-accounts' },
  { label: 'Alerts', path: '/alerts' },
  { label: 'New Transaction', path: '/transactions/new' },
]

const chartPalette = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6']

function formatMoney(value, currencyCode) {
  return formatMoneyAmount(value, currencyCode)
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

function getListData(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.content)) return payload.content
  return []
}

function App() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const selectedCurrency = useSelectedCurrency()
  const [dashboardData, setDashboardData] = useState(null)
  const [financialSummary, setFinancialSummary] = useState(null)
  const [forecast, setForecast] = useState(null)
  const [advisory, setAdvisory] = useState(null)
  const [recommendations, setRecommendations] = useState(null)
  const [financialPlan, setFinancialPlan] = useState(null)
  const [recommendationError, setRecommendationError] = useState('')
  const [goals, setGoals] = useState([])
  const [transactions, setTransactions] = useState([])
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [creditCardIntel, setCreditCardIntel] = useState(null)
  const [linkedAccounts, setLinkedAccounts] = useState([])
  const [editingBalanceAccount, setEditingBalanceAccount] = useState(null)
  const [newBalanceInput, setNewBalanceInput] = useState('')
  const [updatingBalance, setUpdatingBalance] = useState(false)
  const [showQuickLinkModal, setShowQuickLinkModal] = useState(false)
  const [quickBankForm, setQuickBankForm] = useState({
    providerName: '',
    accountName: '',
    accountSubtype: 'Savings Account',
    maskedIdentifier: '',
    ifscCode: '',
    balance: '0',
  })
  const [savingQuickBank, setSavingQuickBank] = useState(false)
  const [bankSuccessMsg, setBankSuccessMsg] = useState('')
  const [theme, setTheme] = useState(() => localStorage.getItem('pulsewallet_theme') || 'money')
  const [particlesEnabled, setParticlesEnabled] = useState(true)
  const [particlesSubtle, setParticlesSubtle] = useState(true)
  const [showOnboardingModal, setShowOnboardingModal] = useState(false)
  const [showSecurityModal, setShowSecurityModal] = useState(false)

  const handleThemeChange = (newTheme) => {
    setTheme(newTheme)
    localStorage.setItem('pulsewallet_theme', newTheme)
  }

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refreshDashboard = async () => {
    setLoading(true)
    setError('')

    try {
      const [dashboardResult, summaryResult, forecastResult, advisoryResult, recommendationResult, financialPlanResult, goalsResult, transactionsResult, notificationsResult, unreadCountResult, rateResult, creditCardResult, linkedAccountsResult] = await Promise.allSettled([
        dashboardApi.summary(),
        summaryApi.get({}),
        forecastApi.get(),
        advisoryApi.get(),
        recommendationApi.get(),
        financialPlanApi.get(),
        goalApi.list(),
        transactionApi.list({ page: 0, size: 8, sort: 'transactionDate,desc' }),
        notificationApi.list(),
        notificationApi.unreadCount(),
        exchangeRateApi.rates({ base: 'INR' }),
        creditCardApi.intelligence(),
        linkedPaymentAccountApi.list(),
      ])

      const dashboard = dashboardResult.status === 'fulfilled' ? dashboardResult.value : null
      const financial = summaryResult.status === 'fulfilled' ? summaryResult.value : null
      const forecastData = forecastResult.status === 'fulfilled' ? forecastResult.value : null
      const advisoryData = advisoryResult.status === 'fulfilled' ? advisoryResult.value : null
      const recommendationData = recommendationResult.status === 'fulfilled' ? recommendationResult.value : null
      const financialPlanData = financialPlanResult.status === 'fulfilled' ? financialPlanResult.value : null
      const goalData = goalsResult.status === 'fulfilled' ? goalsResult.value : []
      setRecommendationError(recommendationResult.status === 'rejected' ? 'Recommendations are unavailable right now.' : '')
      const transactionData = transactionsResult.status === 'fulfilled' ? getListData(transactionsResult.value) : []
      const notificationData = notificationsResult.status === 'fulfilled' ? getListData(notificationsResult.value) : []
      const unread = unreadCountResult.status === 'fulfilled' ? Number(unreadCountResult.value ?? 0) : 0
      const currentRates = rateResult.status === 'fulfilled' ? rateResult.value?.rates || null : null
      const creditCardData = creditCardResult.status === 'fulfilled' ? creditCardResult.value : null
      const linkedAccountsData = linkedAccountsResult.status === 'fulfilled' ? getListData(linkedAccountsResult.value) : []

      if (currentRates) {
        setStoredExchangeRates(currentRates)
      }

      setDashboardData(dashboard)
      setFinancialSummary(financial || dashboard?.financialSummary || null)
      setForecast(forecastData)
      setAdvisory(advisoryData)
      setRecommendations(recommendationData)
      setFinancialPlan(financialPlanData)
      setGoals(goalData)
      setTransactions(transactionData)
      setNotifications(notificationData.slice(0, 5))
      setUnreadCount(unread)
      setCreditCardIntel(creditCardData)
      setLinkedAccounts(linkedAccountsData)

      if (!dashboard && !financial && !forecastData && !advisoryData && !transactionData.length && !notificationData.length && !linkedAccountsData.length) {
        setError('Unable to load dashboard. Try again.')
      }
    } catch (err) {
      setError(err?.message || 'Unable to load dashboard. Try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refreshDashboard()
    const intervalId = window.setInterval(refreshDashboard, 60000)
    return () => window.clearInterval(intervalId)
  }, [])

  const stats = useMemo(() => {
    const totalIncome = Number(recommendations?.monthlyIncome ?? financialSummary?.totalIncome ?? dashboardData?.financialSummary?.totalIncome ?? 0)
    const totalExpenses = Number(recommendations?.totalExpenses ?? financialSummary?.totalExpenses ?? dashboardData?.financialSummary?.totalExpenses ?? 0)
    const disposable = Number(recommendations?.monthlyLeft ?? financialSummary?.disposableIncome ?? dashboardData?.financialSummary?.disposableIncome ?? totalIncome - totalExpenses)
    const savings = Number(recommendations?.monthlySavings ?? advisory?.recommendedSavings ?? 0)
    const investments = Number(recommendations?.investments ?? 0)
    const goals = Number(recommendations?.goalSavings ?? 0)
    const totalLiquidBank = (linkedAccounts || [])
      .filter((a) => a.status === 'CONNECTED')
      .reduce((sum, a) => sum + Number(a.balance || 0), 0)

    return [
      { label: 'Bank balance', value: formatMoney(totalLiquidBank, selectedCurrency), tone: 'positive' },
      { label: 'Total income', value: formatMoney(totalIncome, selectedCurrency), tone: 'positive' },
      { label: 'Total expenses', value: formatMoney(totalExpenses, selectedCurrency), tone: 'neutral' },
      { label: 'Monthly left', value: formatMoney(disposable, selectedCurrency), tone: disposable >= 0 ? 'positive' : 'warning' },
      { label: 'Monthly savings', value: formatMoney(savings, selectedCurrency), tone: 'positive' },
      { label: 'Investments', value: formatMoney(investments, selectedCurrency), tone: 'neutral' },
    ]
  }, [dashboardData, financialSummary, advisory, recommendations, selectedCurrency, linkedAccounts])

  const categoryBreakdown = useMemo(() => {
    const source = financialSummary?.expensesByCategory || dashboardData?.financialSummary?.expensesByCategory || []
    return source.map((item, index) => ({
      name: item.categoryName || `Category ${index + 1}`,
      value: Number(item.total ?? 0),
      fill: chartPalette[index % chartPalette.length],
    }))
  }, [dashboardData, financialSummary])

  const goalSummary = useMemo(() => {
    const activeGoals = goals.filter((goal) => goal.status !== 'COMPLETED')
    return {
      activeCount: activeGoals.length,
      target: activeGoals.reduce((sum, goal) => sum + Number(goal.targetAmount ?? 0), 0),
      saved: activeGoals.reduce((sum, goal) => sum + Number(goal.currentAmount ?? 0), 0),
      remaining: activeGoals.reduce((sum, goal) => sum + Number(goal.remainingAmount ?? 0), 0),
      monthly: activeGoals.reduce((sum, goal) => sum + Number(goal.requiredMonthlySaving ?? 0), 0),
    }
  }, [goals])

  const monthlyTrend = useMemo(() => {
    const source = financialSummary?.monthlyIncomeExpense || dashboardData?.financialSummary?.monthlyIncomeExpense || []
    const groups = new Map()

    source.forEach((item) => {
      const monthKey = `${item.year}-${String(item.month).padStart(2, '0')}`
      if (!groups.has(monthKey)) {
        groups.set(monthKey, { month: new Intl.DateTimeFormat('en-US', { month: 'short' }).format(new Date(item.year, item.month - 1, 1)), income: 0, expenses: 0 })
      }
      const bucket = groups.get(monthKey)
      if (item.type === 'INCOME') bucket.income = Number(item.total ?? 0)
      if (item.type === 'EXPENSE') bucket.expenses = Number(item.total ?? 0)
    })

    return Array.from(groups.values()).slice(-6)
  }, [dashboardData, financialSummary])

  const budgetCards = useMemo(() => {
    const budgets = dashboardData?.budgets || []
    const transactionList = transactions || []

    return budgets.map((budget) => {
      const start = budget.startDate ? new Date(budget.startDate) : null
      const end = budget.endDate ? new Date(budget.endDate) : null
      const budgetAmount = Number(budget.amount ?? 0)

      const spent = transactionList.reduce((sum, transaction) => {
        if (transaction.type !== 'EXPENSE') return sum
        const matchCategory = !budget.categoryId || String(transaction.categoryId) === String(budget.categoryId)
        const txDate = transaction.transactionDate ? new Date(transaction.transactionDate) : null
        const inRange = (!start || !txDate || txDate >= start) && (!end || !txDate || txDate <= end)
        if (!matchCategory || !inRange) return sum
        return sum + Number(transaction.amount ?? 0)
      }, 0)

      return {
        ...budget,
        spent,
        remaining: budgetAmount - spent,
        percent: budgetAmount > 0 ? Math.min(100, Math.max(0, (spent / budgetAmount) * 100)) : 0,
      }
    })
  }, [dashboardData, transactions])

  const recentTransactions = useMemo(() => transactions.slice(0, 5), [transactions])
  const recentAlerts = notifications.slice(0, 5)

  async function handleMarkRead(notificationId) {
    try {
      await notificationApi.markRead(notificationId)
      setNotifications((current) => current.map((item) => item.id === notificationId ? { ...item, read: true } : item))
      setUnreadCount((current) => Math.max(0, current - 1))
    } catch (error) {
      setError(error?.message || 'Unable to update notification.')
    }
  }

  async function handleUpdateBankBalance(e) {
    e.preventDefault()
    if (!editingBalanceAccount) return
    setUpdatingBalance(true)
    try {
      const updated = await linkedPaymentAccountApi.updateBalance(editingBalanceAccount.id, parseFloat(newBalanceInput || '0'))
      setLinkedAccounts((current) => current.map((acc) => acc.id === updated.id ? updated : acc))
      setEditingBalanceAccount(null)
      setBankSuccessMsg('Bank balance updated successfully!')
      setTimeout(() => setBankSuccessMsg(''), 4000)
    } catch (err) {
      alert(err?.message || 'Failed to update balance')
    } finally {
      setUpdatingBalance(false)
    }
  }

  async function handleQuickLinkBank(e) {
    e.preventDefault()
    setSavingQuickBank(true)
    try {
      let masked = quickBankForm.maskedIdentifier.trim()
      if (!masked.startsWith('••••') && !masked.startsWith('****') && masked.length <= 4) {
        masked = `•••• ${masked}`
      }
      const created = await linkedPaymentAccountApi.create({
        providerName: quickBankForm.providerName.trim(),
        accountType: 'BANK',
        accountName: quickBankForm.accountName.trim(),
        accountSubtype: quickBankForm.accountSubtype,
        maskedIdentifier: masked,
        ifscCode: quickBankForm.ifscCode ? quickBankForm.ifscCode.trim().toUpperCase() : null,
        balance: parseFloat(quickBankForm.balance || '0'),
      })
      setLinkedAccounts((current) => [created, ...current])
      setShowQuickLinkModal(false)
      setQuickBankForm({
        providerName: '',
        accountName: '',
        accountSubtype: 'Savings Account',
        maskedIdentifier: '',
        ifscCode: '',
        balance: '0',
      })
      setBankSuccessMsg(`Linked ${created.providerName} successfully!`)
      setTimeout(() => setBankSuccessMsg(''), 4000)
    } catch (err) {
      alert(err?.message || 'Failed to link bank account')
    } finally {
      setSavingQuickBank(false)
    }
  }

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell" data-theme={theme}>
      <MoneyCanvasAnimation theme={theme} enabled={particlesEnabled} subtle={particlesSubtle} />

      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">P</div>
          <div>
            <p className="eyebrow">PulseWallet</p>
            <h1>Finance</h1>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main navigation">
          {navItems.map((item) => (
            <button
              key={item.label}
              type="button"
              className={window.location.pathname === item.path ? 'nav-item active' : 'nav-item'}
              onClick={() => navigate(item.path)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-card">
          <p className="eyebrow">Security status</p>
          <strong>Protected</strong>
          <span>Fraud detection is active</span>
        </div>

        <div className="sidebar-user">
          {user && <span className="user-name">{user.name}</span>}
          <button type="button" className="logout-btn" onClick={handleLogout}>Sign out</button>
        </div>
      </aside>

      <main className="main-panel">
        <PageNavHeader title="Dashboard" />
        <header className="topbar">
          <div>
            <p className="eyebrow">{user ? `Welcome, ${user.name}` : 'Welcome back'}</p>
            <h2>Dashboard</h2>
          </div>

          <div className="topbar-actions">
            <ThemeSwitcher
              currentTheme={theme}
              onThemeChange={handleThemeChange}
              particlesEnabled={particlesEnabled}
              onToggleParticles={() => setParticlesEnabled((p) => !p)}
              particlesSubtle={particlesSubtle}
              onToggleSubtle={() => setParticlesSubtle((s) => !s)}
            />
            <button
              type="button"
              className="secondary-btn"
              onClick={() => setShowOnboardingModal(true)}
              title="Complete rapid 2-minute onboarding flow"
              style={{ background: 'rgba(37, 99, 235, 0.1)', borderColor: 'var(--accent-primary, #3b82f6)' }}
            >
              ⚡ Onboarding (&lt; 2m)
            </button>
            <button
              type="button"
              className="secondary-btn"
              onClick={() => setShowSecurityModal(true)}
              title="Security, 2FA Passkeys and DPDP Act 2023 Consent"
              style={{ background: 'rgba(16, 185, 129, 0.1)', borderColor: '#10b981' }}
            >
              🛡️ Security & DPDP
            </button>
            <CurrencySelector />
            <button type="button" className="secondary-btn" onClick={refreshDashboard}>Refresh</button>
            <button type="button" className="primary-btn" onClick={() => navigate('/transactions/new')}>New transaction</button>
          </div>
        </header>

        <FinancialTickerTape theme={theme} />

        <PageNavHeader title="Financial Overview & Recommendations" />

        {error && <div className="inline-error" role="alert">{error}</div>}

        {loading ? (
          <div className="loader-row">Loading dashboard…</div>
        ) : (
          <>
            <div
              className="fintech-features-banner"
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
                gap: '0.85rem',
                marginBottom: '1.25rem',
              }}
            >
              <div
                className="feature-banner-card"
                onClick={() => navigate('/insights')}
                style={{
                  background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(16, 185, 129, 0.08))',
                  border: '1px solid rgba(59, 130, 246, 0.3)',
                  borderRadius: '12px',
                  padding: '0.85rem 1rem',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#60a5fa', fontWeight: 700, letterSpacing: '0.04em' }}>Proactive Daily Insight</span>
                  <h4 style={{ margin: '0.2rem 0 0.1rem', fontSize: '1.02rem', color: '#f8fafc' }}>Safe to Spend: ₹2,140/day</h4>
                  <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)' }}>Factoring recurring bills & savings</p>
                </div>
                <span style={{ fontSize: '1.4rem' }}>💡</span>
              </div>

              <div
                className="feature-banner-card"
                onClick={() => navigate('/fraud-center')}
                style={{
                  background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(245, 158, 11, 0.08))',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  borderRadius: '12px',
                  padding: '0.85rem 1rem',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#f87171', fontWeight: 700, letterSpacing: '0.04em' }}>AI Fraud Sentinel</span>
                  <h4 style={{ margin: '0.2rem 0 0.1rem', fontSize: '1.02rem', color: '#f8fafc' }}>Anomaly Radar Active</h4>
                  <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)' }}>Explainable risk & 1-tap card lock</p>
                </div>
                <span style={{ fontSize: '1.4rem' }}>🛡️</span>
              </div>

              <div
                className="feature-banner-card"
                onClick={() => navigate('/credit-networth')}
                style={{
                  background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(6, 182, 212, 0.08))',
                  border: '1px solid rgba(16, 185, 129, 0.3)',
                  borderRadius: '12px',
                  padding: '0.85rem 1rem',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#34d399', fontWeight: 700, letterSpacing: '0.04em' }}>CIBIL & Net Worth</span>
                  <h4 style={{ margin: '0.2rem 0 0.1rem', fontSize: '1.02rem', color: '#f8fafc' }}>Score 784 • Snowball Hub</h4>
                  <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)' }}>Debt payoff & 80C Tax Tracker</p>
                </div>
                <span style={{ fontSize: '1.4rem' }}>💰</span>
              </div>

              <div
                className="feature-banner-card"
                onClick={() => navigate('/bank-ingestion')}
                style={{
                  background: 'linear-gradient(135deg, rgba(168, 85, 247, 0.12), rgba(59, 130, 246, 0.08))',
                  border: '1px solid rgba(168, 85, 247, 0.3)',
                  borderRadius: '12px',
                  padding: '0.85rem 1rem',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#c084fc', fontWeight: 700, letterSpacing: '0.04em' }}>RBI Account Aggregator</span>
                  <h4 style={{ margin: '0.2rem 0 0.1rem', fontSize: '1.02rem', color: '#f8fafc' }}>Setu / Finvu Consent</h4>
                  <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)' }}>+ SMS/UPI Statement Parsing</p>
                </div>
                <span style={{ fontSize: '1.4rem' }}>🏛️</span>
              </div>
            </div>

            <section className="stats-grid" aria-label="Financial summary cards">
              {stats.map((stat) => (
                <article key={stat.label} className="stat-card">
                  <p>{stat.label}</p>
                  <strong>{stat.value}</strong>
                  <span className={`trend ${stat.tone}`}>{Number((financialSummary?.totalIncome ?? dashboardData?.financialSummary?.totalIncome ?? 0)) === 0 && Number((financialSummary?.totalExpenses ?? dashboardData?.financialSummary?.totalExpenses ?? 0)) === 0 ? 'No data yet' : 'Live data'}</span>
                </article>
              ))}
            </section>

            <section className="panel goals-panel" aria-labelledby="goals-title">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Planning</p>
                  <h3 id="goals-title">Financial Goals</h3>
                </div>
                <button type="button" className="link-btn" onClick={() => navigate('/goals')}>Manage goals</button>
              </div>
              <div className="goal-summary-grid">
                <div><span>Active goals</span><strong>{goalSummary.activeCount}</strong></div>
                <div><span>Total target</span><strong>{formatMoney(goalSummary.target, selectedCurrency)}</strong></div>
                <div><span>Total saved</span><strong>{formatMoney(goalSummary.saved, selectedCurrency)}</strong></div>
                <div><span>Total remaining</span><strong>{formatMoney(goalSummary.remaining, selectedCurrency)}</strong></div>
                <div><span>Monthly requirement</span><strong>{formatMoney(goalSummary.monthly, selectedCurrency)}</strong></div>
              </div>
              {goals.length ? (
                <div className="dashboard-goal-list">
                  {goals.filter((goal) => goal.status !== 'COMPLETED').slice(0, 4).map((goal) => (
                    <div className="dashboard-goal-item" key={goal.id}>
                      <div className="goal-item-head"><strong>{goal.name}</strong><span className={`tag ${goal.status === 'NEEDS_ATTENTION' || goal.status === 'OVERDUE' ? 'danger' : 'success'}`}>{goal.status.replaceAll('_', ' ')}</span></div>
                      <div className="goal-item-meta"><span>{formatMoney(goal.currentAmount, selectedCurrency)} saved of {formatMoney(goal.targetAmount, selectedCurrency)}</span><span>{formatMoney(goal.requiredMonthlySaving, selectedCurrency)}/month</span></div>
                      <div className="progress-track"><div className="progress-fill" style={{ width: `${Math.min(100, Number(goal.progressPercentage ?? 0))}%` }} /></div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-card">No financial goals created yet.</div>
              )}
            </section>

            <section className="two-column-layout ai-recommendations-split-layout">
              {/* Column 1: Live Market & General AI Recommendations */}
              <article className="panel recommendations-panel" aria-labelledby="recommendations-title">
                <div className="panel-header">
                  <div>
                    <p className="eyebrow">Rules engine & Market Intelligence</p>
                    <h3 id="recommendations-title">Live Market & AI Advisory</h3>
                  </div>
                  {recommendations && <span className="engine-badge">Explainable recommendations</span>}
                </div>

                <LiveMarketWidget />
                {recommendationError ? (
                  <div className="inline-error" role="alert">{recommendationError}</div>
                ) : recommendations?.recommendations?.length ? (
                  <div className="recommendation-list">
                    {recommendations.recommendations.map((recommendation, index) => (
                      <article key={`${recommendation.type}-${index}`} className={`recommendation-item priority-${recommendation.priority?.toLowerCase()}`}>
                        <div className="recommendation-copy">
                          <span className="recommendation-type">{recommendation.type?.replaceAll('_', ' ')}</span>
                          <h4>{recommendation.title}</h4>
                          <p>{recommendation.message}</p>
                          <small>{recommendation.reason}</small>
                        </div>
                        {recommendation.relatedAmount != null && <strong>{formatMoney(recommendation.relatedAmount, selectedCurrency)}</strong>}
                      </article>
                    ))}
                  </div>
                ) : recommendations ? (
                  <div className="empty-card">No recommendations are available from the current financial data.</div>
                ) : (
                  <div className="empty-card">Recommendations are unavailable right now.</div>
                )}
              </article>

              {/* Column 2: Dedicated AI Income-Based Advisory Column */}
              <article className="panel income-advisory-panel" aria-labelledby="income-advisory-title">
                <IncomeAdvisoryColumn
                  detectedIncome={recommendations?.monthlyIncome ?? financialSummary?.totalIncome ?? dashboardData?.financialSummary?.totalIncome ?? 50000}
                  currencyCode={selectedCurrency}
                  theme={theme}
                />
              </article>
            </section>

            <section className="panel financial-plan-panel" aria-labelledby="financial-plan-title">
              <div className="panel-header">
                <div><p className="eyebrow">Savings & investment intelligence</p><h3 id="financial-plan-title">Financial Plan</h3></div>
                <button type="button" className="link-btn" onClick={() => navigate('/financial-plan')}>Open plan</button>
              </div>
              {financialPlan ? (
                <>
                  <div className="plan-summary-grid">
                    <div><span>Monthly income</span><strong>{formatMoney(financialPlan.monthlyIncome, selectedCurrency)}</strong></div>
                    <div><span>Monthly expenses</span><strong>{formatMoney(financialPlan.monthlyExpenses, selectedCurrency)}</strong></div>
                    <div><span>Monthly left</span><strong>{formatMoney(financialPlan.monthlyLeft, selectedCurrency)}</strong></div>
                    <div><span>Goal savings</span><strong>{formatMoney(financialPlan.goalSavingsRequired, selectedCurrency)}</strong></div>
                    <div><span>Emergency/buffer</span><strong>{formatMoney(financialPlan.recommendedEmergencyAllocation, selectedCurrency)}</strong></div>
                    <div><span>Recommended investment</span><strong>{formatMoney(financialPlan.recommendedInvestmentAmount, selectedCurrency)}</strong></div>
                    <div><span>Flexible spending</span><strong>{formatMoney(financialPlan.flexibleSpendingAmount, selectedCurrency)}</strong></div>
                  </div>
                  <p className="muted-copy">{financialPlan.riskPreference ? `${financialPlan.riskPreference} profile, ${financialPlan.timeHorizonYears}-year horizon.` : 'Investment profile not configured.'}</p>
                  <div className="dashboard-plan-allocation">
                    {financialPlan.allocation?.map((item) => <div key={item.category}><span>{item.category}</span><strong>{item.percentage}%</strong></div>)}
                  </div>
                </>
              ) : <div className="empty-card">Financial plan is unavailable right now.</div>}
            </section>

            {creditCardIntel && creditCardIntel.cards?.length > 0 && <section className="panel cc-dashboard-panel" aria-labelledby="cc-health-title">
              <div className="panel-header">
                <div><p className="eyebrow">Credit card intelligence</p><h3 id="cc-health-title">Credit Card Health</h3></div>
                <button type="button" className="link-btn" onClick={() => navigate('/credit-cards')}>Manage cards</button>
              </div>
              <div className="cc-dashboard-summary">
                <div><span>Total Limit</span><strong>{formatMoney(creditCardIntel.summary?.totalCreditLimit, selectedCurrency)}</strong></div>
                <div><span>Total Outstanding</span><strong>{formatMoney(creditCardIntel.summary?.totalOutstanding, selectedCurrency)}</strong></div>
                <div><span>Available Credit</span><strong>{formatMoney(creditCardIntel.summary?.totalAvailableCredit, selectedCurrency)}</strong></div>
                <div><span>Overall Utilization</span><strong style={{ color: Number(creditCardIntel.summary?.overallUtilization) > 75 ? '#dc2626' : Number(creditCardIntel.summary?.overallUtilization) > 50 ? '#b45309' : '#15803d' }}>{Number(creditCardIntel.summary?.overallUtilization ?? 0).toFixed(1)}%</strong></div>
                <div><span>Repayment Capacity</span><strong className={creditCardIntel.repaymentCapacity?.status === 'COMFORTABLE' ? '' : 'overrun'}>{creditCardIntel.repaymentCapacity?.status?.replace('_', ' ') || '—'}</strong></div>
                <div><span>Cards Needing Attention</span><strong style={{ color: (creditCardIntel.summary?.cardsNeedingAttention ?? 0) > 0 ? '#dc2626' : '#15803d' }}>{creditCardIntel.summary?.cardsNeedingAttention ?? 0}</strong></div>
              </div>
              <div className="cc-dashboard-cards">
                {creditCardIntel.cards.slice(0, 4).map((card) => <div key={card.id} className="cc-dashboard-card-item">
                  <div className="cc-dashboard-card-head"><strong>{card.cardName}</strong><span className={`tag ${card.healthStatus === 'HEALTHY' ? 'success' : card.healthStatus === 'WATCH' ? 'warning-tag' : 'danger'}`}>{card.healthStatus}</span></div>
                  <div className="cc-dashboard-card-meta"><span>•••• {card.lastFourDigits}</span><span>{formatMoney(card.currentOutstanding, selectedCurrency)} / {formatMoney(card.creditLimit, selectedCurrency)}</span><span style={{ color: Number(card.utilizationPercentage) > 75 ? '#dc2626' : Number(card.utilizationPercentage) > 50 ? '#b45309' : '#15803d' }}>{Number(card.utilizationPercentage).toFixed(1)}%</span><span>Due: {card.dueDate || '—'}</span></div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: `${Math.min(100, Number(card.utilizationPercentage))}%`, background: Number(card.utilizationPercentage) > 75 ? 'linear-gradient(90deg, #f59e0b, #ef4444)' : undefined }} /></div>
                </div>)}
              </div>
            </section>}

            {/* Linked Bank Accounts Panel */}
            <section className="panel bank-accounts-dashboard-panel" aria-labelledby="bank-accounts-title">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Liquid Assets & Indian Banking</p>
                  <h3 id="bank-accounts-title">Linked Bank Accounts</h3>
                </div>
                <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center' }}>
                  <button type="button" className="secondary-btn small" onClick={() => navigate('/linked-accounts')}>Manage Accounts</button>
                  <button type="button" className="primary-btn small" onClick={() => setShowQuickLinkModal(true)}>+ Link Bank</button>
                </div>
              </div>

              {bankSuccessMsg && (
                <div style={{ background: '#ecfdf5', color: '#065f46', padding: '0.6rem 0.9rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid #a7f3d0', fontSize: '0.85rem' }}>
                  {bankSuccessMsg}
                </div>
              )}

              <div className="bank-dashboard-summary" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1.25rem' }}>
                <div style={{ background: 'var(--bg-subtle, #f8fafc)', border: '1px solid var(--border-color, #e2e8f0)', padding: '0.85rem 1.1rem', borderRadius: '10px' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)', textTransform: 'uppercase', fontWeight: 600 }}>Total Liquid Bank Balance</span>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0284c7', marginTop: '0.2rem' }}>
                    {formatMoney(linkedAccounts.filter(a => a.status === 'CONNECTED').reduce((s, a) => s + Number(a.balance || 0), 0), selectedCurrency)}
                  </div>
                </div>
                <div style={{ background: 'var(--bg-subtle, #f8fafc)', border: '1px solid var(--border-color, #e2e8f0)', padding: '0.85rem 1.1rem', borderRadius: '10px' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)', textTransform: 'uppercase', fontWeight: 600 }}>Connected Accounts</span>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#1e293b', marginTop: '0.2rem' }}>
                    {linkedAccounts.filter(a => a.status === 'CONNECTED').length} Active
                  </div>
                </div>
                <div style={{ background: 'var(--bg-subtle, #f8fafc)', border: '1px solid var(--border-color, #e2e8f0)', padding: '0.85rem 1.1rem', borderRadius: '10px' }}>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)', textTransform: 'uppercase', fontWeight: 600 }}>Primary Provider</span>
                  <div style={{ fontSize: '1.1rem', fontWeight: 600, color: '#334155', marginTop: '0.35rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {linkedAccounts[0]?.providerName || 'None linked yet'}
                  </div>
                </div>
              </div>

              {linkedAccounts.length === 0 ? (
                <div className="empty-card" style={{ padding: '2rem 1rem', textAlign: 'center' }}>
                  <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>🏛️</div>
                  <h4 style={{ margin: '0 0 0.5rem 0' }}>No bank accounts connected yet</h4>
                  <p className="muted-copy" style={{ maxWidth: '420px', margin: '0 auto 1rem' }}>
                    Link your Indian bank accounts (HDFC, SBI, ICICI, Axis, etc.) to track your real liquid cash and power your financial planning.
                  </p>
                  <button type="button" className="primary-btn" onClick={() => setShowQuickLinkModal(true)}>+ Link Your First Bank Account</button>
                </div>
              ) : (
                <div className="bank-dashboard-cards" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' }}>
                  {linkedAccounts.map((account) => {
                    const isConnected = account.status === 'CONNECTED'
                    return (
                      <div
                        key={account.id}
                        className="bank-dashboard-card-item"
                        style={{
                          border: '1px solid var(--border-color, #e2e8f0)',
                          borderRadius: '12px',
                          padding: '1rem 1.1rem',
                          background: isConnected ? '#ffffff' : '#f8fafc',
                          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                          display: 'flex',
                          flexDirection: 'column',
                          justifyContent: 'space-between',
                          gap: '0.75rem',
                        }}
                      >
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                              <span style={{ fontSize: '1.3rem' }}>🏛️</span>
                              <div>
                                <strong style={{ display: 'block', fontSize: '0.95rem' }}>{account.providerName}</strong>
                                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)' }}>{account.accountName}</span>
                              </div>
                            </div>
                            <span className={`tag ${isConnected ? 'success' : 'danger'}`} style={{ fontSize: '0.7rem' }}>
                              {isConnected ? 'Active' : 'Offline'}
                            </span>
                          </div>

                          <div style={{ margin: '0.5rem 0' }}>
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted, #64748b)', textTransform: 'uppercase' }}>Available Balance</div>
                            <div style={{ fontSize: '1.35rem', fontWeight: 700, color: '#0f172a' }}>
                              {formatMoney(account.balance ?? 0, selectedCurrency)}
                            </div>
                          </div>

                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', fontSize: '0.75rem' }}>
                            <span style={{ background: '#f1f5f9', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 500 }}>
                              {account.maskedIdentifier}
                            </span>
                            {account.accountSubtype && (
                              <span style={{ background: '#e0f2fe', color: '#0369a1', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 500 }}>
                                {account.accountSubtype}
                              </span>
                            )}
                            {account.ifscCode && (
                              <span style={{ background: '#fef3c7', color: '#92400e', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 500 }}>
                                {account.ifscCode}
                              </span>
                            )}
                          </div>
                        </div>

                        <div style={{ borderTop: '1px solid #f1f5f9', paddingTop: '0.5rem', display: 'flex', justifyContent: 'flex-end', gap: '0.4rem' }}>
                          <button
                            type="button"
                            className="secondary-btn small"
                            style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}
                            onClick={() => {
                              setEditingBalanceAccount(account)
                              setNewBalanceInput(String(account.balance ?? 0))
                            }}
                          >
                            Update Balance
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </section>

            <section className="dashboard-grid">
              <article className="panel chart-panel">
                <div className="panel-header">
                  <h3>Monthly income vs expense</h3>
                </div>
                {monthlyTrend.length ? (
                  <div className="chart-wrap">
                    <ResponsiveContainer width="100%" height={260}>
                      <BarChart data={monthlyTrend}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="month" />
                        <YAxis tickFormatter={(value) => formatMoney(value, selectedCurrency)} />
                        <Tooltip formatter={(value) => formatMoney(value, selectedCurrency)} />
                        <Bar dataKey="income" fill="#2563eb" radius={[4, 4, 0, 0]} />
                        <Bar dataKey="expenses" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <div className="empty-card">No monthly data available.</div>
                )}
              </article>

              <article className="panel chart-panel">
                <div className="panel-header">
                  <h3>Expense breakdown</h3>
                </div>
                {categoryBreakdown.length ? (
                  <div className="chart-wrap">
                    <ResponsiveContainer width="100%" height={260}>
                      <PieChart>
                        <Pie data={categoryBreakdown} dataKey="value" nameKey="name" innerRadius={52} outerRadius={82} paddingAngle={2}>
                          {categoryBreakdown.map((entry) => (
                            <Cell key={entry.name} fill={entry.fill} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(value) => formatMoney(value, selectedCurrency)} />
                      </PieChart>
                    </ResponsiveContainer>
                    <div className="legend-list">
                      {categoryBreakdown.map((entry) => (
                        <div key={entry.name} className="legend-item">
                          <span className="legend-swatch" style={{ background: entry.fill }} />
                          <span>{entry.name}</span>
                          <strong>{formatMoney(entry.value, selectedCurrency)}</strong>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="empty-card">No spending categories yet.</div>
                )}
              </article>
            </section>

            <section className="two-column-layout">
              <article className="panel">
                <div className="panel-header">
                  <h3>Budget monitoring</h3>
                  <button type="button" className="link-btn" onClick={() => navigate('/budgets')}>View all</button>
                </div>

                {budgetCards.length ? (
                  <div className="budget-list">
                    {budgetCards.map((budget) => (
                      <div key={budget.id} className={`budget-item ${budget.remaining < 0 ? 'over' : ''}`}>
                        <div className="budget-head">
                          <div>
                            <strong>{budget.categoryName || 'General budget'}</strong>
                            <span>{budget.startDate ? formatDate(budget.startDate) : '—'} to {budget.endDate ? formatDate(budget.endDate) : '—'}</span>
                          </div>
                          <strong className={budget.remaining < 0 ? 'overrun' : ''}>{formatMoney(budget.remaining, selectedCurrency)} left</strong>
                        </div>
                        <div className="budget-row">
                          <span>{formatMoney(budget.spent, selectedCurrency)} spent</span>
                          <span>{formatMoney(budget.amount, selectedCurrency)} total</span>
                        </div>
                        <div className="progress-track" aria-label={`Progress for ${budget.categoryName || 'General budget'}`}>
                          <div className={`progress-fill ${budget.remaining < 0 ? 'over' : ''}`} style={{ width: `${Math.min(100, budget.percent)}%` }} />
                        </div>
                        <div className="budget-row muted">
                          <span>{budget.remaining < 0 ? 'Exceeded by ' : 'Remaining '}{formatMoney(Math.abs(budget.remaining), selectedCurrency)}</span>
                          <span>{Math.round(budget.percent)}%</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="empty-card">No budgets created yet.</div>
                )}
              </article>

              <article className="panel">
                <div className="panel-header">
                  <h3>Forecast & advisory</h3>
                </div>

                {forecast || advisory ? (
                  <div className="forecast-stack">
                    {forecast && (
                      <div className="mini-section">
                        <h4>Forecast</h4>
                        <div className="metric-row">
                          <span>Average monthly expense</span>
                          <strong>{formatMoney(forecast.averageMonthlyExpense ?? 0, selectedCurrency)}</strong>
                        </div>
                        <div className="metric-row">
                          <span>Forecasted monthly expense</span>
                          <strong>{formatMoney(forecast.forecastMonthlyExpense ?? 0, selectedCurrency)}</strong>
                        </div>
                        {(forecast.monthlyExpenses || []).length ? (
                          <div className="line-chart-wrap">
                            <ResponsiveContainer width="100%" height={180}>
                              <LineChart data={(forecast.monthlyExpenses || []).slice(-6)}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey={(point) => `${point.year}-${String(point.month).padStart(2, '0')}`} tick={false} />
                                <YAxis tickFormatter={(value) => formatMoney(value, selectedCurrency)} />
                                <Tooltip formatter={(value) => formatMoney(value, selectedCurrency)} />
                                <Line type="monotone" dataKey="expenseTotal" stroke="#22c55e" strokeWidth={3} dot={{ r: 3 }} />
                              </LineChart>
                            </ResponsiveContainer>
                          </div>
                        ) : null}
                        <p className="muted-copy">{forecast.forecastBasis || 'No forecast basis was returned by the API.'}</p>
                      </div>
                    )}

                    {advisory && (
                      <div className="mini-section">
                        <h4>Advisory</h4>
                        <div className="metric-row">
                          <span>Monthly income</span>
                          <strong>{formatMoney(advisory.monthlyIncome ?? 0, selectedCurrency)}</strong>
                        </div>
                        <div className="metric-row">
                          <span>Monthly expenses</span>
                          <strong>{formatMoney(advisory.monthlyExpenses ?? 0, selectedCurrency)}</strong>
                        </div>
                        <div className="metric-row">
                          <span>Disposable income</span>
                          <strong>{formatMoney(advisory.disposableIncome ?? 0, selectedCurrency)}</strong>
                        </div>
                        <div className="metric-row">
                          <span>Recommended savings</span>
                          <strong>{formatMoney(advisory.recommendedSavings ?? 0, selectedCurrency)}</strong>
                        </div>
                        <p className="muted-copy">{advisory.guidance || 'No advisory guidance was returned by the API.'}</p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="empty-card">No forecast or advisory data is available right now.</div>
                )}
              </article>
            </section>

            <section className="two-column-layout">
              <article className="panel">
                <div className="panel-header">
                  <h3>Recent transactions</h3>
                  <button type="button" className="link-btn" onClick={() => navigate('/transactions')}>All transactions</button>
                </div>
                {recentTransactions.length ? (
                  <ul className="activity-list">
                    {recentTransactions.map((item) => (
                      <li key={item.id ?? `${item.transactionDate}-${item.description}`}>
                        <div>
                          <strong>{item.description || item.merchant || 'Transaction'}</strong>
                          <span>{item.merchant ? `${item.merchant} • ` : ''}{item.categoryName || 'Uncategorized'} • {formatDate(item.transactionDate)}</span>
                        </div>
                        <em className={item.type === 'INCOME' ? 'income' : 'expense'}>{item.type === 'INCOME' ? '+' : '-'}{formatMoney(item.amount, selectedCurrency)}</em>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="empty-card">No transactions yet.</div>
                )}
              </article>

              <article className="panel">
                <div className="panel-header">
                  <h3>Fraud & alerts</h3>
                  <button type="button" className="link-btn" onClick={() => navigate('/alerts')}>All alerts</button>
                </div>
                {recentAlerts.length ? (
                  <ul className="alert-list">
                    {recentAlerts.map((alert) => (
                      <li key={alert.id ?? `${alert.title}-${alert.createdAt}`}>
                        <div className={`severity-dot ${alert.type?.toLowerCase() === 'fraud' ? 'high' : alert.read ? 'low' : 'medium'}`} aria-hidden="true" />
                        <div className="alert-copy">
                          <strong>{alert.title || 'Alert'}</strong>
                          <span>{alert.message || 'No message provided.'}</span>
                          <small>{formatDateTime(alert.createdAt)} • {alert.read ? 'Read' : 'Unread'}</small>
                        </div>
                        {!alert.read && (
                          <button type="button" className="mini-btn" onClick={() => handleMarkRead(alert.id)}>Mark read</button>
                        )}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <div className="empty-card">No fraud alerts.</div>
                )}
                <div className="alert-summary">
                  <span>{unreadCount} unread</span>
                  <button type="button" className="secondary-btn small" onClick={() => navigate('/alerts')}>Open notifications</button>
                </div>
              </article>
            </section>

            {/* Quick Link Bank Account Modal */}
            {showQuickLinkModal && (
              <div className="modal-backdrop" role="presentation">
                <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="quick-link-bank-title" style={{ maxWidth: '500px' }}>
                  <div className="panel-header">
                    <div>
                      <p className="eyebrow">Banking Connection</p>
                      <h2 id="quick-link-bank-title">Link Indian Bank Account</h2>
                    </div>
                    <button type="button" className="link-btn" onClick={() => setShowQuickLinkModal(false)}>Close</button>
                  </div>

                  {/* Preset chips */}
                  <div style={{ marginBottom: '1rem' }}>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-muted, #64748b)', marginBottom: '0.4rem', fontWeight: 500 }}>
                      Quick Select Bank:
                    </p>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                      {DASHBOARD_INDIAN_BANKS.map((preset) => (
                        <button
                          type="button"
                          key={preset.code}
                          onClick={() => setQuickBankForm((prev) => ({
                            ...prev,
                            providerName: preset.name,
                            accountName: prev.accountName || `${preset.name} Primary`,
                            ifscCode: prev.ifscCode || `${preset.code}0000001`,
                          }))}
                          style={{
                            border: quickBankForm.providerName === preset.name ? '1px solid #0284c7' : '1px solid #cbd5e1',
                            background: quickBankForm.providerName === preset.name ? '#e0f2fe' : '#ffffff',
                            color: quickBankForm.providerName === preset.name ? '#0369a1' : '#334155',
                            padding: '0.3rem 0.6rem',
                            borderRadius: '6px',
                            fontSize: '0.78rem',
                            cursor: 'pointer',
                          }}
                        >
                          {preset.name}
                        </button>
                      ))}
                    </div>
                  </div>

                  <form className="goal-form" onSubmit={handleQuickLinkBank}>
                    <label>
                      Bank Provider Name *
                      <input
                        required
                        value={quickBankForm.providerName}
                        onChange={(e) => setQuickBankForm((prev) => ({ ...prev, providerName: e.target.value }))}
                        placeholder="e.g. HDFC Bank, SBI, ICICI"
                      />
                    </label>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                      <label>
                        Account Type
                        <select
                          value={quickBankForm.accountSubtype}
                          onChange={(e) => setQuickBankForm((prev) => ({ ...prev, accountSubtype: e.target.value }))}
                        >
                          <option value="Savings Account">Savings Account</option>
                          <option value="Salary Account">Salary Account</option>
                          <option value="Current Account">Current Account</option>
                          <option value="Fixed Deposit">Fixed Deposit</option>
                          <option value="NRE / NRO Account">NRE / NRO Account</option>
                        </select>
                      </label>
                      <label>
                        Account Nickname *
                        <input
                          required
                          value={quickBankForm.accountName}
                          onChange={(e) => setQuickBankForm((prev) => ({ ...prev, accountName: e.target.value }))}
                          placeholder="e.g. Salary Account"
                        />
                      </label>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                      <label>
                        Last 4 Digits / Masked *
                        <input
                          required
                          value={quickBankForm.maskedIdentifier}
                          onChange={(e) => setQuickBankForm((prev) => ({ ...prev, maskedIdentifier: e.target.value }))}
                          placeholder="•••• 4892 or 4892"
                        />
                      </label>
                      <label>
                        IFSC Code (optional)
                        <input
                          value={quickBankForm.ifscCode}
                          onChange={(e) => setQuickBankForm((prev) => ({ ...prev, ifscCode: e.target.value }))}
                          placeholder="e.g. HDFC0001234"
                        />
                      </label>
                    </div>

                    <label>
                      Opening / Current Balance ({selectedCurrency})
                      <input
                        type="number"
                        step="0.01"
                        value={quickBankForm.balance}
                        onChange={(e) => setQuickBankForm((prev) => ({ ...prev, balance: e.target.value }))}
                        placeholder="e.g. 50000"
                      />
                    </label>

                    <div className="modal-actions" style={{ marginTop: '1rem' }}>
                      <button type="button" className="secondary-btn" onClick={() => setShowQuickLinkModal(false)}>Cancel</button>
                      <button type="submit" className="primary-btn" disabled={savingQuickBank}>
                        {savingQuickBank ? 'Linking…' : 'Link Account'}
                      </button>
                    </div>
                  </form>
                </section>
              </div>
            )}

            {/* Quick Update Balance Modal */}
            {editingBalanceAccount && (
              <div className="modal-backdrop" role="presentation">
                <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="quick-update-balance-title" style={{ maxWidth: '420px' }}>
                  <div className="panel-header">
                    <div>
                      <p className="eyebrow">{editingBalanceAccount.providerName}</p>
                      <h2 id="quick-update-balance-title">Update Balance</h2>
                    </div>
                    <button type="button" className="link-btn" onClick={() => setEditingBalanceAccount(null)}>Close</button>
                  </div>
                  <form className="goal-form" onSubmit={handleUpdateBankBalance}>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted, #64748b)' }}>
                      Enter current balance for <strong>{editingBalanceAccount.accountName}</strong> ({editingBalanceAccount.maskedIdentifier}):
                    </p>
                    <label>
                      New Balance ({selectedCurrency})
                      <input
                        type="number"
                        step="0.01"
                        required
                        autoFocus
                        value={newBalanceInput}
                        onChange={(e) => setNewBalanceInput(e.target.value)}
                        placeholder="e.g. 75000.00"
                      />
                    </label>
                    <div className="modal-actions">
                      <button type="button" className="secondary-btn" onClick={() => setEditingBalanceAccount(null)}>Cancel</button>
                      <button type="submit" className="primary-btn" disabled={updatingBalance}>
                        {updatingBalance ? 'Saving…' : 'Save Balance'}
                      </button>
                    </div>
                  </form>
                </section>
              </div>
            )}

            {/* Onboarding Wizard Modal (< 2 min setup) */}
            <OnboardingWizardModal
              isOpen={showOnboardingModal}
              onClose={() => setShowOnboardingModal(false)}
              onComplete={() => {
                setShowOnboardingModal(false)
                refreshDashboard()
              }}
            />

            {/* Security & Compliance Modal (2FA passkeys, audit logs, DPDP consent) */}
            <SecurityAndComplianceModal
              isOpen={showSecurityModal}
              onClose={() => setShowSecurityModal(false)}
            />

            <PageNavFooter />
          </>
        )}
      </main>
    </div>
  )
}

export default App
