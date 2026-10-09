import React, { useState } from 'react';
import { PageNavHeader, PageNavFooter, TabStepNav } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

const GAMIFICATION_TABS = [
  { id: 'health', label: 'Financial Health Score' },
  { id: 'challenges', label: 'Streaks & Badges' },
  { id: 'roundup', label: 'Spare Change Round-Up' },
  { id: 'split', label: 'Split Bills' },
  { id: 'family', label: 'Family Shared Wallets' },
];

export default function GamificationAndSocialPage() {
  const currency = useSelectedCurrency();
  const [activeTab, setActiveTab] = useState('health'); // health, challenges, roundup, split, family

  // Bill Splitting State
  const [splitBillAmount, setSplitBillAmount] = useState('3600');
  const [splitPeopleCount, setSplitPeopleCount] = useState('4');
  const [splitNote, setSplitNote] = useState('Dinner at Social');
  const [splitUpiVpa, setSplitUpiVpa] = useState('user@okaxis');
  const [copiedLink, setCopiedLink] = useState(false);

  // Round-Up Savings state
  const [roundUpTier, setRoundUpTier] = useState(50); // nearest 10 or 50
  const [roundUpEnabled, setRoundUpEnabled] = useState(true);

  // Shared Family Wallet state
  const [kidAllowance, setKidAllowance] = useState('2500');
  const [familyMembers, setFamilyMembers] = useState([
    { name: 'Devansh (Primary)', role: 'Admin', limit: 85000, spent: 34200 },
    { name: 'Priya (Partner)', role: 'Co-Owner', limit: 45000, spent: 18900 },
    { name: 'Aarav (Kid)', role: 'Student Allowance', limit: 2500, spent: 850 },
  ]);

  const perPersonSplit = (Number(splitBillAmount || 0) / Math.max(1, Number(splitPeopleCount || 1))).toFixed(2);
  const upiDeepLink = `upi://pay?pa=${encodeURIComponent(splitUpiVpa)}&pn=PulseWallet&am=${perPersonSplit}&cu=INR&tn=${encodeURIComponent(splitNote)}`;

  function handleCopySplitLink() {
    navigator.clipboard?.writeText(upiDeepLink);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 3000);
  }

  return (
    <div className="page-shell">
      <PageNavHeader title="Gamification & Social Hub" />

      <div className="page-header">
        <div>
          <p className="eyebrow">Financial Health Score • Challenges • Bill Split • Family Wallets</p>
          <h1>Gamification, Social & Retention Hub</h1>
        </div>
      </div>

      {/* Tabs */}
      <div className="cc-tabs" style={{ marginBottom: '1.5rem' }}>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'health' ? 'active' : ''}`}
          onClick={() => setActiveTab('health')}
        >
          🏅 Financial Health Score (84/100)
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'challenges' ? 'active' : ''}`}
          onClick={() => setActiveTab('challenges')}
        >
          🔥 Streaks & Challenges
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'roundup' ? 'active' : ''}`}
          onClick={() => setActiveTab('roundup')}
        >
          🪙 Round-Up Spare Change
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'split' ? 'active' : ''}`}
          onClick={() => setActiveTab('split')}
        >
          👥 Split Bills with UPI QR
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'family' ? 'active' : ''}`}
          onClick={() => setActiveTab('family')}
        >
          👨‍👩‍👧 Shared Family Wallet
        </button>
      </div>

      {/* TAB 1: Financial Health Score */}
      {activeTab === 'health' && (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
            <div className="page-card" style={{ textAlign: 'center', padding: '2rem 1.5rem' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>
                OVERALL FINANCIAL HEALTH
              </span>
              <div style={{ fontSize: '4rem', fontWeight: 900, color: '#16a34a', margin: '0.3rem 0' }}>
                84<span style={{ fontSize: '1.6rem', color: '#94a3b8' }}>/100</span>
              </div>
              <span style={{ background: '#dcfce7', color: '#166534', fontWeight: 700, padding: '0.25rem 0.8rem', borderRadius: '9999px', fontSize: '0.82rem' }}>
                🌟 FINANCIALLY FIT (TOP 12% DISCIPLINE)
              </span>
              <p style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.75rem' }}>
                Calculated across savings rate, budget compliance, emergency cushion, and debt ratios.
              </p>
            </div>

            <div className="page-card">
              <h3 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Score Breakdown</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.2rem' }}>
                    <span>Savings Rate (35% of income)</span>
                    <strong>23 / 25 pts</strong>
                  </div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: '92%', background: '#16a34a' }} /></div>
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.2rem' }}>
                    <span>Budget Discipline</span>
                    <strong>21 / 25 pts</strong>
                  </div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: '84%', background: '#0284c7' }} /></div>
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.2rem' }}>
                    <span>Emergency Runway (4.5 months)</span>
                    <strong>22 / 25 pts</strong>
                  </div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: '88%', background: '#8b5cf6' }} /></div>
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: '0.2rem' }}>
                    <span>Debt-to-Income Health</span>
                    <strong>18 / 25 pts</strong>
                  </div>
                  <div className="progress-track"><div className="progress-fill" style={{ width: '72%', background: '#f59e0b' }} /></div>
                </div>
              </div>
            </div>
            <TabStepNav tabs={GAMIFICATION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
          </div>
        </div>
      )}

      {/* TAB 2: Streaks & Badges */}
      {activeTab === 'challenges' && (
        <div className="page-card">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Habit Formation</p>
              <h2>Active Savings Streaks & Challenges</h2>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            <div style={{ background: '#f0fdf4', border: '1px solid #86efac', padding: '1.25rem', borderRadius: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
                <span style={{ fontSize: '1.5rem' }}>🥗</span>
                <strong style={{ fontSize: '1rem', color: '#166534' }}>7-Day No Food Delivery Streak</strong>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#15803d', margin: '0 0 0.8rem' }}>
                Cook or dine at home for 7 consecutive days to save estimated ₹3,200.
              </p>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 700, color: '#166534', marginBottom: '0.25rem' }}>
                <span>Day 5 of 7 Completed!</span>
                <span>🔥 5-Day Active Streak</span>
              </div>
              <div className="progress-track"><div className="progress-fill" style={{ width: '71%', background: '#16a34a' }} /></div>
            </div>

            <div style={{ background: '#eff6ff', border: '1px solid #93c5fd', padding: '1.25rem', borderRadius: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
                <span style={{ fontSize: '1.5rem' }}>🎯</span>
                <strong style={{ fontSize: '1rem', color: '#1e40af' }}>₹5,000 Micro-Save Sprint</strong>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#1d4ed8', margin: '0 0 0.8rem' }}>
                Save ₹5,000 spare cash into your primary goal within 30 days.
              </p>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 700, color: '#1e40af', marginBottom: '0.25rem' }}>
                <span>₹3,850 of ₹5,000 saved</span>
                <span>77% Complete</span>
              </div>
              <div className="progress-track"><div className="progress-fill" style={{ width: '77%', background: '#2563eb' }} /></div>
            </div>

            <div style={{ background: '#faf5ff', border: '1px solid #d8b4fe', padding: '1.25rem', borderRadius: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
                <span style={{ fontSize: '1.5rem' }}>🛡️</span>
                <strong style={{ fontSize: '1rem', color: '#6b21a8' }}>Zero-Overrun Hero Badge</strong>
              </div>
              <p style={{ fontSize: '0.82rem', color: '#7e22ce', margin: '0 0 0.8rem' }}>
                Stay 100% within your monthly budget limits for 2 consecutive billing cycles.
              </p>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 700, color: '#6b21a8', marginBottom: '0.25rem' }}>
                <span>Cycle 1 of 2 Complete</span>
                <span>🏅 Gold Badge Locked</span>
              </div>
              <div className="progress-track"><div className="progress-fill" style={{ width: '50%', background: '#9333ea' }} /></div>
            </div>
          </div>
          <TabStepNav tabs={GAMIFICATION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      {/* TAB 3: Round-Up Savings */}
      {activeTab === 'roundup' && (
        <div className="page-card">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Micro-Investing Spare Change</p>
              <h2>Automatic Round-Up Savings</h2>
            </div>
          </div>
          <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1.25rem' }}>
            Whenever you spend via UPI, card, or net banking, PulseWallet rounds up the purchase to the nearest ₹10 or ₹50 and sweeps the spare change directly into your goal or liquid mutual fund!
          </p>

          <div style={{ background: '#f8fafc', padding: '1.25rem', borderRadius: '12px', border: '1px solid #e2e8f0', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <strong style={{ fontSize: '1rem', color: '#0f172a' }}>Round-up Rule:</strong>
                <p style={{ fontSize: '0.82rem', color: '#64748b', margin: '0.2rem 0 0' }}>
                  e.g., A ₹463 Swiggy order rounds to ₹500 → <strong>₹37</strong> goes straight to your savings.
                </p>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setRoundUpTier(10)}
                  style={{
                    padding: '0.4rem 0.8rem',
                    borderRadius: '6px',
                    border: roundUpTier === 10 ? '2px solid #2563eb' : '1px solid #cbd5e1',
                    background: roundUpTier === 10 ? '#eff6ff' : '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Nearest ₹10
                </button>
                <button
                  type="button"
                  onClick={() => setRoundUpTier(50)}
                  style={{
                    padding: '0.4rem 0.8rem',
                    borderRadius: '6px',
                    border: roundUpTier === 50 ? '2px solid #2563eb' : '1px solid #cbd5e1',
                    background: roundUpTier === 50 ? '#eff6ff' : '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Nearest ₹50 (Recommended)
                </button>
              </div>
            </div>

            <div style={{ marginTop: '1.25rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
              <div style={{ background: '#ffffff', padding: '0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '0.72rem', color: '#64748b', textTransform: 'uppercase' }}>SPARE CHANGE ACCUMULATED THIS MONTH</span>
                <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#16a34a' }}>₹1,482.00</div>
              </div>
              <div style={{ background: '#ffffff', padding: '0.85rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <span style={{ fontSize: '0.72rem', color: '#64748b', textTransform: 'uppercase' }}>PROJECTED ANNUAL SPARE COMPOUND</span>
                <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0284c7' }}>₹20,150.00</div>
              </div>
            </div>
          </div>
          <TabStepNav tabs={GAMIFICATION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      {/* TAB 4: Bill Splitting with UPI QR */}
      {activeTab === 'split' && (
        <div className="page-card">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Social Settlements</p>
              <h2>Instant Bill Splitting with UPI QR & Deep Link</h2>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.5rem' }}>
            <div>
              <div style={{ marginBottom: '0.85rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.3rem' }}>Total Bill Amount (₹):</label>
                <input
                  type="number"
                  value={splitBillAmount}
                  onChange={(e) => setSplitBillAmount(e.target.value)}
                  style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ marginBottom: '0.85rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.3rem' }}>Number of People:</label>
                <input
                  type="number"
                  min="1"
                  value={splitPeopleCount}
                  onChange={(e) => setSplitPeopleCount(e.target.value)}
                  style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ marginBottom: '0.85rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.3rem' }}>Your UPI ID (VPA):</label>
                <input
                  type="text"
                  value={splitUpiVpa}
                  onChange={(e) => setSplitUpiVpa(e.target.value)}
                  style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                />
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.3rem' }}>Bill Note:</label>
                <input
                  type="text"
                  value={splitNote}
                  onChange={(e) => setSplitNote(e.target.value)}
                  style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                />
              </div>
            </div>

            {/* Split Result Card */}
            <div style={{ background: '#f8fafc', padding: '1.5rem', borderRadius: '14px', border: '1px solid #e2e8f0', textAlign: 'center' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>EACH PERSON PAYS</span>
              <div style={{ fontSize: '2.6rem', fontWeight: 900, color: '#0f172a', margin: '0.2rem 0' }}>
                ₹{perPersonSplit}
              </div>
              <p style={{ fontSize: '0.82rem', color: '#64748b' }}>
                Total: ₹{splitBillAmount} split among {splitPeopleCount} friends
              </p>

              {/* QR Code Container simulation */}
              <div style={{ margin: '1rem auto', width: '140px', height: '140px', background: '#ffffff', border: '2px solid #0f172a', borderRadius: '10px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                <span style={{ fontSize: '2.5rem' }}>📱</span>
                <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#0f172a' }}>Scan to Pay ₹{perPersonSplit}</span>
              </div>

              <button
                type="button"
                className="primary-btn"
                style={{ width: '100%', marginBottom: '0.5rem' }}
                onClick={handleCopySplitLink}
              >
                {copiedLink ? '✓ UPI Link Copied!' : '📋 Copy UPI Payment Link'}
              </button>

              <span style={{ fontSize: '0.72rem', color: '#64748b' }}>
                Share link via WhatsApp, Telegram, or SMS. Supports GPay, PhonePe & Paytm.
              </span>
            </div>
          </div>
          <TabStepNav tabs={GAMIFICATION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      {/* TAB 5: Shared Family Wallet */}
      {activeTab === 'family' && (
        <div className="page-card">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Household Financial Governance</p>
              <h2>Shared Wallets & Student Allowance Limits</h2>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1rem' }}>
            {familyMembers.map((member, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1rem 1.25rem',
                  background: '#f8fafc',
                  borderRadius: '12px',
                  border: '1px solid #e2e8f0',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <strong style={{ fontSize: '1rem', color: '#0f172a' }}>{member.name}</strong>
                    <span className="tag" style={{ background: '#e0f2fe', color: '#0369a1' }}>{member.role}</span>
                  </div>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
                    Monthly Limit: ₹{member.limit.toLocaleString()} • Spent: ₹{member.spent.toLocaleString()}
                  </span>
                </div>

                <div style={{ width: '180px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: '#64748b', marginBottom: '0.2rem' }}>
                    <span>Utilized</span>
                    <strong>{Math.round((member.spent / member.limit) * 100)}%</strong>
                  </div>
                  <div className="progress-track">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${Math.min(100, (member.spent / member.limit) * 100)}%`,
                        background: (member.spent / member.limit) > 0.8 ? '#ef4444' : '#10b981',
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
          <TabStepNav tabs={GAMIFICATION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      <PageNavFooter />
    </div>
  );
}
