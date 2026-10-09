import React, { useState, useMemo, useEffect } from 'react';
import { formatMoneyAmount } from '../utils/currency';

const QUICK_INCOME_PRESETS = [
  { label: '₹25K', value: 25000 },
  { label: '₹50K', value: 50000 },
  { label: '₹80K', value: 80000 },
  { label: '₹1.5L', value: 150000 },
  { label: '₹3L', value: 300000 },
];

export default function IncomeAdvisoryColumn({
  detectedIncome = 50000,
  currencyCode = 'INR',
  theme = 'money',
}) {
  // Use detected income if available, fallback to 50,000
  const initial = Number(detectedIncome) > 0 ? Number(detectedIncome) : 50000;
  const [income, setIncome] = useState(initial);
  const [riskTolerance, setRiskTolerance] = useState('BALANCED'); // CONSERVATIVE, BALANCED, AGGRESSIVE
  const [isSimulating, setIsSimulating] = useState(false);

  useEffect(() => {
    if (Number(detectedIncome) > 0 && !isSimulating) {
      setIncome(Number(detectedIncome));
    }
  }, [detectedIncome, isSimulating]);

  // Derived financial computations based on income
  const analysis = useMemo(() => {
    const inc = Math.max(1000, Number(income) || 0);

    // 50-30-20 Rule breakdown
    let needsPct = 50;
    let wantsPct = 30;
    let investPct = 20;

    if (riskTolerance === 'CONSERVATIVE') {
      needsPct = 55;
      wantsPct = 25;
      investPct = 20;
    } else if (riskTolerance === 'AGGRESSIVE') {
      needsPct = 45;
      wantsPct = 25;
      investPct = 30;
    }

    const needsAmount = (inc * needsPct) / 100;
    const wantsAmount = (inc * wantsPct) / 100;
    const investAmount = (inc * investPct) / 100;

    // Emergency fund recommendation: 6 months of basic needs
    const emergencyFundGoal = needsAmount * 6;

    // SIP Allocations
    let equitySip = investAmount * 0.6;
    let midSmallSip = investAmount * 0.25;
    let goldDebtSip = investAmount * 0.15;

    if (riskTolerance === 'CONSERVATIVE') {
      equitySip = investAmount * 0.4;
      midSmallSip = investAmount * 0.15;
      goldDebtSip = investAmount * 0.45;
    } else if (riskTolerance === 'AGGRESSIVE') {
      equitySip = investAmount * 0.5;
      midSmallSip = investAmount * 0.4;
      goldDebtSip = investAmount * 0.1;
    }

    // Tax Saving Suggestion (Section 80C limit is ₹1,50,000 / year = ₹12,500 / month)
    const monthly80CLimit = 12500;
    const taxSavingSip = Math.min(investAmount * 0.4, monthly80CLimit);

    // Projected 5-Year Wealth (SIP formula with ~12% CAGR)
    // FV = P * [((1 + r)^n - 1) / r] * (1 + r)
    const monthlyRate = 0.12 / 12;
    const months = 60;
    const projectedCorpus5Years =
      investAmount * (((Math.pow(1 + monthlyRate, months) - 1) / monthlyRate) * (1 + monthlyRate));

    // Income Bracket Categorization & Tailored Advice
    let bracket = {
      name: 'Starter Tier (Seed)',
      badgeColor: '#10b981',
      summary: 'Focus on high-liquidity capital preservation and establishing your first emergency safety net.',
      primaryFocus: 'Emergency Buffer & Zero High-Interest Debt',
      sipStrategy: 'Micro-SIP in broad Nifty 50 Index funds. Start building healthy financial discipline.',
      pitfall: 'Avoid instant loan apps, buy-now-pay-later (BNPL), and impulse subscriptions.',
    };

    if (inc >= 30000 && inc < 75000) {
      bracket = {
        name: 'Growth Professional Tier',
        badgeColor: '#38bdf8',
        summary: 'Accelerate wealth compounding through systematic equity exposure and debt reduction.',
        primaryFocus: 'Disciplined 50/30/20 Budgeting & Term Insurance',
        sipStrategy: 'Index Fund (50%) + Flexicap Fund (30%) + Sovereign Gold Bond (20%).',
        pitfall: 'Watch out for vehicle loans or upgrading gadgets prematurely as salary increases.',
      };
    } else if (inc >= 75000 && inc < 150000) {
      bracket = {
        name: 'High-Velocity Wealth Builder',
        badgeColor: '#f59e0b',
        summary: 'Maximize tax efficiency (80C, 80D, NPS) and aggressively scale mutual fund equity allocations.',
        primaryFocus: 'Tax Alpha & Mid-to-Large Equity Accumulation',
        sipStrategy: 'Nifty 50 (40%) + Midcap 150 (30%) + Smallcap 250 (15%) + Gold/Arbitrage (15%).',
        pitfall: 'Avoid over-concentrating in single stock speculation without proper stop-losses.',
      };
    } else if (inc >= 150000) {
      bracket = {
        name: 'HNW Strategic Portfolio',
        badgeColor: '#a855f7',
        summary: 'Institutional multi-asset diversification across domestic equity, global funds, and debt vaults.',
        primaryFocus: 'Capital Preservation, Global Hedging & Estate Structuring',
        sipStrategy: 'Direct Equity & PMS + Nasdaq/International ETFs (15%) + Arbitrage & Liquid Debt Funds.',
        pitfall: 'Lifestyle creep. Ensure savings rate remains above 35% of post-tax income.',
      };
    }

    return {
      income: inc,
      needsPct,
      wantsPct,
      investPct,
      needsAmount,
      wantsAmount,
      investAmount,
      emergencyFundGoal,
      equitySip,
      midSmallSip,
      goldDebtSip,
      taxSavingSip,
      projectedCorpus5Years,
      bracket,
    };
  }, [income, riskTolerance]);

  return (
    <div className="income-advisory-column" aria-label="AI Income-Based Recommendations">
      <div className="panel-header" style={{ marginBottom: '1rem' }}>
        <div>
          <p className="eyebrow">Personalized AI Financial Roadmap</p>
          <h3 id="income-advisory-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span>🧠</span> AI Advice by Income
          </h3>
        </div>
        <span
          className="engine-badge"
          style={{
            background: `${analysis.bracket.badgeColor}20`,
            color: analysis.bracket.badgeColor,
            border: `1px solid ${analysis.bracket.badgeColor}50`,
          }}
        >
          {analysis.bracket.name}
        </span>
      </div>

      {/* Income Simulator Controls */}
      <div className="income-simulator-card" style={{ background: 'rgba(255, 255, 255, 0.04)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '12px', padding: '1rem', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted, #94a3b8)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Monthly Income Basis
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <span style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--theme-accent, #38bdf8)' }}>
              {formatMoneyAmount(analysis.income, currencyCode)}
            </span>
            <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>/ month</span>
          </div>
        </div>

        {/* Quick presets */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginBottom: '0.75rem' }}>
          {QUICK_INCOME_PRESETS.map((preset) => (
            <button
              key={preset.value}
              type="button"
              onClick={() => {
                setIncome(preset.value);
                setIsSimulating(true);
              }}
              style={{
                border: income === preset.value ? '1px solid #38bdf8' : '1px solid rgba(255,255,255,0.12)',
                background: income === preset.value ? 'rgba(56, 189, 248, 0.2)' : 'rgba(255,255,255,0.04)',
                color: income === preset.value ? '#38bdf8' : 'inherit',
                padding: '0.25rem 0.55rem',
                borderRadius: '6px',
                fontSize: '0.75rem',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              {preset.label}
            </button>
          ))}
          {Number(detectedIncome) > 0 && isSimulating && (
            <button
              type="button"
              onClick={() => {
                setIncome(Number(detectedIncome));
                setIsSimulating(false);
              }}
              style={{
                border: '1px dashed #22c55e',
                background: 'rgba(34, 197, 94, 0.15)',
                color: '#4ade80',
                padding: '0.25rem 0.55rem',
                borderRadius: '6px',
                fontSize: '0.75rem',
                cursor: 'pointer',
              }}
            >
              Reset to Actual ({formatMoneyAmount(detectedIncome, currencyCode)})
            </button>
          )}
        </div>

        {/* Range slider */}
        <input
          type="range"
          min="10000"
          max="300000"
          step="5000"
          value={income}
          onChange={(e) => {
            setIncome(Number(e.target.value));
            setIsSimulating(true);
          }}
          style={{ width: '100%', accentColor: 'var(--theme-accent, #38bdf8)', cursor: 'pointer' }}
        />

        {/* Risk appetite pills */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.75rem', paddingTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
          <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Strategy Profile:</span>
          <div style={{ display: 'flex', gap: '0.35rem' }}>
            {['CONSERVATIVE', 'BALANCED', 'AGGRESSIVE'].map((mode) => (
              <button
                key={mode}
                type="button"
                onClick={() => setRiskTolerance(mode)}
                style={{
                  padding: '0.2rem 0.5rem',
                  borderRadius: '4px',
                  fontSize: '0.7rem',
                  border: 'none',
                  cursor: 'pointer',
                  fontWeight: 600,
                  background: riskTolerance === mode ? 'var(--theme-accent, #0284c7)' : 'rgba(255,255,255,0.08)',
                  color: riskTolerance === mode ? '#ffffff' : '#94a3b8',
                }}
              >
                {mode.charAt(0) + mode.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 50 / 30 / 20 Dynamic Distribution Breakdown */}
      <div style={{ marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.35rem' }}>
          <span><strong>Budget Allocation</strong> ({analysis.needsPct}% / {analysis.wantsPct}% / {analysis.investPct}%)</span>
          <span style={{ color: '#94a3b8' }}>Ideal Rule</span>
        </div>

        {/* Segmented bar */}
        <div style={{ display: 'flex', height: '10px', borderRadius: '6px', overflow: 'hidden', background: '#334155', marginBottom: '0.6rem' }}>
          <div style={{ width: `${analysis.needsPct}%`, background: '#38bdf8' }} title={`Needs: ${formatMoneyAmount(analysis.needsAmount, currencyCode)}`} />
          <div style={{ width: `${analysis.wantsPct}%`, background: '#f59e0b' }} title={`Wants: ${formatMoneyAmount(analysis.wantsAmount, currencyCode)}`} />
          <div style={{ width: `${analysis.investPct}%`, background: '#22c55e' }} title={`Investments: ${formatMoneyAmount(analysis.investAmount, currencyCode)}`} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem', fontSize: '0.75rem' }}>
          <div style={{ background: 'rgba(56, 189, 248, 0.08)', padding: '0.5rem', borderRadius: '6px', borderLeft: '3px solid #38bdf8' }}>
            <span style={{ color: '#94a3b8', display: 'block' }}>Needs ({analysis.needsPct}%)</span>
            <strong style={{ color: '#e0f2fe' }}>{formatMoneyAmount(analysis.needsAmount, currencyCode)}</strong>
          </div>
          <div style={{ background: 'rgba(245, 158, 11, 0.08)', padding: '0.5rem', borderRadius: '6px', borderLeft: '3px solid #f59e0b' }}>
            <span style={{ color: '#94a3b8', display: 'block' }}>Wants ({analysis.wantsPct}%)</span>
            <strong style={{ color: '#fef3c7' }}>{formatMoneyAmount(analysis.wantsAmount, currencyCode)}</strong>
          </div>
          <div style={{ background: 'rgba(34, 197, 94, 0.08)', padding: '0.5rem', borderRadius: '6px', borderLeft: '3px solid #22c55e' }}>
            <span style={{ color: '#94a3b8', display: 'block' }}>Invest ({analysis.investPct}%)</span>
            <strong style={{ color: '#dcfce7' }}>{formatMoneyAmount(analysis.investAmount, currencyCode)}</strong>
          </div>
        </div>
      </div>

      {/* AI Tailored Recommendations List */}
      <div className="income-suggestions-stack" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {/* Recommended SIP */}
        <div style={{ background: 'rgba(255, 255, 255, 0.04)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '0.85rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#4ade80' }}>
              📈 Recommended Monthly SIP
            </span>
            <strong style={{ color: '#22c55e', fontSize: '0.95rem' }}>
              {formatMoneyAmount(analysis.investAmount, currencyCode)} / mo
            </strong>
          </div>
          <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)', lineHeight: 1.4 }}>
            {analysis.bracket.sipStrategy}
          </p>
          <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.5rem', flexWrap: 'wrap', fontSize: '0.7rem' }}>
            <span style={{ background: 'rgba(34, 197, 94, 0.15)', color: '#4ade80', padding: '0.15rem 0.4rem', borderRadius: '4px' }}>
              Largecap Index: {formatMoneyAmount(analysis.equitySip, currencyCode)}
            </span>
            <span style={{ background: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8', padding: '0.15rem 0.4rem', borderRadius: '4px' }}>
              Mid & Smallcap: {formatMoneyAmount(analysis.midSmallSip, currencyCode)}
            </span>
            <span style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24', padding: '0.15rem 0.4rem', borderRadius: '4px' }}>
              Gold / Debt: {formatMoneyAmount(analysis.goldDebtSip, currencyCode)}
            </span>
          </div>
        </div>

        {/* Emergency Fund Target */}
        <div style={{ background: 'rgba(255, 255, 255, 0.04)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '0.85rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#38bdf8' }}>
              🛡️ Target Emergency Shield (6 Mo)
            </span>
            <strong style={{ color: '#38bdf8', fontSize: '0.95rem' }}>
              {formatMoneyAmount(analysis.emergencyFundGoal, currencyCode)}
            </strong>
          </div>
          <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-muted, #94a3b8)', lineHeight: 1.4 }}>
            Based on monthly essential expenses of {formatMoneyAmount(analysis.needsAmount, currencyCode)}. Keep 70% in Liquid/Arbitrage Fund and 30% in high-yield savings.
          </p>
        </div>

        {/* 5-Year Compounding Milestone */}
        <div style={{ background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(6, 182, 212, 0.08))', border: '1px solid rgba(16, 185, 129, 0.25)', borderRadius: '10px', padding: '0.85rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.85rem', color: '#34d399' }}>
              🚀 5-Year Wealth Milestone
            </span>
            <strong style={{ color: '#34d399', fontSize: '1.05rem' }}>
              ~{formatMoneyAmount(analysis.projectedCorpus5Years, currencyCode)}
            </strong>
          </div>
          <p style={{ margin: 0, fontSize: '0.75rem', color: '#a7f3d0', lineHeight: 1.4 }}>
            Projected value by deploying {formatMoneyAmount(analysis.investAmount, currencyCode)}/month with disciplined 12% annual compounding across Indian equity index & flexicap funds.
          </p>
        </div>

        {/* Pitfall to avoid */}
        <div style={{ background: 'rgba(239, 68, 68, 0.06)', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '10px', padding: '0.75rem' }}>
          <strong style={{ display: 'block', fontSize: '0.78rem', color: '#f87171', marginBottom: '0.2rem' }}>
            ⚠️ Pitfall to Avoid at This Income
          </strong>
          <p style={{ margin: 0, fontSize: '0.75rem', color: '#fca5a5', lineHeight: 1.35 }}>
            {analysis.bracket.pitfall}
          </p>
        </div>
      </div>
    </div>
  );
}
