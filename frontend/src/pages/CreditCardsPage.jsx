import { useEffect, useState } from 'react';
import { creditCardApi } from '../lib/api';
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency';
import { PageNavHeader, PageNavFooter, TabStepNav } from '../components/PageNavigation.jsx';

const CREDIT_CARDS_TABS = [
  { id: 'cards', label: 'Cards & Management' },
  { id: 'statement', label: 'Statement PDF Import' },
  { id: 'intelligence', label: 'Financial Intelligence' },
  { id: 'purchase', label: 'Purchase & Rewards' },
  { id: 'banksync', label: 'Bank Sync Readiness' },
];

const EMPTY_FORM = {
  issuer: '',
  cardName: '',
  cardNetwork: 'VISA',
  annualFee: '',
  lastFourDigits: '',
  creditLimit: '',
  currentOutstanding: '0',
  statementBalance: '',
  minimumDue: '',
  statementDate: '',
  dueDate: '',
  rewardsDescription: '',
  benefitsDescription: '',
};

const PURCHASE_EMPTY = { amount: '', category: '', purpose: '' };

function money(value, currency) {
  return formatMoneyAmount(value, currency);
}

function utilizationColor(level) {
  switch (level) {
    case 'LOW':
      return '#15803d';
    case 'MODERATE':
      return '#b45309';
    case 'HIGH':
      return '#dc2626';
    case 'VERY_HIGH':
      return '#991b1b';
    default:
      return '#475569';
  }
}

function healthBadge(status) {
  switch (status) {
    case 'HEALTHY':
      return 'success';
    case 'WATCH':
      return 'warning-tag';
    case 'WARNING':
    case 'CRITICAL':
      return 'danger';
    default:
      return '';
  }
}

