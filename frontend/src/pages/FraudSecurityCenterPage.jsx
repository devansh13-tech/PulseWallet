import React, { useState, useEffect } from 'react';
import { advancedFraudApi } from '../lib/api';
import { PageNavHeader, PageNavFooter } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

export default function FraudSecurityCenterPage() {
  const currency = useSelectedCurrency();
  const [securityData, setSecurityData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Phishing scanner state
  const [phishingInput, setPhishingInput] = useState('lottery-claim-reward@ybl');
  const [phishingResult, setPhishingResult] = useState(null);
  const [scanning, setScanning] = useState(false);

  // Report modal
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState(null);
  const [loadingReport, setLoadingReport] = useState(false);

  async function loadData() {
    setLoading(true);
    setErrorMsg('');
    try {
      const data = await advancedFraudApi.getSecurityCenter();
      setSecurityData(data);
    } catch (err) {
      setErrorMsg(err?.message || 'Failed to load fraud security data.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleTakeAction(transactionId, action) {
    try {
      const res = await advancedFraudApi.takeAction({ transactionId, action });
      setActionSuccess(`✓ ${res.message}`);
      setTimeout(() => setActionSuccess(''), 5000);
      loadData();
    } catch (err) {
      alert('Action failed: ' + (err.message || 'Error'));
    }
  }

  async function handleScanPhishing(e) {
    e.preventDefault();
    if (!phishingInput.trim()) return;
    setScanning(true);
    try {
      const result = await advancedFraudApi.scanPhishing({ inputQuery: phishingInput.trim() });
      setPhishingResult(result);
    } catch (err) {
      alert('Scanner failed: ' + err.message);
    } finally {
      setScanning(false);
    }
  }

  async function handleOpenReport() {
    setShowReportModal(true);
    setLoadingReport(true);
    try {
      const report = await advancedFraudApi.getInvestigationReport();
      setReportData(report);
    } catch (err) {
      alert('Could not generate report: ' + err.message);
    } finally {
      setLoadingReport(false);
    }
  }

  return (
    <div className="page-shell">
      <PageNavHeader title="Fraud Security Center" />

      <div className="page-header">
        <div>
          <p className="eyebrow">Real-Time Threat Radar & Anomaly Defense</p>
          <h1>Fraud Security Center & Scanner</h1>
        </div>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button type="button" className="secondary-btn" onClick={loadData}>Refresh</button>
          <button type="button" className="primary-btn" onClick={handleOpenReport}>
            📑 Generate Forensic Audit Report
          </button>
        </div>
      </div>

      {actionSuccess && (
        <div style={{ background: '#ecfdf5', color: '#065f46', padding: '0.85rem 1.1rem', borderRadius: '10px', marginBottom: '1.25rem', border: '1px solid #a7f3d0', fontWeight: 600 }}>
          {actionSuccess}
        </div>
      )}

      {errorMsg && <div className="inline-error" role="alert">{errorMsg}</div>}

      {/* Defense Posture Overview Banner */}
      {securityData && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
          <div style={{ background: 'linear-gradient(135deg, #0f172a, #1e293b)', color: '#ffffff', padding: '1.2rem', borderRadius: '14px', border: '1px solid rgba(255,255,255,0.1)' }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#94a3b8', fontWeight: 700 }}>SYSTEM SHIELD LEVEL</span>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#38bdf8', marginTop: '0.2rem' }}>
              {securityData.systemShieldLevel}%
            </div>
            <span style={{ fontSize: '0.78rem', color: '#4ade80' }}>● XGBoost & Anomaly Radar Active</span>
          </div>

          <div style={{ background: '#ffffff', padding: '1.2rem', borderRadius: '14px', border: '1px solid #e2e8f0' }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>ACTIVE ANOMALIES</span>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: securityData.activeAlertsCount > 0 ? '#dc2626' : '#15803d', marginTop: '0.2rem' }}>
              {securityData.activeAlertsCount} Flagged
            </div>
            <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Requires User Attention</span>
          </div>

          <div style={{ background: '#ffffff', padding: '1.2rem', borderRadius: '14px', border: '1px solid #e2e8f0' }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>THREATS NEUTRALIZED</span>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#16a34a', marginTop: '0.2rem' }}>
              {securityData.threatsNeutralizedCount}
            </div>
            <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Auto-Blocked Collect Requests</span>
          </div>
        </div>
      )}

      {/* Flagged Transactions with Risk Score Explanations */}
      <div className="page-card" style={{ marginBottom: '1.5rem' }}>
        <div className="panel-header">
          <div>
            <p className="eyebrow">Explainable Machine Learning</p>
            <h2>Flagged Anomalous Transactions</h2>
          </div>
        </div>

        {loading ? (
          <div className="loader-row">Analyzing real-time threat signals…</div>
        ) : securityData?.flaggedTransactions?.length === 0 ? (
          <div className="empty-card">No suspicious transactions detected. All authorizations are healthy.</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {securityData?.flaggedTransactions?.map((tx) => (
              <div
                key={tx.id}
                style={{
                  border: tx.severity === 'CRITICAL' ? '2px solid #fecaca' : '1px solid #fed7aa',
                  background: tx.severity === 'CRITICAL' ? '#fff5f5' : '#fffaf0',
                  borderRadius: '14px',
                  padding: '1.25rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '0.75rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <strong style={{ fontSize: '1.1rem', color: '#0f172a' }}>{tx.merchant}</strong>
                      <span className={`tag ${tx.severity === 'CRITICAL' ? 'danger' : 'warning'}`}>
                        {tx.severity} RISK
                      </span>
                      <span style={{ background: '#f1f5f9', color: '#475569', fontSize: '0.72rem', padding: '0.15rem 0.5rem', borderRadius: '4px', fontWeight: 600 }}>
                        STATUS: {tx.status}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                      {tx.paymentChannel} • {tx.timestamp} • Location: {tx.location} ({tx.clientIp})
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#dc2626' }}>
                      {formatMoneyAmount(tx.amount, currency)}
                    </div>
                    <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#991b1b' }}>
                      Risk Score: {tx.riskScore}/100
                    </div>
                  </div>
                </div>

                {/* Natural Language Explanations */}
                <div style={{ background: 'rgba(255, 255, 255, 0.8)', border: '1px solid rgba(0,0,0,0.06)', borderRadius: '8px', padding: '0.75rem 1rem', marginBottom: '1rem' }}>
                  <span style={{ fontSize: '0.74rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', display: 'block', marginBottom: '0.35rem' }}>
                    AI Detection Signals & Reasons:
                  </span>
                  <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.84rem', color: '#334155', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    {tx.anomalySignals?.map((sig, sIdx) => (
                      <li key={sIdx}>{sig}</li>
                    ))}
                  </ul>
                </div>

                {/* One-Tap Defense Actions */}
                <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}>
                  {tx.status !== 'FROZEN' && (
                    <button
                      type="button"
                      onClick={() => handleTakeAction(tx.id, 'FREEZE_CARD')}
                      style={{ background: '#dc2626', color: '#ffffff', border: 'none', padding: '0.5rem 1rem', borderRadius: '8px', fontWeight: 700, fontSize: '0.84rem', cursor: 'pointer' }}
                    >
                      🚨 Not Me — Freeze Card / Block UPI
                    </button>
                  )}
                  {tx.status !== 'DISPUTED' && (
                    <button
                      type="button"
                      className="secondary-btn small"
                      onClick={() => handleTakeAction(tx.id, 'DISPUTE')}
                    >
                      🛡️ File Chargeback Dispute
                    </button>
                  )}
                  {tx.status !== 'VERIFIED' && (
                    <button
                      type="button"
                      className="secondary-btn small"
                      onClick={() => handleTakeAction(tx.id, 'VERIFY')}
                    >
                      ✓ I Recognize This Spend
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Phishing & UPI Scam Awareness Scanner */}
      <div className="page-card" style={{ marginBottom: '1.5rem' }}>
        <div className="panel-header">
          <div>
            <p className="eyebrow">Scam Awareness Engine</p>
            <h2>Phishing & Malicious UPI Collect Request Scanner</h2>
          </div>
        </div>
        <p style={{ fontSize: '0.85rem', color: '#64748b', marginBottom: '1rem' }}>
          Paste any suspicious UPI VPA (e.g., <code>lottery-claim@ybl</code>), SMS link, or message snippet to verify legitimacy before paying.
        </p>

        <form onSubmit={handleScanPhishing} style={{ display: 'flex', gap: '0.6rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
          <input
            type="text"
            value={phishingInput}
            onChange={(e) => setPhishingInput(e.target.value)}
            placeholder="e.g. claim-reward@ybl or https://bit.ly/sbi-pan-update"
            style={{ flex: 1, minWidth: '240px', padding: '0.65rem 0.85rem', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.88rem' }}
          />
          <button type="submit" className="primary-btn" disabled={scanning}>
            {scanning ? 'Scanning…' : '🔍 Scan for Threat'}
          </button>
        </form>

        {phishingResult && (
          <div
            style={{
              background: phishingResult.isScam ? '#fef2f2' : '#f0fdf4',
              border: phishingResult.isScam ? '1px solid #fecaca' : '1px solid #bbf7d0',
              padding: '1.1rem',
              borderRadius: '12px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span style={{ fontSize: '1.2rem' }}>{phishingResult.isScam ? '⚠️' : '✅'}</span>
              <strong style={{ color: phishingResult.isScam ? '#b91c1c' : '#15803d', fontSize: '0.95rem' }}>
                {phishingResult.threatType} (Risk: {phishingResult.riskScore}/100)
              </strong>
            </div>
            <p style={{ fontSize: '0.85rem', color: '#1e293b', marginBottom: '0.4rem', fontWeight: 500 }}>
              {phishingResult.warningMessage}
            </p>
            <p style={{ fontSize: '0.8rem', color: '#64748b', margin: 0 }}>
              💡 <strong>Pulse AI Recommendation:</strong> {phishingResult.recommendation}
            </p>
          </div>
        )}
      </div>

      {/* Forensic Report Modal */}
      {showReportModal && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal-card" role="dialog" aria-modal="true" style={{ maxWidth: '680px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="panel-header">
              <div>
                <p className="eyebrow">RBI Zero-Liability Document</p>
                <h2>Forensic Fraud Investigation Report</h2>
              </div>
              <button type="button" className="link-btn" onClick={() => setShowReportModal(false)}>Close</button>
            </div>

            {loadingReport ? (
              <div className="loader-row">Generating forensic packet…</div>
            ) : reportData && (
              <div style={{ fontSize: '0.88rem', color: '#1e293b' }}>
                <div style={{ background: '#f8fafc', padding: '0.85rem 1rem', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '1rem', display: 'flex', justifyContent: 'space-between' }}>
                  <div>
                    <strong>Report ID:</strong> {reportData.reportId}<br />
                    <strong>Generated:</strong> {reportData.generatedAt}
                  </div>
                  <div>
                    <strong>Case Code:</strong> {reportData.disputeReferenceCode}<br />
                    <span className="tag danger">EVIDENCE READY</span>
                  </div>
                </div>

                <h3>Incident Summary & Timeline</h3>
                <p style={{ color: '#475569', fontSize: '0.85rem' }}>
                  The PulseWallet Real-Time Fraud Engine detected an anomalous outbound authorization outside user baseline spending profile with unauthorized terminal geolocation.
                </p>

                <h4 style={{ marginTop: '1rem' }}>Forensic Machine Learning Signals</h4>
                <ul>
                  {reportData.mlFeatureWeights?.map((w, idx) => (
                    <li key={idx} style={{ marginBottom: '0.2rem' }}>{w}</li>
                  ))}
                </ul>

                <h4 style={{ marginTop: '1rem' }}>Mandatory Cardholder Next Steps</h4>
                <ul>
                  {reportData.recommendedNextSteps?.map((s, idx) => (
                    <li key={idx} style={{ marginBottom: '0.2rem' }}>{s}</li>
                  ))}
                </ul>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.6rem', marginTop: '1.5rem' }}>
                  <button type="button" className="secondary-btn" onClick={() => window.print()}>
                    🖨️ Print / Save PDF
                  </button>
                  <button type="button" className="primary-btn" onClick={() => setShowReportModal(false)}>
                    Done
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      )}

      <PageNavFooter />
    </div>
  );
}
