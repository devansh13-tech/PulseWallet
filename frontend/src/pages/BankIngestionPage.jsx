import React, { useState } from 'react';
import { bankIngestionApi } from '../lib/api';
import { PageNavHeader, PageNavFooter, TabStepNav } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

const BANK_INGESTION_TABS = [
  { id: 'sms', label: 'UPI / SMS Statement Parser' },
  { id: 'aa', label: 'RBI Account Aggregator' },
  { id: 'learning', label: 'ML Auto-Categorization' },
];

const INDIAN_BANKS = [
  { name: 'HDFC Bank', code: 'HDFC', logo: '🏛️' },
  { name: 'State Bank of India', code: 'SBIN', logo: '🇮🇳' },
  { name: 'ICICI Bank', code: 'ICIC', logo: '🏢' },
  { name: 'Axis Bank', code: 'UTIB', logo: '💳' },
  { name: 'Kotak Mahindra Bank', code: 'KKBK', logo: '🏦' },
  { name: 'Punjab National Bank', code: 'PUNB', logo: '🏧' },
];

const SAMPLE_SMS = [
  {
    title: 'HDFC Swiggy UPI Debit',
    text: 'HDFC Bank: Rs 450.00 debited from A/C **1092 on 10-OCT-26 at SWIGGY via UPI Ref 428910291029. Avl Bal: Rs 42,150.00',
  },
  {
    title: 'SBI Salary Credit',
    text: 'SBI: Dear Customer, Your A/c **8812 Credited by Rs 82,500.00 on 01-OCT-26 by SALARY TECH CORP. Bal Rs 1,24,500.00',
  },
  {
    title: 'Axis Amazon Credit Card',
    text: 'Axis Bank: Alert! Rs 3,499.00 spent on your Credit Card ending 9021 at AMAZON INDIA on 09-OCT-26. Avl Limit: Rs 1,45,000.00',
  },
  {
    title: 'Indian Oil Fuel UPI',
    text: 'A/c **4102 debited by Rs 1,200.00 on 08-OCT-26 at INDIAN OIL PETROL UPI Ref 881920192. Bal: Rs 38,900.00',
  },
];