function safetyBadge(status) {
  switch (status) {
    case 'SAFE':
      return 'success';
    case 'CAUTION':
      return 'warning-tag';
    case 'NOT_RECOMMENDED':
      return 'danger';
    default:
      return '';
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

  // Card Identification state
  const [identCardNumber, setIdentCardNumber] = useState('');
  const [identLoading, setIdentLoading] = useState(false);
  const [identResult, setIdentResult] = useState(null);
  const [identError, setIdentError] = useState('');

  // Statement Upload & Draft state
  const [statementFile, setStatementFile] = useState(null);
  const [statementCardId, setStatementCardId] = useState('');
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementDraft, setStatementDraft] = useState(null);
  const [statementConfirming, setStatementConfirming] = useState(false);
  const [statementMessage, setStatementMessage] = useState('');
  const [statementError, setStatementError] = useState('');

  // Benefits Modal state
  const [selectedBenefits, setSelectedBenefits] = useState(null);
  const [benefitsLoading, setBenefitsLoading] = useState(false);
  const [benefitsError, setBenefitsError] = useState('');

  // Bank Sync Readiness state
  const [bankSyncInfo, setBankSyncInfo] = useState(null);
  const [bankSyncLoading, setBankSyncLoading] = useState(false);
  const [consentResult, setConsentResult] = useState(null);
  const [consentLoading, setConsentLoading] = useState(false);

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [cardData, intelData] = await Promise.allSettled([
        creditCardApi.list(),
        creditCardApi.intelligence(),
      ]);
      setCards(cardData.status === 'fulfilled' ? cardData.value : []);
      setIntelligence(intelData.status === 'fulfilled' ? intelData.value : null);
    } catch (err) {
      setError(err?.message || 'Unable to load credit card data.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  function change(field) {
    return (e) => setForm((c) => ({ ...c, [field]: e.target.value }));
  }

  function edit(card) {
    setEditingId(card.id);
    setForm({
      issuer: card.issuer || '',
      cardName: card.cardName || '',
      cardNetwork: card.cardNetwork || 'VISA',
      annualFee: card.annualFee != null ? card.annualFee : '',
      lastFourDigits: card.lastFourDigits || '',
      creditLimit: card.creditLimit != null ? card.creditLimit : '',
      currentOutstanding: card.currentOutstanding != null ? card.currentOutstanding : '0',
      statementBalance: card.statementBalance != null ? card.statementBalance : '',
      minimumDue: card.minimumDue != null ? card.minimumDue : '',
      statementDate: card.statementDate || '',
      dueDate: card.dueDate || '',
      rewardsDescription: card.rewardsDescription || '',
      benefitsDescription: card.benefitsDescription || '',
    });
    setActiveTab('cards');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = {
        ...form,
        annualFee: form.annualFee !== '' ? Number(form.annualFee) : null,
        creditLimit: Number(form.creditLimit),
        currentOutstanding: Number(form.currentOutstanding),
        statementBalance: form.statementBalance !== '' ? Number(form.statementBalance) : null,
        minimumDue: form.minimumDue !== '' ? Number(form.minimumDue) : null,
      };
      if (editingId) {
        await creditCardApi.update(editingId, payload);
      } else {
        await creditCardApi.create(payload);
      }
      resetForm();
      await loadData();
    } catch (err) {
      setError(err?.message || 'Unable to save card.');
    } finally {
      setSaving(false);
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this credit card?')) return;
    try {
      await creditCardApi.delete(id);
      await loadData();
    } catch (err) {
      setError(err?.message || 'Unable to delete card.');
    }
  }

  // Identify card
  async function handleIdentify(e) {
    e.preventDefault();
    setIdentLoading(true);
    setIdentError('');
    setIdentResult(null);
    try {
      const result = await creditCardApi.identify({ cardNumber: identCardNumber });
      setIdentResult(result);
    } catch (err) {
      setIdentError(err?.message || 'Card identification failed.');
    } finally {
      setIdentLoading(false);
    }
  }

  function applyIdentResult() {
    if (!identResult) return;
    const digitsOnly = identCardNumber.replace(/\D/g, '');
    const last4 = digitsOnly.length >= 4 ? digitsOnly.slice(-4) : '';
    setForm((f) => ({
      ...f,
      issuer: identResult.issuer !== 'Unknown Issuer' ? identResult.issuer : f.issuer,
      cardNetwork: identResult.network !== 'UNKNOWN' ? identResult.network : f.cardNetwork,
      cardName: identResult.possibleProductVariant || f.cardName,
      lastFourDigits: last4 || f.lastFourDigits,
    }));
  }

  // Statement PDF Parse
  async function handleParseStatement(e) {
    e.preventDefault();
    if (!statementFile) {
      setStatementError('Please select a PDF statement file.');
      return;
    }
    setStatementLoading(true);
    setStatementError('');
    setStatementMessage('');
    setStatementDraft(null);

    const formData = new FormData();
    formData.append('file', statementFile);

    try {
      const draft = await creditCardApi.parseStatement(formData);
      setStatementDraft(draft);
      if (draft.requiresOcr) {
        setStatementError('Scanned PDF detected. Text extraction requires OCR. Please verify and fill draft values manually.');
      }
    } catch (err) {
      setStatementError(err?.message || 'Failed to parse statement PDF.');
    } finally {
      setStatementLoading(false);
    }
  }

  // Statement Draft Confirm
  async function handleConfirmDraft(e) {
    e.preventDefault();
    if (!statementDraft) return;
    setStatementConfirming(true);
    setStatementError('');
    try {
      const last4 = statementDraft.maskedCardIdentifier
        ? statementDraft.maskedCardIdentifier.replace(/\D/g, '').slice(-4)
        : '0000';
      const payload = {
        cardId: statementCardId ? Number(statementCardId) : null,
        issuer: statementDraft.issuer || 'Unknown Issuer',
        cardName: statementDraft.cardName || 'Card Statement',
        cardNetwork: statementDraft.cardNetwork || 'VISA',
        lastFourDigits: last4 || '0000',
        creditLimit: statementDraft.creditLimit ? Number(statementDraft.creditLimit) : 0,
        currentOutstanding: statementDraft.currentOutstanding ? Number(statementDraft.currentOutstanding) : 0,
        statementBalance: statementDraft.statementBalance ? Number(statementDraft.statementBalance) : null,
        minimumDue: statementDraft.minimumDue ? Number(statementDraft.minimumDue) : null,
        statementDate: statementDraft.statementDate || null,
        dueDate: statementDraft.dueDate || null,
        confirmedTransactions: statementDraft.extractedTransactions || [],
      };
      await creditCardApi.confirmStatement(payload);
      setStatementMessage('Statement successfully confirmed and saved to your account!');
      setStatementDraft(null);
      setStatementFile(null);
      await loadData();
    } catch (err) {
      setStatementError(err?.message || 'Failed to confirm statement.');
    } finally {
      setStatementConfirming(false);
    }
  }

  // View Benefits Modal
  async function handleViewBenefits(card) {
    setSelectedBenefits({ cardName: card.cardName, issuer: card.issuer });
    setBenefitsLoading(true);
    setBenefitsError('');
    try {
      const benefits = await creditCardApi.benefits(card.id);
      setSelectedBenefits(benefits);
    } catch (err) {
      setBenefitsError(err?.message || 'Unable to load card benefits.');
    } finally {
      setBenefitsLoading(false);
    }
  }

  // Load Bank Sync Status
  async function loadBankSyncStatus() {
    setBankSyncLoading(true);
    try {
      const status = await creditCardApi.bankSyncStatus();
      setBankSyncInfo(status);
    } catch {
      // Graceful fallback
    } finally {
      setBankSyncLoading(false);
    }
  }

  // Test Bank Consent sandbox
  async function handleTestConsent() {
    setConsentLoading(true);
    try {
      const res = await creditCardApi.bankSyncConsent({
        consentHandle: `consent-${Date.now()}`,
        redirectUrl: window.location.href,
      });
      setConsentResult(res);
    } catch (err) {
      setError(err?.message || 'Consent simulation failed.');
    } finally {
      setConsentLoading(false);
    }
  }

  async function checkPurchase(e) {
    e.preventDefault();
    setChecking(true);
    setError('');
    setPurchaseResult(null);
    try {
      const result = await creditCardApi.purchaseCheck({
        amount: Number(purchaseForm.amount),
        category: purchaseForm.category,
        purpose: purchaseForm.purpose || null,
      });
      setPurchaseResult(result);
    } catch (err) {
      setError(err?.message || 'Unable to check purchase.');
    } finally {
      setChecking(false);
    }
  }

  const summary = intelligence?.summary;
  const repayment = intelligence?.repaymentCapacity;

  return (
    <div className="page-shell">
      <PageNavHeader title="Credit Cards" />
      <div className="page-header">
        <div>
          <p className="eyebrow">PulseWallet</p>
          <h1>Smart Credit Card Intelligence</h1>
        </div>
        <button type="button" className="secondary-btn" onClick={loadData}>
          Refresh
        </button>
      </div>

      {error && <div className="inline-error" role="alert">{error}</div>}

      <div className="cc-tabs">
        <button
          type="button"
          className={`cc-tab ${activeTab === 'cards' ? 'active' : ''}`}
          onClick={() => setActiveTab('cards')}
        >
          Cards &amp; Management
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'statement' ? 'active' : ''}`}
          onClick={() => setActiveTab('statement')}
        >
          Statement PDF Import
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'intelligence' ? 'active' : ''}`}
          onClick={() => setActiveTab('intelligence')}
        >
          Financial Intelligence
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'purchase' ? 'active' : ''}`}
          onClick={() => setActiveTab('purchase')}
        >
          Purchase &amp; Rewards
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'banksync' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('banksync');
            loadBankSyncStatus();
          }}
        >
          Bank Sync Readiness
        </button>
      </div>

      {/* TAB 1: CARDS & MANAGEMENT */}
      {activeTab === 'cards' && (
        <>
          <div className="cc-security-notice">
            <strong>🔒 Security &amp; Privacy Notice</strong>
            <p>
              Never enter your full card number, CVV, PIN, password, or banking OTP.
              PulseWallet only stores safe card metadata, masked digits, and statement summaries.
            </p>
          </div>

          {/* Quick Card Identification Tool */}
          <div className="cc-ident-card">
            <h3>Card Network &amp; Issuer Detection (BIN Lookup)</h3>
            <p className="muted-copy" style={{ marginBottom: 12 }}>
              Enter a card number or 6–8 digit prefix to instantly identify the card network and issuer.
              Full card numbers are never saved or sent to external servers.
            </p>
            <form onSubmit={handleIdentify} className="cc-ident-row">
              <input
                type="text"
                placeholder="Enter card number or first 8 digits (e.g. 4111 2222 3333 4444)"
                value={identCardNumber}
                onChange={(e) => setIdentCardNumber(e.target.value)}
                maxLength={23}
              />
              <button className="secondary-btn" type="submit" disabled={identLoading || !identCardNumber.trim()}>
                {identLoading ? 'Identifying…' : 'Identify Card'}
              </button>
            </form>

            {identError && <p className="inline-error" style={{ marginTop: 10 }}>{identError}</p>}

            {identResult && (
              <div className="cc-ident-result">
                <div className="cc-ident-meta">
                  <strong>
                    {identResult.issuer} &bull; {identResult.network}
                  </strong>
                  <span>{identResult.possibleProductVariant || 'Standard Credit Card'}</span>
                  <div className="cc-ident-badges">
                    <span className="tag success">Confidence: {identResult.confidence}</span>
                    <span className="tag tag-source">Source: {identResult.source}</span>
                    {identResult.providerAvailable ? (
                      <span className="tag success">Provider Online</span>
                    ) : (
                      <span className="tag warning-tag">Provider Offline / Unconfigured</span>
                    )}
                  </div>
                </div>
                <button type="button" className="primary-btn" onClick={applyIdentResult}>
                  Apply to Add Card Form &darr;
                </button>
              </div>
            )}
          </div>

          {/* Add / Edit Card Form */}
          <div className="page-card cc-form-card">
            <div className="panel-header">
              <h2>{editingId ? 'Edit card' : 'Add a credit card'}</h2>
              {editingId && (
                <button type="button" className="link-btn" onClick={resetForm}>
                  Cancel edit
                </button>
              )}
            </div>
            <form className="cc-form" onSubmit={submit}>
              <label>
                Issuer (Bank)
                <input required value={form.issuer} onChange={change('issuer')} placeholder="HDFC Bank" />
              </label>
              <label>
                Card Name / Product
                <input required value={form.cardName} onChange={change('cardName')} placeholder="Regalia Gold" />
              </label>
              <label>
                Card Network
                <select value={form.cardNetwork} onChange={change('cardNetwork')}>
                  <option value="VISA">Visa</option>
                  <option value="MASTERCARD">Mastercard</option>
                  <option value="RUPAY">RuPay</option>
                  <option value="AMEX">American Express</option>
                  <option value="DISCOVER">Discover</option>
                  <option value="DINERS_CLUB">Diners Club</option>
                  <option value="UNKNOWN">Other / Unknown</option>
                </select>
              </label>
              <label>
                Annual Fee (if any)
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.annualFee}
                  onChange={change('annualFee')}
                  placeholder="2500.00"
                />
              </label>
              <label>
                Last 4 Digits
                <input
                  required
                  value={form.lastFourDigits}
                  onChange={change('lastFourDigits')}
                  placeholder="1234"
                  maxLength={4}
                  pattern="\d{4}"
                  title="Exactly 4 digits"
                />
              </label>
              <label>
                Credit Limit
                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={form.creditLimit}
                  onChange={change('creditLimit')}
                />
              </label>
              <label>
                Current Outstanding
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.currentOutstanding}
                  onChange={change('currentOutstanding')}
                />
              </label>
              <label>
                Statement Balance
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.statementBalance}
                  onChange={change('statementBalance')}
                  placeholder="Optional"
                />
              </label>
              <label>
                Minimum Amount Due
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.minimumDue}
                  onChange={change('minimumDue')}
                  placeholder="Optional"
                />
              </label>
              <label>
                Statement Date
                <input required type="date" value={form.statementDate} onChange={change('statementDate')} />
              </label>
              <label>
                Payment Due Date
                <input required type="date" value={form.dueDate} onChange={change('dueDate')} />
              </label>
              <label className="cc-full-row">
                Rewards Description
                <textarea
                  value={form.rewardsDescription}
                  onChange={change('rewardsDescription')}
                  rows="2"
                  placeholder="e.g. 4 reward points per Rs 150 spent, 5X on dining"
                />
              </label>
              <label className="cc-full-row">
                Benefits Description
                <textarea
                  value={form.benefitsDescription}
                  onChange={change('benefitsDescription')}
                  rows="2"
                  placeholder="e.g. 12 complimentary domestic airport lounge visits/year"
                />
              </label>
              <button className="primary-btn" type="submit" disabled={saving}>
                {saving ? 'Saving…' : editingId ? 'Update card' : 'Add card'}
              </button>
            </form>
          </div>

          {/* User's Cards List */}
          <div className="page-card">
            <div className="panel-header">
              <h2>Your Cards</h2>
              <span>{cards.length} total</span>
            </div>
            {loading ? (
              <div className="loader-row">Loading cards…</div>
            ) : cards.length === 0 ? (
              <div className="empty-card">No credit cards added yet.</div>
            ) : (
              <div className="cc-card-list">
                {cards.map((card) => {
                  const intelCard = intelligence?.cards?.find((c) => c.id === card.id);
                  const isStale = card.freshnessStatus === 'STALE';
                  return (
                    <article className="cc-card" key={card.id}>
                      <div className="cc-card-head">
                        <div>
                          <span className="eyebrow">{card.issuer} &bull; {card.cardNetwork || 'CREDIT'}</span>
                          <h3>{card.cardName}</h3>
                          <span className="cc-digits">&bull;&bull;&bull;&bull; {card.lastFourDigits}</span>
                        </div>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                          <span className={`tag ${isStale ? 'tag-stale' : 'tag-fresh'}`}>
                            {card.freshnessStatus || 'FRESH'}
                          </span>
                          <span className="tag tag-source">
                            {card.dataSource || 'MANUAL'}
                          </span>
                          {intelCard && (
                            <span className={`tag ${healthBadge(intelCard.healthStatus)}`}>
                              {intelCard.healthStatus}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="cc-card-metrics">
                        <div>
                          <span>Credit Limit</span>
                          <strong>{money(card.creditLimit, currency)}</strong>
                        </div>
                        <div>
                          <span>Outstanding</span>
                          <strong>{money(card.currentOutstanding, currency)}</strong>
                        </div>
                        <div>
                          <span>Available</span>
                          <strong>
                            {money(
                              card.availableCredit != null
                                ? card.availableCredit
                                : intelCard?.availableCredit ?? (card.creditLimit - card.currentOutstanding),
                              currency,
                            )}
                          </strong>
                        </div>
                        <div>
                          <span>Utilization</span>
                          <strong
                            style={{
                              color: utilizationColor(
                                intelCard?.utilizationLevel ||
                                  (card.creditUtilization > 75 ? 'HIGH' : 'LOW'),
                              ),
                            }}
                          >
                            {card.creditUtilization != null
                              ? `${Number(card.creditUtilization).toFixed(1)}%`
                              : intelCard
                                ? `${Number(intelCard.utilizationPercentage).toFixed(1)}%`
                                : '—'}
                          </strong>
                        </div>
                      </div>

                      <div className="progress-track">
                        <div
                          className="progress-fill"
                          style={{
                            width: `${Math.min(
                              100,
                              Number(card.creditUtilization ?? intelCard?.utilizationPercentage ?? 0),
                            )}%`,
                            background:
                              Number(card.creditUtilization ?? intelCard?.utilizationPercentage ?? 0) > 75
                                ? 'linear-gradient(90deg, #f59e0b, #ef4444)'
                                : undefined,
                          }}
                        />
                      </div>

                      <div className="cc-card-dates">
                        <span>Statement: {formatDate(card.statementDate)}</span>
                        <span>Due: {formatDate(card.dueDate)}</span>
                        {card.statementBalance != null && (
                          <span>Stmt Bal: {money(card.statementBalance, currency)}</span>
                        )}
                        {card.minimumDue != null && (
                          <span>Min Due: {money(card.minimumDue, currency)}</span>
                        )}
                      </div>

                      <div style={{ fontSize: '0.75rem', color: '#64748b', marginBottom: 8 }}>
                        <span>
                          Last sync: {card.lastStatementSyncAt ? new Date(card.lastStatementSyncAt).toLocaleDateString() : 'Manual entry'}
                        </span>
                        {card.annualFee != null && (
                          <span style={{ marginLeft: 14 }}>Annual Fee: {money(card.annualFee, currency)}</span>
                        )}
                      </div>

                      {(card.rewardsDescription || card.benefitsDescription) && (
                        <div className="cc-card-extras">
                          {card.rewardsDescription && (
                            <div>
                              <span className="eyebrow">Rewards</span>
                              <p>{card.rewardsDescription}</p>
                            </div>
                          )}
                          {card.benefitsDescription && (
                            <div>
                              <span className="eyebrow">Benefits</span>
                              <p>{card.benefitsDescription}</p>
                            </div>
                          )}
                        </div>
                      )}

                      {intelCard?.warnings?.length > 0 && (
                        <div className="cc-warnings">
                          {intelCard.warnings.map((w, i) => (
                            <p key={i}>⚠️ {w}</p>
                          ))}
                        </div>
                      )}

                      <div className="table-actions" style={{ marginTop: 12 }}>
                        <button
                          type="button"
                          className="secondary-btn small"
                          onClick={() => handleViewBenefits(card)}
                        >
                          View Benefits
                        </button>
                        <button
                          type="button"
                          className="secondary-btn small"
                          onClick={() => edit(card)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="mini-btn"
                          onClick={() => remove(card.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </div>
          <TabStepNav tabs={CREDIT_CARDS_TABS} activeTab={activeTab} onTabChange={(tab) => { setActiveTab(tab); if (tab === 'banksync') loadBankSyncStatus(); }} />
        </>
      )}

      {/* TAB 2: STATEMENT IMPORT */}
      {activeTab === 'statement' && (
        <div className="cc-statement-card">
          <div className="panel-header">
            <h2>Credit Card Statement PDF Import</h2>
          </div>
          <p className="muted-copy">
            Upload your monthly credit card e-statement in PDF format. PulseWallet will parse the document in-memory,
            extract balances and transactions, and present a draft for your verification.
            <strong>No data is saved until you review and confirm the draft.</strong>
          </p>

          <div className="cc-security-notice" style={{ marginTop: 14 }}>
            <strong>🔒 Document Safety Guarantees</strong>
            <p>
              Statements are processed completely in-memory and discarded immediately after extraction.
              Original PDF files are never stored on disk. Password-protected PDFs are rejected.
            </p>
          </div>

          {statementMessage && <div className="tag success" style={{ padding: 10, marginBottom: 12 }}>{statementMessage}</div>}
          {statementError && <div className="inline-error" style={{ marginBottom: 12 }}>{statementError}</div>}

          <form onSubmit={handleParseStatement}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 14 }}>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 6, fontWeight: 600 }}>
                Link to Card (optional)
                <select
                  value={statementCardId}
                  onChange={(e) => setStatementCardId(e.target.value)}
                  style={{ padding: 10, borderRadius: 8, border: '1px solid #cbd5e1' }}
                >
                  <option value="">Create new card from statement</option>
                  {cards.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.issuer} - {c.cardName} (•••• {c.lastFourDigits})
                    </option>
                  ))}
                </select>
              </label>

              <label style={{ display: 'flex', flexDirection: 'column', gap: 6, fontWeight: 600 }}>
                Statement PDF File (Max 10MB)
                <input
                  type="file"
                  accept="application/pdf"
                  required
                  onChange={(e) => setStatementFile(e.target.files?.[0] || null)}
                  style={{ padding: 8, borderRadius: 8, border: '1px solid #cbd5e1', background: '#fff' }}
                />
              </label>
            </div>

            <button className="primary-btn" type="submit" disabled={statementLoading || !statementFile}>
              {statementLoading ? 'Parsing PDF in Memory…' : 'Extract Statement Fields'}
            </button>
          </form>

          {/* Extracted Draft Review */}
          {statementDraft && (
            <div className="cc-draft-preview">
              <div className="cc-draft-header">
                <div>
                  <h3>Draft Extraction Review</h3>
                  <p className="muted-copy">
                    Review and edit any extracted values below before confirming persistence.
                  </p>
                </div>
                <span className="tag warning-tag">Draft &bull; Pending Confirmation</span>
              </div>

              <form onSubmit={handleConfirmDraft}>
                <div className="cc-draft-grid">
                  <div className="cc-draft-field">
                    <label>Detected Issuer</label>
                    <input
                      type="text"
                      value={statementDraft.issuer || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, issuer: e.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Card Identifier</label>
                    <input
                      type="text"
                      value={statementDraft.maskedCardIdentifier || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, maskedCardIdentifier: e.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Billing Period</label>
                    <input
                      type="text"
                      value={statementDraft.billingPeriod || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, billingPeriod: e.target.value })
                      }
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Statement Date</label>
                    <input
                      type="date"
                      value={statementDraft.statementDate || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, statementDate: e.target.value })
                      }
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Payment Due Date</label>
                    <input
                      type="date"
                      value={statementDraft.dueDate || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, dueDate: e.target.value })
                      }
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Credit Limit</label>
                    <input
                      type="number"
                      step="0.01"
                      value={statementDraft.creditLimit || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, creditLimit: e.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Current Outstanding</label>
                    <input
                      type="number"
                      step="0.01"
                      value={statementDraft.currentOutstanding || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, currentOutstanding: e.target.value })
                      }
                      required
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Statement Balance</label>
                    <input
                      type="number"
                      step="0.01"
                      value={statementDraft.statementBalance || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, statementBalance: e.target.value })
                      }
                    />
                  </div>
                  <div className="cc-draft-field">
                    <label>Minimum Due</label>
                    <input
                      type="number"
                      step="0.01"
                      value={statementDraft.minimumDue || ''}
                      onChange={(e) =>
                        setStatementDraft({ ...statementDraft, minimumDue: e.target.value })
                      }
                    />
                  </div>
                </div>

                {statementDraft.rewardSummary && (
                  <div style={{ margin: '12px 0', padding: 10, background: '#f8fafc', borderRadius: 8 }}>
                    <strong>Extracted Rewards:</strong> {statementDraft.rewardSummary}
                  </div>
                )}

                <h4>Extracted Transactions ({statementDraft.extractedTransactions?.length || 0})</h4>
                {statementDraft.extractedTransactions?.length > 0 ? (
                  <div className="cc-table-wrapper">
                    <table className="cc-table">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Description</th>
                          <th>Amount</th>
                        </tr>
                      </thead>
                      <tbody>
                        {statementDraft.extractedTransactions.map((tx, idx) => (
                          <tr key={idx}>
                            <td>{tx.transactionDate}</td>
                            <td>{tx.description}</td>
                            <td>{money(tx.amount, currency)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="muted-copy">No itemized transactions detected in parsed statement.</p>
                )}

                <div style={{ marginTop: 20, display: 'flex', gap: 12 }}>
                  <button className="primary-btn" type="submit" disabled={statementConfirming}>
                    {statementConfirming ? 'Saving Confirmed Data…' : 'Confirm & Save Statement'}
                  </button>
                  <button
                    className="secondary-btn"
                    type="button"
                    onClick={() => setStatementDraft(null)}
                  >
                    Discard Draft
                  </button>
                </div>
              </form>
            </div>
          )}
          <TabStepNav tabs={CREDIT_CARDS_TABS} activeTab={activeTab} onTabChange={(tab) => { setActiveTab(tab); if (tab === 'banksync') loadBankSyncStatus(); }} />
        </div>
      )}

      {/* TAB 3: FINANCIAL INTELLIGENCE */}
      {activeTab === 'intelligence' && (
        <>
          {loading ? (
            <div className="loader-row">Loading intelligence…</div>
          ) : !intelligence ? (
            <div className="empty-card">Intelligence data unavailable. Add credit cards first.</div>
          ) : (
            <>
              <div className="cc-summary-grid">
                <div className="cc-summary-item">
                  <span>Total Credit Limit</span>
                  <strong>{money(summary?.totalCreditLimit, currency)}</strong>
                </div>
                <div className="cc-summary-item">
                  <span>Total Outstanding</span>
                  <strong>{money(summary?.totalOutstanding, currency)}</strong>
                </div>
                <div className="cc-summary-item">
                  <span>Available Credit</span>
                  <strong>{money(summary?.totalAvailableCredit, currency)}</strong>
                </div>
                <div className="cc-summary-item">
                  <span>Overall Utilization</span>
                  <strong
                    style={{
                      color:
                        Number(summary?.overallUtilization) > 75
                          ? '#dc2626'
                          : Number(summary?.overallUtilization) > 50
                            ? '#b45309'
                            : '#15803d',
                    }}
                  >
                    {Number(summary?.overallUtilization ?? 0).toFixed(1)}%
                  </strong>
                </div>
                <div className="cc-summary-item">
                  <span>Cards</span>
                  <strong>{summary?.numberOfCards ?? 0}</strong>
                </div>
                <div className="cc-summary-item">
                  <span>Needing Attention</span>
                  <strong
                    style={{
                      color: (summary?.cardsNeedingAttention ?? 0) > 0 ? '#dc2626' : '#15803d',
                    }}
                  >
                    {summary?.cardsNeedingAttention ?? 0}
                  </strong>
                </div>
                <div className="cc-summary-item">
                  <span>Upcoming Due</span>
                  <strong>{summary?.cardsWithUpcomingDueDate ?? 0}</strong>
                </div>
              </div>

              {repayment && (
                <div className="page-card cc-repayment-card">
                  <div className="panel-header">
                    <h2>PulseWallet Repayment Capacity</h2>
                    <span
                      className={`tag ${
                        repayment.status === 'COMFORTABLE'
                          ? 'success'
                          : repayment.status === 'MANAGEABLE'
                            ? 'warning-tag'
                            : 'danger'
                      }`}
                    >
                      {repayment.status?.replace('_', ' ')}
                    </span>
                  </div>
                  <div className="cc-repayment-grid">
                    <div>
                      <span>Monthly Left</span>
                      <strong>{money(repayment.monthlyLeft, currency)}</strong>
                    </div>
                    <div>
                      <span>Available After Plan</span>
                      <strong>{money(repayment.availableAfterPlan, currency)}</strong>
                    </div>
                    <div>
                      <span>Total Outstanding</span>
                      <strong>{money(repayment.totalOutstanding, currency)}</strong>
                    </div>
                  </div>
                  {repayment.explanation?.length > 0 && (
                    <div className="cc-explanation">
                      <p className="eyebrow">How this was calculated</p>
                      {repayment.explanation.map((line, i) => (
                        <p key={i} className="muted-copy">
                          {line}
                        </p>
                      ))}
                    </div>
                  )}
                  <p className="cc-disclaimer">
                    This is PulseWallet's internal planning assessment, not an official credit-bureau or banking classification.
                  </p>
                </div>
              )}

              {intelligence.cards?.length > 0 && (
                <div className="page-card">
                  <div className="panel-header">
                    <h2>Individual Card Health</h2>
                  </div>
                  <div className="cc-health-list">
                    {intelligence.cards.map((card) => (
                      <div className="cc-health-item" key={card.id}>
                        <div className="cc-health-head">
                          <div>
                            <strong>{card.cardName}</strong>
                            <span className="cc-digits">&bull;&bull;&bull;&bull; {card.lastFourDigits}</span>
                            <span className="muted-copy" style={{ marginLeft: 8 }}>
                              {card.issuer}
                            </span>
                          </div>
                          <span className={`tag ${healthBadge(card.healthStatus)}`}>
                            {card.healthStatus}
                          </span>
                        </div>
                        <div className="cc-card-metrics">
                          <div>
                            <span>Limit</span>
                            <strong>{money(card.creditLimit, currency)}</strong>
                          </div>
                          <div>
                            <span>Outstanding</span>
                            <strong>{money(card.currentOutstanding, currency)}</strong>
                          </div>
                          <div>
                            <span>Available</span>
                            <strong>{money(card.availableCredit, currency)}</strong>
                          </div>
                          <div>
                            <span>Utilization</span>
                            <strong style={{ color: utilizationColor(card.utilizationLevel) }}>
                              {Number(card.utilizationPercentage).toFixed(1)}%
                            </strong>
                          </div>
                        </div>
                        <div className="progress-track">
                          <div
                            className="progress-fill"
                            style={{
                              width: `${Math.min(100, Number(card.utilizationPercentage))}%`,
                              background:
                                Number(card.utilizationPercentage) > 75
                                  ? 'linear-gradient(90deg, #f59e0b, #ef4444)'
                                  : undefined,
                            }}
                          />
                        </div>
                        {card.warnings?.length > 0 && (
                          <div className="cc-warnings">
                            {card.warnings.map((w, i) => (
                              <p key={i}>⚠️ {w}</p>
                            ))}
                          </div>
                        )}
                        {card.recommendations?.length > 0 && (
                          <div className="cc-recs">
                            {card.recommendations.map((r, i) => (
                              <p key={i}>💡 {r}</p>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {(intelligence.warnings?.length > 0 || intelligence.recommendations?.length > 0) && (
                <div className="page-card">
                  <div className="panel-header">
                    <h2>Insights &amp; Advisories</h2>
                  </div>
                  {intelligence.warnings?.length > 0 && (
                    <div className="cc-insight-section">
                      <h3>Warnings</h3>
                      {intelligence.warnings.map((w, i) => (
                        <div key={i} className="cc-insight-item warning">
                          ⚠️ {w}
                        </div>
                      ))}
                    </div>
                  )}
                  {intelligence.recommendations?.length > 0 && (
                    <div className="cc-insight-section">
                      <h3>Recommendations</h3>
                      {intelligence.recommendations.map((r, i) => (
                        <div key={i} className="cc-insight-item rec">
                          💡 {r}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
          <TabStepNav tabs={CREDIT_CARDS_TABS} activeTab={activeTab} onTabChange={(tab) => { setActiveTab(tab); if (tab === 'banksync') loadBankSyncStatus(); }} />
        </>
      )}

      {/* TAB 4: PURCHASE & REWARDS */}
      {activeTab === 'purchase' && (
        <>
          <div className="page-card cc-form-card">
            <div className="panel-header">
              <h2>Purchase Readiness &amp; Rewards Engine</h2>
            </div>
            <p className="muted-copy" style={{ marginBottom: 16 }}>
              Check whether a planned purchase is safe for your credit limit, and estimate eligible reward points and cashback across your cards.
            </p>
            <form className="cc-purchase-form" onSubmit={checkPurchase}>
              <label>
                Amount
                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={purchaseForm.amount}
                  onChange={(e) => setPurchaseForm((c) => ({ ...c, amount: e.target.value }))}
                />
              </label>
              <label>
                Category
                <input
                  required
                  value={purchaseForm.category}
                  onChange={(e) => setPurchaseForm((c) => ({ ...c, category: e.target.value }))}
                  placeholder="e.g. TRAVEL, DINING, ELECTRONICS"
                />
              </label>
              <label>
                Purpose (optional)
                <input
                  value={purchaseForm.purpose}
                  onChange={(e) => setPurchaseForm((c) => ({ ...c, purpose: e.target.value }))}
                  placeholder="e.g. Flight booking"
                />
              </label>
              <button className="primary-btn" type="submit" disabled={checking}>
                {checking ? 'Checking…' : 'Check Purchase & Rewards'}
              </button>
            </form>
          </div>

          {purchaseResult && (
            <div className="page-card">
              <div className="panel-header">
                <h2>Purchase Assessment</h2>
                <span className={`tag ${safetyBadge(purchaseResult.financialSafetyStatus)}`}>
                  {purchaseResult.financialSafetyStatus?.replace('_', ' ')}
                </span>
              </div>
              <div className="cc-purchase-overview">
                <div>
                  <span>Amount</span>
                  <strong>{money(purchaseResult.amount, currency)}</strong>
                </div>
                <div>
                  <span>Category</span>
                  <strong>{purchaseResult.category}</strong>
                </div>
                {purchaseResult.purpose && (
                  <div>
                    <span>Purpose</span>
                    <strong>{purchaseResult.purpose}</strong>
                  </div>
                )}
                <div>
                  <span>Monthly Left</span>
                  <strong>{money(purchaseResult.currentMonthlyLeft, currency)}</strong>
                </div>
                <div>
                  <span>Available After Plan</span>
                  <strong>{money(purchaseResult.availableAfterPlan, currency)}</strong>
                </div>
              </div>

              {purchaseResult.cardAssessments?.length > 0 && (
                <div className="cc-assessment-list">
                  <h3>Per-Card Assessment &amp; Reward Estimates</h3>
                  {purchaseResult.cardAssessments.map((ca) => (
                    <div key={ca.cardId} className="cc-assessment-item">
                      <div className="cc-assessment-head">
                        <div>
                          <strong>{ca.cardName}</strong>
                          <span className="cc-digits">&bull;&bull;&bull;&bull; {ca.lastFourDigits}</span>
                        </div>
                        <span className={`tag ${safetyBadge(ca.safetyStatus)}`}>
                          {ca.safetyStatus?.replace('_', ' ')}
                        </span>
                      </div>
                      <div className="cc-card-metrics">
                        <div>
                          <span>Available</span>
                          <strong>{money(ca.availableCredit, currency)}</strong>
                        </div>
                        <div>
                          <span>Projected Outstanding</span>
                          <strong>{money(ca.projectedOutstanding, currency)}</strong>
                        </div>
                        <div>
                          <span>Projected Utilization</span>
                          <strong>{Number(ca.projectedUtilization).toFixed(1)}%</strong>
                        </div>
                        {ca.estimatedReward && (
                          <div>
                            <span>Estimated Reward</span>
                            <strong style={{ color: '#0369a1' }}>
                              {ca.estimatedReward.rewardPointsEstimated > 0
                                ? `${ca.estimatedReward.rewardPointsEstimated} pts`
                                : money(ca.estimatedReward.cashbackAmountEstimated, currency)}
                              <span style={{ fontSize: '0.68rem', display: 'block', color: '#64748b' }}>
                                ({ca.estimatedReward.calculationBasis})
                              </span>
                            </strong>
                          </div>
                        )}
                      </div>
                      {ca.reasons?.map((r, i) => (
                        <p key={i} className="muted-copy">
                          {r}
                        </p>
                      ))}
                    </div>
                  ))}
                </div>
              )}

              {purchaseResult.warnings?.length > 0 && (
                <div className="cc-insight-section">
                  <h3>Warnings</h3>
                  {purchaseResult.warnings.map((w, i) => (
                    <div key={i} className="cc-insight-item warning">
                      ⚠️ {w}
                    </div>
                  ))}
                </div>
              )}
              {purchaseResult.recommendations?.length > 0 && (
                <div className="cc-insight-section">
                  <h3>Recommendations</h3>
                  {purchaseResult.recommendations.map((r, i) => (
                    <div key={i} className="cc-insight-item rec">
                      💡 {r}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
          <TabStepNav tabs={CREDIT_CARDS_TABS} activeTab={activeTab} onTabChange={(tab) => { setActiveTab(tab); if (tab === 'banksync') loadBankSyncStatus(); }} />
        </>
      )}

      {/* TAB 5: BANK SYNC READINESS */}
      {activeTab === 'banksync' && (
        <div className="cc-bank-sync-box">
          <div className="panel-header">
            <h2>Authorized Bank-Data Integration Readiness (RBI Account Aggregator)</h2>
            <span className="tag tag-source">Framework Ready &bull; Sandbox</span>
          </div>

          {bankSyncLoading ? (
            <div className="loader-row">Loading bank sync status…</div>
          ) : (
            <>
              <p className="muted-copy">
                PulseWallet architected its bank data pipeline around the Reserve Bank of India (RBI)
                Account Aggregator (AA) framework. This ensures user-directed, consent-based financial data
                synchronization with zero credential scraping or credential storage.
              </p>

              <div className="cc-benefit-metric-grid" style={{ margin: '18px 0' }}>
                <div className="cc-benefit-metric">
                  <span>Framework Compliance</span>
                  <strong>{bankSyncInfo?.securityCompliance || 'RBI AA (FIP/FIU)'}</strong>
                </div>
                <div className="cc-benefit-metric">
                  <span>Authentication Protocol</span>
                  <strong>TLS 1.3 &bull; Digital Signature</strong>
                </div>
                <div className="cc-benefit-metric">
                  <span>Credential Policy</span>
                  <strong>Zero Storage (No CVV/PIN/OTP)</strong>
                </div>
              </div>

              <h3>Supported Financial Information Providers (FIPs)</h3>
              <ul className="cc-checklist">
                {bankSyncInfo?.supportedBanks?.map((bank, i) => (
                  <li key={i}>
                    <span style={{ color: '#15803d' }}>&check;</span> {bank}
                  </li>
                )) || (
                  <>
                    <li><span style={{ color: '#15803d' }}>&check;</span> HDFC Bank (Cards &amp; Accounts)</li>
                    <li><span style={{ color: '#15803d' }}>&check;</span> State Bank of India (SBI Cards)</li>
                    <li><span style={{ color: '#15803d' }}>&check;</span> ICICI Bank (Cards &amp; Accounts)</li>
                    <li><span style={{ color: '#15803d' }}>&check;</span> Axis Bank (Credit Cards)</li>
                    <li><span style={{ color: '#15803d' }}>&check;</span> Kotak Mahindra Bank</li>
                  </>
                )}
              </ul>

              <div style={{ marginTop: 20 }}>
                <h3>Consent Simulation</h3>
                <p className="muted-copy" style={{ marginBottom: 10 }}>
                  Simulate creating a temporary user consent artifact via the Account Aggregator interface.
                </p>
                <button
                  className="secondary-btn"
                  type="button"
                  onClick={handleTestConsent}
                  disabled={consentLoading}
                >
                  {consentLoading ? 'Simulating Consent Request…' : 'Initiate Consent Sandbox Request'}
                </button>

                {consentResult && (
                  <div style={{ marginTop: 14, padding: 12, background: '#f8fafc', borderRadius: 8 }}>
                    <strong>Consent Artifact Created:</strong>
                    <div>Consent Handle: <code>{consentResult.consentHandle}</code></div>
                    <div>Status: <span className="tag success">{consentResult.status}</span></div>
                    <div>Redirect URL: <code>{consentResult.redirectUrl}</code></div>
                    <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: 4 }}>
                      Expires: {new Date(consentResult.expiresAt).toLocaleString()}
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      )}

      {/* Benefits Modal Dialog */}
      {selectedBenefits && (
        <div className="cc-modal-overlay" onClick={() => setSelectedBenefits(null)}>
          <div className="cc-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="panel-header">
              <div>
                <h2>{selectedBenefits.cardName} Benefits</h2>
                <span className="eyebrow">{selectedBenefits.issuer} &bull; {selectedBenefits.cardNetwork}</span>
              </div>
              <button
                type="button"
                className="mini-btn"
                onClick={() => setSelectedBenefits(null)}
              >
                &times;
              </button>
            </div>

            {benefitsLoading ? (
              <div className="loader-row">Loading card benefits…</div>
            ) : benefitsError ? (
              <div className="inline-error">{benefitsError}</div>
            ) : (
              <div>
                <div className="cc-benefit-metric-grid">
                  <div className="cc-benefit-metric">
                    <span>Published Annual Fee</span>
                    <strong>{money(selectedBenefits.annualFee, currency)}</strong>
                  </div>
                  <div className="cc-benefit-metric">
                    <span>Base Cashback Rate</span>
                    <strong>{selectedBenefits.cashbackRate ? `${selectedBenefits.cashbackRate}%` : 'N/A'}</strong>
                  </div>
                  <div className="cc-benefit-metric">
                    <span>Reward Points</span>
                    <strong>{selectedBenefits.rewardPointsPerSpend ? `${selectedBenefits.rewardPointsPerSpend} pts / Rs 150` : 'N/A'}</strong>
                  </div>
                </div>

                <div style={{ margin: '14px 0' }}>
                  <strong>Point Redemption Value:</strong> Rs {selectedBenefits.redemptionValuePerPoint || '0.25'} per point
                </div>

                {selectedBenefits.spendingExclusions?.length > 0 && (
                  <div style={{ margin: '14px 0' }}>
                    <strong>Spending Exclusions:</strong>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 6 }}>
                      {selectedBenefits.spendingExclusions.map((ex, i) => (
                        <span key={i} className="tag danger">{ex}</span>
                      ))}
                    </div>
                  </div>
                )}

                {selectedBenefits.eligibilityCriteria && (
                  <div style={{ margin: '14px 0' }}>
                    <strong>Eligibility:</strong> {selectedBenefits.eligibilityCriteria}
                  </div>
                )}

                <div style={{ margin: '14px 0', fontSize: '0.8rem', color: '#64748b' }}>
                  <span>Data Source: <strong>{selectedBenefits.dataSource}</strong></span>
                </div>
              </div>
            )}
            <TabStepNav tabs={CREDIT_CARDS_TABS} activeTab={activeTab} onTabChange={(tab) => { setActiveTab(tab); if (tab === 'banksync') loadBankSyncStatus(); }} />
          </div>
        </div>
      )}
      <PageNavFooter />
    </div>
  );
}
