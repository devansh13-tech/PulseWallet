import { useEffect, useState } from 'react';
import { creditCardApi } from '../lib/api';
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency';

const EMPTY_FORM = {
  issuer: '', cardName: '', lastFourDigits: '', creditLimit: '', currentOutstanding: '0',
  statementDate: '', dueDate: '', rewardsDescription: '', benefitsDescription: '',
};

const PURCHASE_EMPTY = { amount: '', category: '', purpose: '' };

function money(value, currency) { return formatMoneyAmount(value, currency); }

function utilizationColor(level) {
  switch (level) {
    case 'LOW': return '#15803d';
    case 'MODERATE': return '#b45309';
    case 'HIGH': return '#dc2626';
    case 'VERY_HIGH': return '#991b1b';
    default: return '#475569';
  }
}

function healthBadge(status) {
  switch (status) {
    case 'HEALTHY': return 'success';
    case 'WATCH': return 'warning-tag';
    case 'WARNING': return 'danger';
    case 'CRITICAL': return 'danger';
    default: return '';
  }
}

function safetyBadge(status) {
  switch (status) {
    case 'SAFE': return 'success';
    case 'CAUTION': return 'warning-tag';
    case 'NOT_RECOMMENDED': return 'danger';
    default: return '';
  }
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value + 'T00:00:00');
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(date);
}

