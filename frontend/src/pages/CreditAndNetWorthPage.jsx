import React, { useState, useEffect } from 'react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { financialHubApi } from '../lib/api';
import { PageNavHeader, PageNavFooter, TabStepNav } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

const PIE_COLORS = ['#0284c7', '#10b981', '#f59e0b', '#8b5cf6'];

const CREDIT_NETWORTH_TABS = [
  { id: 'cibil', label: 'CIBIL Credit Score' },
  { id: 'networth', label: 'Consolidated Net Worth' },
  { id: 'debt', label: 'Debt Payoff Snowball' },
  { id: 'tax', label: 'Section 80C Tax Optimizer' },
];

export default function CreditAndNetWorthPage() {
  const currency = useSelectedCurrency();
  const [activeTab, setActiveTab] = useState('cibil'); // cibil, networth, debt, tax
  const [cibilData, setCibilData] = useState(null);
  const [netWorthData, setNetWorthData] = useState(null);
  const [debtData, setDebtData] = useState(null);
  const [taxData, setTaxData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [debtStrategy, setDebtStrategy] = useState('AVALANCHE'); // AVALANCHE or SNOWBALL

  useEffect(() => {
    async function loadAll() {
      setLoading(true);
      try {
        const [cibil, netWorth, debt, tax] = await Promise.all([
          financialHubApi.getCibilScore(),
          financialHubApi.getNetWorth(),
          financialHubApi.getDebtPayoff(),
          financialHubApi.getTaxTracker(),
        ]);
        setCibilData(cibil);
        setNetWorthData(netWorth);
        setDebtData(debt);
        setTaxData(tax);
      } catch (err) {
        console.error('Failed to load credit & net worth data', err);
      } finally {
        setLoading(false);
      }
    }
    loadAll();
  }, []);

  const netWorthPieData = netWorthData
    ? [
        { name: 'Liquid Bank Accounts', value: Number(netWorthData.liquidBankBalances || 0) },
        { name: 'Mutual Funds & Equities', value: Number(netWorthData.mutualFundsAndEquities || 0) },
        { name: 'Gold (Digital / SGB)', value: Number(netWorthData.goldHoldings || 0) },
        { name: 'Real Estate / Land', value: Number(netWorthData.realEstateProperty || 0) },
      ]
    : [];

  const cibilChartData = cibilData?.historyTrend?.map((score, idx) => ({
    month: `M-${12 - idx}`,
    score,
  })) || [];

  return (
    <div className="page-shell">
      <PageNavHeader title="Credit & Net Worth Hub" />

      <div className="page-header">
        <div>
          <p className="eyebrow">CIBIL • Debt Payoff • Net Worth • Tax 80C</p>
          <h1>Credit & Net Worth Wealth Hub</h1>
        </div>
      </div>

      {/* Tabs */}
      <div className="cc-tabs" style={{ marginBottom: '1.5rem' }}>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'cibil' ? 'active' : ''}`}
          onClick={() => setActiveTab('cibil')}
        >
          🏆 CIBIL & Credit Score
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'networth' ? 'active' : ''}`}
          onClick={() => setActiveTab('networth')}
        >
          💰 Consolidated Net Worth
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'debt' ? 'active' : ''}`}
          onClick={() => setActiveTab('debt')}
        >
          ⚡ Debt Payoff (Snowball vs Avalanche)
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'tax' ? 'active' : ''}`}
          onClick={() => setActiveTab('tax')}
        >
          📑 Section 80C & Tax Tracker
        </button>
      </div>

      {loading ? (
        <div className="loader-row">Loading financial intelligence…</div>
      ) : (
        <>
          {/* TAB 1: CIBIL Credit Score */}
          {activeTab === 'cibil' && cibilData && (
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
                {/* Score Card */}
                <div className="page-card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '2rem 1.5rem' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
                    {cibilData.provider}
                  </span>
                  <div style={{ fontSize: '3.6rem', fontWeight: 900, color: '#16a34a', margin: '0.4rem 0' }}>
                    {cibilData.score}
                  </div>
                  <span style={{ background: '#dcfce7', color: '#15803d', fontWeight: 700, padding: '0.25rem 0.8rem', borderRadius: '9999px', fontSize: '0.82rem' }}>
                    {cibilData.scoreBand} SCORE TIER
                  </span>
                  <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '0.8rem' }}>
                    Top 8% of borrowers in India. Excellent pre-approved loan & credit card rates available.
                  </p>
                </div>

                {/* Score Pillars */}
                <div className="page-card">
                  <h3 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Credit Score Factors</h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.25rem' }}>
                        <span>On-Time Payment History</span>
                        <strong>{cibilData.paymentHistoryPercent}% (High Impact)</strong>
                      </div>
                      <div className="progress-track"><div className="progress-fill" style={{ width: `${cibilData.paymentHistoryPercent}%`, background: '#16a34a' }} /></div>
                    </div>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.25rem' }}>
                        <span>Credit Limit Utilization</span>
                        <strong>{cibilData.creditUtilizationPercent}% (Under 30% Ideal)</strong>
                      </div>
                      <div className="progress-track"><div className="progress-fill" style={{ width: `${cibilData.creditUtilizationPercent}%`, background: '#0284c7' }} /></div>
                    </div>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.25rem' }}>
                        <span>Average Credit Age</span>
                        <strong>{cibilData.averageCreditAgeYears} Years (Medium Impact)</strong>
                      </div>
                      <div className="progress-track"><div className="progress-fill" style={{ width: '80%', background: '#8b5cf6' }} /></div>
                    </div>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.25rem' }}>
                        <span>Hard Credit Inquiries</span>
                        <strong>{cibilData.hardInquiriesCount} (Low Impact)</strong>
                      </div>
                      <div className="progress-track"><div className="progress-fill" style={{ width: '20%', background: '#f59e0b' }} /></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* 12-Month Score Trend */}
              <div className="page-card">
                <div className="panel-header">
                  <h3>12-Month Score Progression</h3>
                  <span className="trend positive">↗ +42 pts in 12 mos</span>
                </div>
                <div style={{ width: '100%', height: 220 }}>
                  <ResponsiveContainer>
                    <LineChart data={cibilChartData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="month" />
                      <YAxis domain={[720, 800]} />
                      <Tooltip />
                      <Line type="monotone" dataKey="score" stroke="#16a34a" strokeWidth={3} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>
              <TabStepNav tabs={CREDIT_NETWORTH_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
            </div>
          )}

          {/* TAB 2: Consolidated Net Worth */}
          {activeTab === 'networth' && netWorthData && (
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ background: 'linear-gradient(135deg, #0284c7, #0369a1)', color: '#ffffff', padding: '1.25rem', borderRadius: '14px' }}>
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#bae6fd', fontWeight: 700 }}>CONSOLIDATED NET WORTH</span>
                  <div style={{ fontSize: '2rem', fontWeight: 800, marginTop: '0.3rem' }}>
                    {formatMoneyAmount(netWorthData.totalNetWorth, currency)}
                  </div>
                  <span style={{ fontSize: '0.78rem', color: '#e0f2fe' }}>Assets minus Total Liabilities</span>
                </div>

                <div className="page-card">
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>GROSS ASSETS</span>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#16a34a', marginTop: '0.3rem' }}>
                    {formatMoneyAmount(netWorthData.totalAssets, currency)}
                  </div>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Liquid + Equity + Gold + Land</span>
                </div>

                <div className="page-card">
                  <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>TOTAL LIABILITIES</span>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#dc2626', marginTop: '0.3rem' }}>
                    {formatMoneyAmount(netWorthData.totalLiabilities, currency)}
                  </div>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Debt-to-Asset: {netWorthData.debtToAssetRatio.toFixed(1)}%</span>
                </div>
              </div>

              {/* Asset Allocation Breakdown */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
                <div className="page-card">
                  <h3>Asset Category Distribution</h3>
                  <div style={{ width: '100%', height: 260 }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Pie data={netWorthPieData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85} paddingAngle={3}>
                          {netWorthPieData.map((entry, index) => (
                            <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(value) => formatMoneyAmount(value, currency)} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="page-card">
                  <h3>Assets & Liabilities Breakdown</h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.75rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9' }}>
                      <span>🏦 Liquid Bank Balances</span>
                      <strong>{formatMoneyAmount(netWorthData.liquidBankBalances, currency)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9' }}>
                      <span>📈 Mutual Funds & Equities</span>
                      <strong>{formatMoneyAmount(netWorthData.mutualFundsAndEquities, currency)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9' }}>
                      <span>🪙 Gold Holdings (SGB / Digital)</span>
                      <strong>{formatMoneyAmount(netWorthData.goldHoldings, currency)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9' }}>
                      <span>🏡 Real Estate & Property Equity</span>
                      <strong>{formatMoneyAmount(netWorthData.realEstateProperty, currency)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f5f9', color: '#dc2626' }}>
                      <span>💳 Credit Card Revolving Balance</span>
                      <strong>-{formatMoneyAmount(netWorthData.creditCardOutstanding, currency)}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', color: '#dc2626' }}>
                      <span>📋 Outstanding Loans</span>
                      <strong>-{formatMoneyAmount(netWorthData.loansOutstanding, currency)}</strong>
                    </div>
                  </div>
                </div>
              </div>
              <TabStepNav tabs={CREDIT_NETWORTH_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
            </div>
          )}

          {/* TAB 3: Debt Payoff Planner */}
          {activeTab === 'debt' && debtData && (
            <div>
              <div style={{ display: 'flex', gap: '0.6rem', marginBottom: '1.25rem' }}>
                <button
                  type="button"
                  className={`secondary-btn ${debtStrategy === 'AVALANCHE' ? 'active-filter' : ''}`}
                  onClick={() => setDebtStrategy('AVALANCHE')}
                  style={{ fontWeight: 700 }}
                >
                  ⚡ Debt Avalanche (Highest Interest First) [RECOMMENDED]
                </button>
                <button
                  type="button"
                  className={`secondary-btn ${debtStrategy === 'SNOWBALL' ? 'active-filter' : ''}`}
                  onClick={() => setDebtStrategy('SNOWBALL')}
                >
                  ⛄ Debt Snowball (Lowest Balance First)
                </button>
              </div>

              {/* Payoff Comparison Summary */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ background: debtStrategy === 'AVALANCHE' ? '#eff6ff' : '#ffffff', border: debtStrategy === 'AVALANCHE' ? '2px solid #3b82f6' : '1px solid #cbd5e1', padding: '1.25rem', borderRadius: '12px' }}>
                  <span className="tag success">MAXIMUM SAVINGS</span>
                  <h3 style={{ marginTop: '0.5rem' }}>Debt Avalanche</h3>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#1e293b' }}>
                    {debtData.avalancheMonthsToDebtFree} Months
                  </div>
                  <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
                    Total Interest Paid: <strong>{formatMoneyAmount(debtData.avalancheTotalInterest, currency)}</strong>
                  </p>
                  <div style={{ background: '#dcfce7', color: '#166534', padding: '0.4rem 0.6rem', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700, marginTop: '0.5rem' }}>
                    ✓ Saves you {formatMoneyAmount(debtData.interestSavedWithAvalanche, currency)} in interest!
                  </div>
                </div>

                <div style={{ background: debtStrategy === 'SNOWBALL' ? '#eff6ff' : '#ffffff', border: debtStrategy === 'SNOWBALL' ? '2px solid #3b82f6' : '1px solid #cbd5e1', padding: '1.25rem', borderRadius: '12px' }}>
                  <span className="tag warning">FASTEST PSYCHOLOGICAL WIN</span>
                  <h3 style={{ marginTop: '0.5rem' }}>Debt Snowball</h3>
                  <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#1e293b' }}>
                    {debtData.snowballMonthsToDebtFree} Months
                  </div>
                  <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
                    Total Interest Paid: <strong>{formatMoneyAmount(debtData.snowballTotalInterest, currency)}</strong>
                  </p>
                  <p style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '0.5rem' }}>
                    Knocks out the smallest loan in just 4 months to build unstoppable momentum.
                  </p>
                </div>
              </div>

              {/* Debts Table */}
              <div className="page-card">
                <h3>Active Debts & Priority Order</h3>
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Debt / Facility</th>
                        <th>Outstanding Balance</th>
                        <th>Interest Rate (APR)</th>
                        <th>Minimum Monthly EMI</th>
                        <th>Payoff Priority ({debtStrategy})</th>
                      </tr>
                    </thead>
                    <tbody>
                      {debtData.userDebts?.map((d, i) => (
                        <tr key={d.id}>
                          <td><strong>{d.debtName}</strong></td>
                          <td>{formatMoneyAmount(d.currentBalance, currency)}</td>
                          <td style={{ color: Number(d.interestRatePercent) > 20 ? '#dc2626' : '#0f172a', fontWeight: 700 }}>
                            {d.interestRatePercent}%
                          </td>
                          <td>{formatMoneyAmount(d.minimumMonthlyEmi, currency)}</td>
                          <td>
                            <span className="tag" style={{ background: i === 0 ? '#dbeafe' : '#f1f5f9', color: i === 0 ? '#1d4ed8' : '#475569' }}>
                              Priority #{i + 1} {i === 0 ? '🎯 Focus Extra EMI Here' : ''}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
              <TabStepNav tabs={CREDIT_NETWORTH_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
            </div>
          )}

          {/* TAB 4: Indian Section 80C & Tax Tracker */}
          {activeTab === 'tax' && taxData && (
            <div className="page-card">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Income Tax Act of 1961</p>
                  <h2>Section 80C & Capital Gains Tax Hub</h2>
                </div>
              </div>

              {/* 80C Bar */}
              <div style={{ background: '#f8fafc', padding: '1.25rem', borderRadius: '12px', border: '1px solid #e2e8f0', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <strong>Section 80C Limit: ₹1,50,000</strong>
                  <span style={{ fontSize: '0.85rem', color: '#16a34a', fontWeight: 700 }}>
                    {formatMoneyAmount(taxData.section80cClaimed, currency)} Claimed (₹{formatMoneyAmount(taxData.section80cRemaining, currency)} remaining)
                  </span>
                </div>
                <div className="progress-track" style={{ height: '10px' }}>
                  <div
                    className="progress-fill"
                    style={{
                      width: `${Math.min(100, (Number(taxData.section80cClaimed) / Number(taxData.section80cMaxLimit)) * 100)}%`,
                      background: 'linear-gradient(90deg, #10b981, #0284c7)',
                    }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', marginTop: '1.25rem' }}>
                  <div style={{ background: '#ffffff', padding: '0.65rem 0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>PPF</span>
                    <strong style={{ display: 'block' }}>{formatMoneyAmount(taxData.ppfContribution, currency)}</strong>
                  </div>
                  <div style={{ background: '#ffffff', padding: '0.65rem 0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>ELSS Tax-Saver Mutual Funds</span>
                    <strong style={{ display: 'block' }}>{formatMoneyAmount(taxData.elssContribution, currency)}</strong>
                  </div>
                  <div style={{ background: '#ffffff', padding: '0.65rem 0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>EPF (Employee Provident Fund)</span>
                    <strong style={{ display: 'block' }}>{formatMoneyAmount(taxData.epfContribution, currency)}</strong>
                  </div>
                  <div style={{ background: '#ffffff', padding: '0.65rem 0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Life / Term Insurance</span>
                    <strong style={{ display: 'block' }}>{formatMoneyAmount(taxData.lifeInsuranceContribution, currency)}</strong>
                  </div>
                </div>
              </div>

              {/* Capital Gains & ITR Recommendation */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4>Capital Gains Summary (FY 2026-27)</h4>
                  <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                    Estimated LTCG Tax (12.5% above ₹1.25L exemption): <strong>{formatMoneyAmount(taxData.estimatedLtcgTax, currency)}</strong><br />
                    Estimated STCG Tax (20% on short-term sales): <strong>{formatMoneyAmount(taxData.estimatedStcgTax, currency)}</strong>
                  </p>
                </div>
                <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                  <h4>ITR Filing Guide & Regime</h4>
                  <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                    Suggested Form: <strong>{taxData.itrFormSuggestion}</strong><br />
                    {taxData.taxRegimeComparison}
                  </p>
                </div>
              </div>
              <TabStepNav tabs={CREDIT_NETWORTH_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
            </div>
          )}
        </>
      )}

      <PageNavFooter />
    </div>
  );
}