export default function BankIngestionPage() {
  const currency = useSelectedCurrency();
  const [activeTab, setActiveTab] = useState('sms'); // sms, aa, learning
  const [smsInput, setSmsInput] = useState(SAMPLE_SMS[0].text);
  const [parsedTxn, setParsedTxn] = useState(null);
  const [parsing, setParsing] = useState(false);
  const [importing, setImporting] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Account Aggregator state
  const [selectedBank, setSelectedBank] = useState(INDIAN_BANKS[0]);
  const [aaVpa, setAaVpa] = useState('user@finvu');
  const [mobileNumber, setMobileNumber] = useState('9876543210');
  const [aaStep, setAaStep] = useState('idle'); // idle, otp, success
  const [otpInput, setOtpInput] = useState('123456');
  const [aaData, setAaData] = useState(null);

  // Category learning feedback state
  const [correctionMerchant, setCorrectionMerchant] = useState('SWIGGY');
  const [correctedCat, setCorrectedCat] = useState('Food & Dining');

  async function handleParseSms() {
    if (!smsInput.trim()) return;
    setParsing(true);
    setErrorMsg('');
    try {
      const result = await bankIngestionApi.parseSms({ smsText: smsInput });
      setParsedTxn(result);
    } catch (err) {
      setErrorMsg(err?.message || 'Failed to parse SMS/UPI text.');
    } finally {
      setParsing(false);
    }
  }

  async function handleImportSingle(txnToImport) {
    const txn = txnToImport || parsedTxn;
    if (!txn) return;
    setImporting(true);
    setErrorMsg('');
    try {
      await bankIngestionApi.batchImport({ transactions: [txn] });
      setSuccessMsg(`✓ Successfully imported ₹${txn.amount} (${txn.merchant}) to transactions!`);
      setTimeout(() => setSuccessMsg(''), 4000);
    } catch (err) {
      setErrorMsg(err?.message || 'Failed to import transaction.');
    } finally {
      setImporting(false);
    }
  }

  async function handleInitiateAa() {
    setAaStep('otp');
  }

  async function handleVerifyAaOtp() {
    setParsing(true);
    setErrorMsg('');
    try {
      const response = await bankIngestionApi.accountAggregatorConsent({
        institutionId: selectedBank.name,
        accountAggregatorVpa: aaVpa,
        mobileNumber,
      });
      setAaData(response);
      setAaStep('success');
      setSuccessMsg(`✓ Consented data fetched via ${response.aggregatorProvider}! 5 verified bank entries ready.`);
    } catch (err) {
      setErrorMsg(err?.message || 'Account Aggregator request failed.');
    } finally {
      setParsing(false);
    }
  }

  async function handleImportAllAa() {
    if (!aaData?.fetchedTransactions) return;
    setImporting(true);
    try {
      await bankIngestionApi.batchImport({ transactions: aaData.fetchedTransactions });
      setSuccessMsg(`✓ Imported all ${aaData.fetchedTransactions.length} bank transactions to your ledger!`);
    } catch (err) {
      setErrorMsg(err?.message || 'Failed to import batch.');
    } finally {
      setImporting(false);
    }
  }

  async function handleSubmitFeedback(e) {
    e.preventDefault();
    try {
      await bankIngestionApi.categoryFeedback({
        merchant: correctionMerchant,
        correctedCategory: correctedCat,
      });
      setSuccessMsg(`✓ Learning model updated: "${correctionMerchant}" mapped to "${correctedCat}".`);
      setTimeout(() => setSuccessMsg(''), 4000);
    } catch (err) {
      setErrorMsg(err?.message || 'Failed to submit feedback.');
    }
  }

  return (
    <div className="page-shell">
      <PageNavHeader title="Bank & Transaction Ingestion" />

      <div className="page-header">
        <div>
          <p className="eyebrow">RBI Consented AA & NLP Ingestion</p>
          <h1>Real Bank & Transaction Ingestion</h1>
        </div>
      </div>

      {successMsg && (
        <div style={{ background: '#ecfdf5', color: '#065f46', padding: '0.85rem 1.1rem', borderRadius: '10px', marginBottom: '1.25rem', border: '1px solid #a7f3d0', fontWeight: 600 }}>
          {successMsg}
        </div>
      )}

      {errorMsg && <div className="inline-error" role="alert">{errorMsg}</div>}

      {/* Tabs */}
      <div className="cc-tabs" style={{ marginBottom: '1.5rem' }}>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'sms' ? 'active' : ''}`}
          onClick={() => setActiveTab('sms')}
        >
          📱 UPI / SMS Statement Parser
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'aa' ? 'active' : ''}`}
          onClick={() => setActiveTab('aa')}
        >
          🏛️ Account Aggregator (Setu / Finvu)
        </button>
        <button
          type="button"
          className={`cc-tab ${activeTab === 'learning' ? 'active' : ''}`}
          onClick={() => setActiveTab('learning')}
        >
          🧠 ML Auto-Categorization & Learning
        </button>
      </div>

      {/* TAB 1: SMS / UPI Parser */}
      {activeTab === 'sms' && (
        <div className="page-card" style={{ marginBottom: '1.5rem' }}>
          <div className="panel-header">
            <div>
              <p className="eyebrow">Instant Extraction</p>
              <h2>Paste Indian Bank SMS or UPI Notification</h2>
            </div>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600, display: 'block', marginBottom: '0.4rem' }}>
              Sample SMS Templates (Click to test):
            </span>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.45rem' }}>
              {SAMPLE_SMS.map((sample, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => {
                    setSmsInput(sample.text);
                    setParsedTxn(null);
                  }}
                  style={{
                    background: smsInput === sample.text ? '#e0f2fe' : '#f8fafc',
                    color: smsInput === sample.text ? '#0369a1' : '#334155',
                    border: smsInput === sample.text ? '1px solid #0284c7' : '1px solid #e2e8f0',
                    padding: '0.35rem 0.65rem',
                    borderRadius: '6px',
                    fontSize: '0.78rem',
                    cursor: 'pointer',
                    fontWeight: 500,
                  }}
                >
                  {sample.title}
                </button>
              ))}
            </div>
          </div>

          <textarea
            rows="3"
            value={smsInput}
            onChange={(e) => setSmsInput(e.target.value)}
            placeholder="Paste your bank debit/credit SMS or UPI alert text here..."
            style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.88rem', fontFamily: 'inherit', marginBottom: '1rem' }}
          />

          <button
            type="button"
            className="primary-btn"
            onClick={handleParseSms}
            disabled={parsing}
          >
            {parsing ? 'Parsing with NLP…' : '🔍 Parse Transaction'}
          </button>

          {/* Parsed Result Card */}
          {parsedTxn && (
            <div style={{ marginTop: '1.5rem', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
                  ✓ Structured Transaction Extracted
                </span>
                <span className={`tag ${parsedTxn.type === 'INCOME' ? 'success' : 'warning'}`}>
                  {parsedTxn.type}
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1.25rem' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>AMOUNT</span>
                  <div style={{ fontSize: '1.4rem', fontWeight: 700, color: parsedTxn.type === 'INCOME' ? '#15803d' : '#b91c1c' }}>
                    {formatMoneyAmount(parsedTxn.amount, currency)}
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>MERCHANT / PAYEE</span>
                  <strong style={{ display: 'block', fontSize: '1rem', color: '#0f172a' }}>{parsedTxn.merchant}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>CATEGORY (AI SUGGESTED)</span>
                  <strong style={{ display: 'block', fontSize: '0.95rem', color: '#2563eb' }}>{parsedTxn.suggestedCategory}</strong>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>ACCOUNT / CHANNEL</span>
                  <strong style={{ display: 'block', fontSize: '0.95rem', color: '#475569' }}>{parsedTxn.accountMasked} ({parsedTxn.paymentChannel})</strong>
                </div>
                {parsedTxn.balance != null && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>UPDATED BALANCE</span>
                    <strong style={{ display: 'block', fontSize: '1rem', color: '#0f172a' }}>{formatMoneyAmount(parsedTxn.balance, currency)}</strong>
                  </div>
                )}
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>REFERENCE / UPI ID</span>
                  <span style={{ display: 'block', fontSize: '0.85rem', color: '#64748b', fontFamily: 'monospace' }}>{parsedTxn.referenceNumber}</span>
                </div>
              </div>

              <button
                type="button"
                className="primary-btn"
                onClick={() => handleImportSingle(parsedTxn)}
                disabled={importing}
              >
                {importing ? 'Importing…' : '📥 Save to My Transactions Ledger'}
              </button>
            </div>
          )}
          <TabStepNav tabs={BANK_INGESTION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      {/* TAB 2: Account Aggregator Flow */}
      {activeTab === 'aa' && (
        <div className="page-card" style={{ marginBottom: '1.5rem' }}>
          <div className="panel-header">
            <div>
              <p className="eyebrow">RBI Account Aggregator Standard</p>
              <h2>Consented Bank Connection (Setu / Finvu / Anumati)</h2>
            </div>
          </div>

          <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', padding: '0.85rem 1rem', borderRadius: '10px', marginBottom: '1.25rem', fontSize: '0.85rem', color: '#0369a1' }}>
            🔒 <strong>100% RBI Regulated:</strong> Data access is end-to-end encrypted and read-only. PulseWallet cannot initiate debits or view net banking credentials.
          </div>

          {aaStep === 'idle' && (
            <div>
              <label style={{ display: 'block', marginBottom: '1rem', fontWeight: 600, fontSize: '0.9rem' }}>
                Select Your Indian Bank:
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '0.65rem', marginTop: '0.5rem' }}>
                  {INDIAN_BANKS.map((bank) => (
                    <button
                      key={bank.code}
                      type="button"
                      onClick={() => setSelectedBank(bank)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                        padding: '0.65rem 0.85rem',
                        borderRadius: '8px',
                        border: selectedBank.code === bank.code ? '2px solid #2563eb' : '1px solid #cbd5e1',
                        background: selectedBank.code === bank.code ? '#eff6ff' : '#ffffff',
                        cursor: 'pointer',
                        textAlign: 'left',
                        fontWeight: 600,
                        fontSize: '0.85rem',
                      }}
                    >
                      <span style={{ fontSize: '1.2rem' }}>{bank.logo}</span>
                      <span>{bank.name}</span>
                    </button>
                  ))}
                </div>
              </label>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                    Account Aggregator ID (VPA):
                  </label>
                  <input
                    type="text"
                    value={aaVpa}
                    onChange={(e) => setAaVpa(e.target.value)}
                    placeholder="e.g. yourname@finvu"
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                    Linked Mobile Number:
                  </label>
                  <input
                    type="text"
                    value={mobileNumber}
                    onChange={(e) => setMobileNumber(e.target.value)}
                    placeholder="10-digit mobile number"
                    style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                  />
                </div>
              </div>

              <button
                type="button"
                className="primary-btn"
                onClick={handleInitiateAa}
              >
                Request Consent & Send OTP
              </button>
            </div>
          )}

          {aaStep === 'otp' && (
            <div style={{ maxWidth: '420px', background: '#f8fafc', padding: '1.5rem', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
              <h3>Enter AA Consent OTP</h3>
              <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1rem' }}>
                A 6-digit verification code was sent to <strong>+91-{mobileNumber}</strong> for <strong>{selectedBank.name}</strong> consent.
              </p>
              <input
                type="text"
                value={otpInput}
                onChange={(e) => setOtpInput(e.target.value)}
                maxLength={6}
                style={{ width: '100%', padding: '0.75rem', fontSize: '1.3rem', letterSpacing: '0.3em', textAlign: 'center', borderRadius: '8px', border: '1px solid #cbd5e1', marginBottom: '1rem' }}
              />
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="button" className="secondary-btn" onClick={() => setAaStep('idle')}>Cancel</button>
                <button type="button" className="primary-btn" onClick={handleVerifyAaOtp} disabled={parsing}>
                  {parsing ? 'Authorizing with AA…' : 'Verify & Ingest Data'}
                </button>
              </div>
            </div>
          )}

          {aaStep === 'success' && aaData && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <div>
                  <span style={{ background: '#dcfce7', color: '#166534', padding: '0.2rem 0.6rem', borderRadius: '6px', fontSize: '0.78rem', fontWeight: 700 }}>
                    CONSENT ACTIVE: {aaData.consentId}
                  </span>
                  <p style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.25rem' }}>
                    Provider: <strong>{aaData.aggregatorProvider}</strong> • Valid until: {aaData.expiresAt}
                  </p>
                </div>
                <button
                  type="button"
                  className="primary-btn"
                  onClick={handleImportAllAa}
                  disabled={importing}
                >
                  {importing ? 'Importing…' : '📥 Import All 5 Bank Transactions'}
                </button>
              </div>

              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Merchant / Payee</th>
                      <th>Channel</th>
                      <th>Category</th>
                      <th>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {aaData.fetchedTransactions?.map((tx, idx) => (
                      <tr key={idx}>
                        <td>{tx.transactionDate}</td>
                        <td><span className={`tag ${tx.type === 'INCOME' ? 'success' : 'warning'}`}>{tx.type}</span></td>
                        <td><strong>{tx.merchant}</strong></td>
                        <td>{tx.paymentChannel}</td>
                        <td>{tx.suggestedCategory}</td>
                        <td className={tx.type === 'INCOME' ? 'amount-positive' : 'amount-negative'}>
                          {tx.type === 'INCOME' ? '+' : '-'}{formatMoneyAmount(tx.amount, currency)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
          <TabStepNav tabs={BANK_INGESTION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      {/* TAB 3: Category Learning Feedback */}
      {activeTab === 'learning' && (
        <div className="page-card" style={{ marginBottom: '1.5rem' }}>
          <div className="panel-header">
            <div>
              <p className="eyebrow">Continuous Learning</p>
              <h2>Teach Pulse AI Your Category Preferences</h2>
            </div>
          </div>
          <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1.25rem' }}>
            When you correct a merchant's category, Pulse AI updates its in-memory classifier so all future SMS, UPI, and bank feeds are categorized to your liking.
          </p>

          <form onSubmit={handleSubmitFeedback} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Merchant Name:</label>
              <input
                type="text"
                value={correctionMerchant}
                onChange={(e) => setCorrectionMerchant(e.target.value)}
                placeholder="e.g. SWIGGY, ZERODHA"
                style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
                required
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Preferred Category:</label>
              <select
                value={correctedCat}
                onChange={(e) => setCorrectedCat(e.target.value)}
                style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              >
                <option value="Food & Dining">Food & Dining</option>
                <option value="Groceries">Groceries</option>
                <option value="Shopping">Shopping</option>
                <option value="Transportation">Transportation</option>
                <option value="Fuel & Utilities">Fuel & Utilities</option>
                <option value="Entertainment">Entertainment</option>
                <option value="Healthcare">Healthcare</option>
                <option value="Investments">Investments</option>
                <option value="Income">Income</option>
                <option value="Miscellaneous">Miscellaneous</option>
              </select>
            </div>
            <div>
              <button type="submit" className="primary-btn" style={{ width: '100%' }}>
                ✓ Save Category Rule
              </button>
            </div>
          </form>
          <TabStepNav tabs={BANK_INGESTION_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
        </div>
      )}

      <PageNavFooter />
    </div>
  );
}