export default function CreditCardsPage() {
  const currency = useSelectedCurrency();
  const [cards, setCards] = useState([]);
  const [intelligence, setIntelligence] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [purchaseForm, setPurchaseForm] = useState(PURCHASE_EMPTY);
  const [purchaseResult, setPurchaseResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('cards');

  async function loadData() {
    setLoading(true); setError('');
    try {
      const [cardData, intelData] = await Promise.allSettled([
        creditCardApi.list(), creditCardApi.intelligence(),
      ]);
      setCards(cardData.status === 'fulfilled' ? cardData.value : []);
      setIntelligence(intelData.status === 'fulfilled' ? intelData.value : null);
    } catch (err) { setError(err?.message || 'Unable to load credit card data.'); }
    finally { setLoading(false); }
  }

  useEffect(() => { loadData(); }, []);

  function change(field) {
    return (e) => setForm((c) => ({ ...c, [field]: e.target.value }));
  }

  function edit(card) {
    setEditingId(card.id);
    setForm({
      issuer: card.issuer, cardName: card.cardName, lastFourDigits: card.lastFourDigits,
      creditLimit: card.creditLimit, currentOutstanding: card.currentOutstanding,
      statementDate: card.statementDate, dueDate: card.dueDate,
      rewardsDescription: card.rewardsDescription || '', benefitsDescription: card.benefitsDescription || '',
    });
    setActiveTab('cards');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function resetForm() { setEditingId(null); setForm(EMPTY_FORM); }

  async function submit(e) {
    e.preventDefault(); setSaving(true); setError('');
    try {
      const payload = {
        ...form,
        creditLimit: Number(form.creditLimit),
        currentOutstanding: Number(form.currentOutstanding),
      };
      if (editingId) {
        await creditCardApi.update(editingId, payload);
      } else {
        await creditCardApi.create(payload);
      }
      resetForm();
      await loadData();
    } catch (err) { setError(err?.message || 'Unable to save card.'); }
    finally { setSaving(false); }
  }

  async function remove(id) {
    if (!window.confirm('Delete this credit card?')) return;
    try { await creditCardApi.delete(id); await loadData(); }
    catch (err) { setError(err?.message || 'Unable to delete card.'); }
  }

  async function checkPurchase(e) {
    e.preventDefault(); setChecking(true); setError(''); setPurchaseResult(null);
    try {
      const result = await creditCardApi.purchaseCheck({
        amount: Number(purchaseForm.amount), category: purchaseForm.category, purpose: purchaseForm.purpose || null,
      });
      setPurchaseResult(result);
    } catch (err) { setError(err?.message || 'Unable to check purchase.'); }
    finally { setChecking(false); }
  }

  const summary = intelligence?.summary;
  const repayment = intelligence?.repaymentCapacity;

  return <div className="page-shell">
    <div className="page-header">
      <div><p className="eyebrow">PulseWallet</p><h1>Credit Card Intelligence</h1></div>
      <button type="button" className="secondary-btn" onClick={loadData}>Refresh</button>
    </div>

    {error && <div className="inline-error" role="alert">{error}</div>}

    <div className="cc-tabs">
      <button type="button" className={`cc-tab ${activeTab === 'cards' ? 'active' : ''}`} onClick={() => setActiveTab('cards')}>Cards</button>
      <button type="button" className={`cc-tab ${activeTab === 'intelligence' ? 'active' : ''}`} onClick={() => setActiveTab('intelligence')}>Intelligence</button>
      <button type="button" className={`cc-tab ${activeTab === 'purchase' ? 'active' : ''}`} onClick={() => setActiveTab('purchase')}>Purchase Check</button>
    </div>

    {activeTab === 'cards' && <>
      <div className="cc-security-notice">
        <strong>🔒 Security Notice</strong>
        <p>Never enter your full card number, CVV, PIN, password, or OTP. PulseWallet only stores safe card metadata.</p>
      </div>

      <div className="page-card cc-form-card">
        <div className="panel-header"><h2>{editingId ? 'Edit card' : 'Add a credit card'}</h2>{editingId && <button type="button" className="link-btn" onClick={resetForm}>Cancel edit</button>}</div>
        <form className="cc-form" onSubmit={submit}>
          <label>Issuer (Bank)<input required value={form.issuer} onChange={change('issuer')} placeholder="HDFC Bank" /></label>
          <label>Card Name<input required value={form.cardName} onChange={change('cardName')} placeholder="Regalia Gold" /></label>
          <label>Last 4 Digits<input required value={form.lastFourDigits} onChange={change('lastFourDigits')} placeholder="1234" maxLength={4} pattern="\d{4}" title="Exactly 4 digits" /></label>
          <label>Credit Limit<input required type="number" min="0.01" step="0.01" value={form.creditLimit} onChange={change('creditLimit')} /></label>
          <label>Current Outstanding<input required type="number" min="0" step="0.01" value={form.currentOutstanding} onChange={change('currentOutstanding')} /></label>
          <label>Statement Date<input required type="date" value={form.statementDate} onChange={change('statementDate')} /></label>
          <label>Due Date<input required type="date" value={form.dueDate} onChange={change('dueDate')} /></label>
          <label className="cc-full-row">Rewards Description<textarea value={form.rewardsDescription} onChange={change('rewardsDescription')} rows="2" placeholder="e.g. 5% cashback on selected categories" /></label>
          <label className="cc-full-row">Benefits Description<textarea value={form.benefitsDescription} onChange={change('benefitsDescription')} rows="2" placeholder="e.g. Airport lounge access" /></label>
          <button className="primary-btn" type="submit" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Update card' : 'Add card'}</button>
        </form>
      </div>

      <div className="page-card">
        <div className="panel-header"><h2>Your Cards</h2><span>{cards.length} total</span></div>
        {loading ? <div className="loader-row">Loading cards…</div> : cards.length === 0 ? <div className="empty-card">No credit cards added yet.</div> :
          <div className="cc-card-list">{cards.map((card) => {
            const intelCard = intelligence?.cards?.find((c) => c.id === card.id);
            return <article className="cc-card" key={card.id}>
              <div className="cc-card-head">
                <div>
                  <span className="eyebrow">{card.issuer}</span>
                  <h3>{card.cardName}</h3>
                  <span className="cc-digits">•••• {card.lastFourDigits}</span>
                </div>
                {intelCard && <span className={`tag ${healthBadge(intelCard.healthStatus)}`}>{intelCard.healthStatus}</span>}
              </div>
              <div className="cc-card-metrics">
                <div><span>Credit Limit</span><strong>{money(card.creditLimit, currency)}</strong></div>
                <div><span>Outstanding</span><strong>{money(card.currentOutstanding, currency)}</strong></div>
                <div><span>Available</span><strong>{money(intelCard?.availableCredit ?? (card.creditLimit - card.currentOutstanding), currency)}</strong></div>
                <div><span>Utilization</span><strong style={{ color: utilizationColor(intelCard?.utilizationLevel) }}>{intelCard ? `${Number(intelCard.utilizationPercentage).toFixed(1)}%` : '—'}</strong></div>
              </div>
              {intelCard && <div className="progress-track"><div className="progress-fill" style={{ width: `${Math.min(100, Number(intelCard.utilizationPercentage))}%`, background: Number(intelCard.utilizationPercentage) > 75 ? 'linear-gradient(90deg, #f59e0b, #ef4444)' : undefined }} /></div>}
              <div className="cc-card-dates">
                <span>Statement: {formatDate(card.statementDate)}</span>
                <span>Due: {formatDate(card.dueDate)}</span>
              </div>
              {(card.rewardsDescription || card.benefitsDescription) && <div className="cc-card-extras">
                {card.rewardsDescription && <div><span className="eyebrow">Rewards</span><p>{card.rewardsDescription}</p></div>}
                {card.benefitsDescription && <div><span className="eyebrow">Benefits</span><p>{card.benefitsDescription}</p></div>}
              </div>}
              {intelCard?.warnings?.length > 0 && <div className="cc-warnings">{intelCard.warnings.map((w, i) => <p key={i}>⚠️ {w}</p>)}</div>}
              <div className="table-actions">
                <button type="button" className="secondary-btn small" onClick={() => edit(card)}>Edit</button>
                <button type="button" className="mini-btn" onClick={() => remove(card.id)}>Delete</button>
              </div>
            </article>;
          })}</div>}
      </div>
    </>}

    {activeTab === 'intelligence' && <>
      {loading ? <div className="loader-row">Loading intelligence…</div> : !intelligence ? <div className="empty-card">Intelligence data unavailable. Add credit cards first.</div> : <>
        <div className="cc-summary-grid">
          <div className="cc-summary-item"><span>Total Credit Limit</span><strong>{money(summary?.totalCreditLimit, currency)}</strong></div>
          <div className="cc-summary-item"><span>Total Outstanding</span><strong>{money(summary?.totalOutstanding, currency)}</strong></div>
          <div className="cc-summary-item"><span>Available Credit</span><strong>{money(summary?.totalAvailableCredit, currency)}</strong></div>
          <div className="cc-summary-item"><span>Overall Utilization</span><strong style={{ color: Number(summary?.overallUtilization) > 75 ? '#dc2626' : Number(summary?.overallUtilization) > 50 ? '#b45309' : '#15803d' }}>{Number(summary?.overallUtilization ?? 0).toFixed(1)}%</strong></div>
          <div className="cc-summary-item"><span>Cards</span><strong>{summary?.numberOfCards ?? 0}</strong></div>
          <div className="cc-summary-item"><span>Needing Attention</span><strong style={{ color: (summary?.cardsNeedingAttention ?? 0) > 0 ? '#dc2626' : '#15803d' }}>{summary?.cardsNeedingAttention ?? 0}</strong></div>
          <div className="cc-summary-item"><span>Upcoming Due</span><strong>{summary?.cardsWithUpcomingDueDate ?? 0}</strong></div>
        </div>

        {repayment && <div className="page-card cc-repayment-card">
          <div className="panel-header">
            <h2>PulseWallet Repayment Capacity</h2>
            <span className={`tag ${repayment.status === 'COMFORTABLE' ? 'success' : repayment.status === 'MANAGEABLE' ? 'warning-tag' : 'danger'}`}>{repayment.status.replace('_', ' ')}</span>
          </div>
          <div className="cc-repayment-grid">
            <div><span>Monthly Left</span><strong>{money(repayment.monthlyLeft, currency)}</strong></div>
            <div><span>Available After Plan</span><strong>{money(repayment.availableAfterPlan, currency)}</strong></div>
            <div><span>Total Outstanding</span><strong>{money(repayment.totalOutstanding, currency)}</strong></div>
          </div>
          {repayment.explanation?.length > 0 && <div className="cc-explanation">
            <p className="eyebrow">How this was calculated</p>
            {repayment.explanation.map((line, i) => <p key={i} className="muted-copy">{line}</p>)}
          </div>}
          <p className="cc-disclaimer">This is PulseWallet's internal planning assessment, not an official credit-bureau or banking classification.</p>
        </div>}

        {intelligence.cards?.length > 0 && <div className="page-card">
          <div className="panel-header"><h2>Individual Card Health</h2></div>
          <div className="cc-health-list">{intelligence.cards.map((card) => <div className="cc-health-item" key={card.id}>
            <div className="cc-health-head">
              <div><strong>{card.cardName}</strong><span className="cc-digits">•••• {card.lastFourDigits}</span><span className="muted-copy">{card.issuer}</span></div>
              <span className={`tag ${healthBadge(card.healthStatus)}`}>{card.healthStatus}</span>
            </div>
            <div className="cc-card-metrics">
              <div><span>Limit</span><strong>{money(card.creditLimit, currency)}</strong></div>
              <div><span>Outstanding</span><strong>{money(card.currentOutstanding, currency)}</strong></div>
              <div><span>Available</span><strong>{money(card.availableCredit, currency)}</strong></div>
              <div><span>Utilization</span><strong style={{ color: utilizationColor(card.utilizationLevel) }}>{Number(card.utilizationPercentage).toFixed(1)}%</strong></div>
            </div>
            <div className="progress-track"><div className="progress-fill" style={{ width: `${Math.min(100, Number(card.utilizationPercentage))}%`, background: Number(card.utilizationPercentage) > 75 ? 'linear-gradient(90deg, #f59e0b, #ef4444)' : undefined }} /></div>
            {card.warnings?.length > 0 && <div className="cc-warnings">{card.warnings.map((w, i) => <p key={i}>⚠️ {w}</p>)}</div>}
            {card.recommendations?.length > 0 && <div className="cc-recs">{card.recommendations.map((r, i) => <p key={i}>💡 {r}</p>)}</div>}
          </div>)}</div>
        </div>}

        {(intelligence.warnings?.length > 0 || intelligence.recommendations?.length > 0) && <div className="page-card">
          <div className="panel-header"><h2>Insights</h2></div>
          {intelligence.warnings?.length > 0 && <div className="cc-insight-section">
            <h3>Warnings</h3>
            {intelligence.warnings.map((w, i) => <div key={i} className="cc-insight-item warning">⚠️ {w}</div>)}
          </div>}
          {intelligence.recommendations?.length > 0 && <div className="cc-insight-section">
            <h3>Recommendations</h3>
            {intelligence.recommendations.map((r, i) => <div key={i} className="cc-insight-item rec">💡 {r}</div>)}
          </div>}
        </div>}
      </>}
    </>}

    {activeTab === 'purchase' && <>
      <div className="page-card cc-form-card">
        <div className="panel-header"><h2>Purchase Readiness Check</h2></div>
        <p className="muted-copy" style={{ marginBottom: 16 }}>Check whether a planned purchase is financially safe given your current card balances and financial capacity. This is a safety assessment only — not a card reward comparison.</p>
        <form className="cc-purchase-form" onSubmit={checkPurchase}>
          <label>Amount<input required type="number" min="0.01" step="0.01" value={purchaseForm.amount} onChange={(e) => setPurchaseForm((c) => ({ ...c, amount: e.target.value }))} /></label>
          <label>Category<input required value={purchaseForm.category} onChange={(e) => setPurchaseForm((c) => ({ ...c, category: e.target.value }))} placeholder="e.g. TRAVEL, ELECTRONICS" /></label>
          <label>Purpose (optional)<input value={purchaseForm.purpose} onChange={(e) => setPurchaseForm((c) => ({ ...c, purpose: e.target.value }))} placeholder="e.g. Flight to Delhi" /></label>
          <button className="primary-btn" type="submit" disabled={checking}>{checking ? 'Checking…' : 'Check Purchase'}</button>
        </form>
      </div>

      {purchaseResult && <div className="page-card">
        <div className="panel-header">
          <h2>Purchase Assessment</h2>
          <span className={`tag ${safetyBadge(purchaseResult.financialSafetyStatus)}`}>{purchaseResult.financialSafetyStatus?.replace('_', ' ')}</span>
        </div>
        <div className="cc-purchase-overview">
          <div><span>Amount</span><strong>{money(purchaseResult.amount, currency)}</strong></div>
          <div><span>Category</span><strong>{purchaseResult.category}</strong></div>
          {purchaseResult.purpose && <div><span>Purpose</span><strong>{purchaseResult.purpose}</strong></div>}
          <div><span>Monthly Left</span><strong>{money(purchaseResult.currentMonthlyLeft, currency)}</strong></div>
          <div><span>Available After Plan</span><strong>{money(purchaseResult.availableAfterPlan, currency)}</strong></div>
        </div>

        {purchaseResult.cardAssessments?.length > 0 && <div className="cc-assessment-list">
          <h3>Per-Card Assessment</h3>
          {purchaseResult.cardAssessments.map((ca) => <div key={ca.cardId} className="cc-assessment-item">
            <div className="cc-assessment-head">
              <div><strong>{ca.cardName}</strong><span className="cc-digits">•••• {ca.lastFourDigits}</span></div>
              <span className={`tag ${safetyBadge(ca.safetyStatus)}`}>{ca.safetyStatus?.replace('_', ' ')}</span>
            </div>
            <div className="cc-card-metrics">
              <div><span>Available</span><strong>{money(ca.availableCredit, currency)}</strong></div>
              <div><span>Projected Outstanding</span><strong>{money(ca.projectedOutstanding, currency)}</strong></div>
              <div><span>Projected Utilization</span><strong>{Number(ca.projectedUtilization).toFixed(1)}%</strong></div>
            </div>
            {ca.reasons?.map((r, i) => <p key={i} className="muted-copy">{r}</p>)}
          </div>)}
        </div>}

        {purchaseResult.warnings?.length > 0 && <div className="cc-insight-section">
          <h3>Warnings</h3>
          {purchaseResult.warnings.map((w, i) => <div key={i} className="cc-insight-item warning">⚠️ {w}</div>)}
        </div>}
        {purchaseResult.recommendations?.length > 0 && <div className="cc-insight-section">
          <h3>Recommendations</h3>
          {purchaseResult.recommendations.map((r, i) => <div key={i} className="cc-insight-item rec">💡 {r}</div>)}
        </div>}
      </div>}
    </>}
  </div>;
}
