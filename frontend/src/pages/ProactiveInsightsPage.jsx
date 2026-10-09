import React, { useState, useEffect } from 'react';
import { financialHubApi } from '../lib/api';
import { PageNavHeader, PageNavFooter } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

export default function ProactiveInsightsPage() {
  const currency = useSelectedCurrency();
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paidBills, setPaidBills] = useState({});

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const data = await financialHubApi.getProactiveInsights();
        setInsights(data);
      } catch (err) {
        console.error('Failed to load insights', err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  function togglePaid(billId) {
    setPaidBills((prev) => ({
      ...prev,
      [billId]: !prev[billId],
    }));
  }

  return (
    <div className="page-shell">
      <PageNavHeader title="Proactive Insights & Safe Spend" />

      <div className="page-header">
        <div>
          <p className="eyebrow">Real-Time Spending Guard & Predictive Cash Flow</p>
          <h1>Proactive Financial Insights</h1>
        </div>
      </div>

      {loading ? (
        <div className="loader-row">Calculating dynamic cash flow and subscriptions…</div>
      ) : insights && (
        <>
          {/* Hero Banner: Safe to Spend Today */}
          <div
            style={{
              background: 'linear-gradient(135deg, #064e3b 0%, #065f46 50%, #047857 100%)',
              color: '#ffffff',
              padding: '1.75rem 2rem',
              borderRadius: '16px',
              marginBottom: '1.75rem',
              boxShadow: '0 10px 30px rgba(6, 78, 59, 0.25)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '1.25rem',
            }}
          >
            <div>
              <span style={{ fontSize: '0.78rem', letterSpacing: '0.08em', textTransform: 'uppercase', color: '#a7f3d0', fontWeight: 700 }}>
                CALCULATED FROM YOUR LIVE SURPLUS & REMAINING DAYS
              </span>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.75rem', marginTop: '0.4rem' }}>
                <span style={{ fontSize: '3rem', fontWeight: 900, letterSpacing: '-0.02em' }}>
                  {formatMoneyAmount(insights.safeToSpendToday, currency)}
                </span>
                <span style={{ fontSize: '1.1rem', color: '#d1fae5', fontWeight: 500 }}>/ safe today</span>
              </div>
              <p style={{ margin: '0.4rem 0 0', fontSize: '0.88rem', color: '#a7f3d0' }}>
                Status: <strong>{insights.safeSpendStatus}</strong> • Remaining Days in Month: <strong>{insights.daysRemainingInMonth}</strong> • Projected Month-End Surplus: <strong>{formatMoneyAmount(insights.projectedMonthEndSurplus, currency)}</strong>
              </p>
            </div>

            <div style={{ background: 'rgba(255, 255, 255, 0.12)', backdropFilter: 'blur(8px)', padding: '1rem 1.25rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.2)' }}>
              <span style={{ fontSize: '0.75rem', color: '#d1fae5', display: 'block', textTransform: 'uppercase', fontWeight: 700 }}>MONTHLY CASH FLOW FORMULA</span>
              <div style={{ fontSize: '0.82rem', color: '#ffffff', marginTop: '0.3rem', lineHeight: 1.4 }}>
                Income (₹85k) - Fixed (₹44.2k) - Spent<br />
                Divided by {insights.daysRemainingInMonth} days remaining
              </div>
            </div>
          </div>

          {/* Weekly Digest Signals */}
          <div className="page-card" style={{ marginBottom: '1.75rem' }}>
            <div className="panel-header">
              <div>
                <p className="eyebrow">Weekly Digest Radar</p>
                <h2>Active Proactive Recommendations</h2>
              </div>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              {insights.weeklyDigestSignals?.map((signal, idx) => (
                <div
                  key={idx}
                  style={{
                    background: '#f8fafc',
                    border: '1px solid #e2e8f0',
                    borderLeft: '4px solid #0284c7',
                    padding: '0.75rem 1rem',
                    borderRadius: '8px',
                    fontSize: '0.88rem',
                    color: '#1e293b',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.65rem',
                  }}
                >
                  <span style={{ fontSize: '1.1rem' }}>💡</span>
                  <span>{signal}</span>
                </div>
              ))}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
            {/* Subscription & Recurring Detector */}
            <div className="page-card">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Recurring Payment Detector</p>
                  <h3>Active Subscriptions ({formatMoneyAmount(insights.totalMonthlySubscriptions, currency)}/mo)</h3>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {insights.detectedSubscriptions?.map((sub) => (
                  <div
                    key={sub.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '0.65rem 0.85rem',
                      background: '#f8fafc',
                      borderRadius: '8px',
                      border: '1px solid #e2e8f0',
                    }}
                  >
                    <div>
                      <strong style={{ display: 'block', fontSize: '0.9rem', color: '#0f172a' }}>{sub.name}</strong>
                      <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                        Due: {sub.nextDueDate} • {sub.category}
                      </span>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <strong style={{ fontSize: '0.95rem', color: '#0284c7' }}>
                        {formatMoneyAmount(sub.monthlyAmount, currency)}
                      </strong>
                      <span style={{ display: 'block', fontSize: '0.7rem', color: '#64748b' }}>{sub.billingCycle}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Bill Reminders & Due Date Tracker */}
            <div className="page-card">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Due Date Tracker</p>
                  <h3>Upcoming Household & Card Bills</h3>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {insights.upcomingBills?.map((bill) => {
                  const isPaid = paidBills[bill.id];
                  return (
                    <div
                      key={bill.id}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '0.65rem 0.85rem',
                        background: isPaid ? '#f0fdf4' : '#ffffff',
                        borderRadius: '8px',
                        border: isPaid ? '1px solid #bbf7d0' : '1px solid #e2e8f0',
                      }}
                    >
                      <div>
                        <strong style={{ display: 'block', fontSize: '0.9rem', color: isPaid ? '#15803d' : '#0f172a', textDecoration: isPaid ? 'line-through' : 'none' }}>
                          {bill.billerName}
                        </strong>
                        <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                          Due Date: {bill.dueDate} • {bill.category}
                        </span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <strong style={{ fontSize: '1rem', color: isPaid ? '#16a34a' : '#b91c1c' }}>
                          {formatMoneyAmount(bill.amount, currency)}
                        </strong>
                        <button
                          type="button"
                          onClick={() => togglePaid(bill.id)}
                          style={{
                            background: isPaid ? '#16a34a' : '#f1f5f9',
                            color: isPaid ? '#ffffff' : '#334155',
                            border: '1px solid #cbd5e1',
                            padding: '0.3rem 0.6rem',
                            borderRadius: '6px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                          }}
                        >
                          {isPaid ? '✓ Paid' : 'Mark Paid'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </>
      )}

      <PageNavFooter />
    </div>
  );
}
