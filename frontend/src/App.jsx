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
  notificationApi,
  recommendationApi,
  summaryApi,
  transactionApi,
} from './lib/api'
import CurrencySelector from './components/CurrencySelector'
import {
  formatMoneyAmount,
  formatNumber,
  setStoredExchangeRates,
  useSelectedCurrency,
} from './utils/currency'
import './App.css'

const navItems = [
  { label: 'Dashboard', path: '/' },
  { label: 'Transactions', path: '/transactions' },
  { label: 'Budgets', path: '/budgets' },
  { label: 'Goals', path: '/goals' },
  { label: 'Financial plan', path: '/financial-plan' },
  { label: 'Credit Cards', path: '/credit-cards' },
  { label: 'Alerts', path: '/alerts' },
  { label: 'New transaction', path: '/transactions/new' },
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
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refreshDashboard = async () => {
    setLoading(true)
    setError('')

    try {
      const [dashboardResult, summaryResult, forecastResult, advisoryResult, recommendationResult, financialPlanResult, goalsResult, transactionsResult, notificationsResult, unreadCountResult, rateResult, creditCardResult] = await Promise.allSettled([
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

      if (!dashboard && !financial && !forecastData && !advisoryData && !transactionData.length && !notificationData.length) {
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

    return [
      { label: 'Total income', value: formatMoney(totalIncome, selectedCurrency), tone: 'positive' },
      { label: 'Total expenses', value: formatMoney(totalExpenses, selectedCurrency), tone: 'neutral' },
      { label: 'Monthly left', value: formatMoney(disposable, selectedCurrency), tone: disposable >= 0 ? 'positive' : 'warning' },
      { label: 'Monthly savings', value: formatMoney(savings, selectedCurrency), tone: 'positive' },
      { label: 'Investments', value: formatMoney(investments, selectedCurrency), tone: 'neutral' },
      { label: 'Goal progress', value: goals ? formatMoney(goals, selectedCurrency) : 'No goals recorded', tone: 'neutral' },
    ]
  }, [dashboardData, financialSummary, advisory, recommendations, selectedCurrency])

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

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
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
        <header className="topbar">
          <div>
            <p className="eyebrow">{user ? `Welcome, ${user.name}` : 'Welcome back'}</p>
            <h2>Dashboard</h2>
          </div>

          <div className="topbar-actions">
            <CurrencySelector />
            <button type="button" className="secondary-btn" onClick={refreshDashboard}>Refresh</button>
            <button type="button" className="primary-btn" onClick={() => navigate('/transactions/new')}>New transaction</button>
          </div>
        </header>

        {error && <div className="inline-error" role="alert">{error}</div>}

        {loading ? (
          <div className="loader-row">Loading dashboard…</div>
        ) : (
          <>
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

            <section className="panel recommendations-panel" aria-labelledby="recommendations-title">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Rules engine</p>
                  <h3 id="recommendations-title">AI Financial Recommendations</h3>
                </div>
                {recommendations && <span className="engine-badge">Explainable recommendations</span>}
              </div>
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
          </>
        )}
      </main>
    </div>
  )
}

export default App
