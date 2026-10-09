import React, { useState } from 'react';
import './PulseAiAssistant.css';

export default function OnboardingWizardModal({ isOpen, onClose, onComplete }) {
  const [step, setStep] = useState(1);
  const [income, setIncome] = useState('85000');
  const [goal, setGoal] = useState('Car / Vehicle');
  const [goalAmount, setGoalAmount] = useState('800000');
  const [risk, setRisk] = useState('MODERATE');

  if (!isOpen) return null;

  function handleFinish() {
    onComplete?.({ income, goal, goalAmount, risk });
    onClose();
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="modal-card" role="dialog" aria-modal="true" style={{ maxWidth: '520px' }}>
        <div className="panel-header">
          <div>
            <p className="eyebrow">Quick Setup (&lt; 2 Minutes)</p>
            <h2>Welcome to PulseWallet!</h2>
          </div>
          <button type="button" className="link-btn" onClick={onClose}>Skip</button>
        </div>

        {/* Step Indicator */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem' }}>
          {[1, 2, 3].map((s) => (
            <div
              key={s}
              style={{
                flex: 1,
                height: '6px',
                borderRadius: '9999px',
                background: s <= step ? 'linear-gradient(90deg, #2563eb, #3b82f6)' : '#e2e8f0',
                transition: 'all 0.3s ease',
              }}
            />
          ))}
        </div>

        {step === 1 && (
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.4rem' }}>Step 1: What is your estimated monthly income?</h3>
            <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1rem' }}>
              Used to compute your automated 50/30/20 budget allocations and Safe-to-Spend limits.
            </p>
            <input
              type="number"
              value={income}
              onChange={(e) => setIncome(e.target.value)}
              placeholder="e.g. 85000"
              style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '1.2rem', fontWeight: 700 }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
              <button type="button" className="secondary-btn" onClick={onClose}>Skip Setup</button>
              <button type="button" className="primary-btn" onClick={() => setStep(2)}>Next: Primary Goal →</button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.4rem' }}>Step 2: What is your #1 financial goal right now?</h3>
            <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1rem' }}>
              We will calculate an exact monthly SIP roadmap to hit your target.
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginBottom: '1rem' }}>
              {['Emergency Fund', 'Car / Vehicle', 'Home Down Payment', 'Wealth Creation'].map((g) => (
                <button
                  key={g}
                  type="button"
                  onClick={() => setGoal(g)}
                  style={{
                    padding: '0.65rem',
                    borderRadius: '8px',
                    border: goal === g ? '2px solid #2563eb' : '1px solid #cbd5e1',
                    background: goal === g ? '#eff6ff' : '#ffffff',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  {g}
                </button>
              ))}
            </div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Target Goal Amount (₹):</label>
            <input
              type="number"
              value={goalAmount}
              onChange={(e) => setGoalAmount(e.target.value)}
              style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
              <button type="button" className="secondary-btn" onClick={() => setStep(1)}>← Back</button>
              <button type="button" className="primary-btn" onClick={() => setStep(3)}>Next: Risk Profile →</button>
            </div>
          </div>
        )}

        {step === 3 && (
          <div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.4rem' }}>Step 3: What is your investment risk profile?</h3>
            <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1rem' }}>
              Determines asset allocation across Equity, Debt, and Gold.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.25rem' }}>
              {[
                { key: 'CONSERVATIVE', title: 'Conservative (Low Risk)', desc: 'Capital preservation first. High debt, FD & arbitrage allocation.' },
                { key: 'MODERATE', title: 'Moderate (Balanced)', desc: 'Healthy balance of index funds, large caps, and fixed income.' },
                { key: 'AGGRESSIVE', title: 'Aggressive (High Growth)', desc: 'Long-term equity compounding via Flexi Cap & Small Cap funds.' },
              ].map((r) => (
                <button
                  key={r.key}
                  type="button"
                  onClick={() => setRisk(r.key)}
                  style={{
                    padding: '0.75rem 1rem',
                    borderRadius: '8px',
                    textAlign: 'left',
                    border: risk === r.key ? '2px solid #2563eb' : '1px solid #cbd5e1',
                    background: risk === r.key ? '#eff6ff' : '#ffffff',
                    cursor: 'pointer',
                  }}
                >
                  <strong style={{ display: 'block', fontSize: '0.9rem' }}>{r.title}</strong>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>{r.desc}</span>
                </button>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <button type="button" className="secondary-btn" onClick={() => setStep(2)}>← Back</button>
              <button type="button" className="primary-btn" onClick={handleFinish}>🎉 Complete Setup & Launch</button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
