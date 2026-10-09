import React, { useState } from 'react';
import { TabStepNav } from './PageNavigation.jsx';
import './PulseAiAssistant.css';

const COMPLIANCE_TABS = [
  { id: '2fa', label: '2FA & Biometrics' },
  { id: 'audit', label: 'Security Audit Trail' },
  { id: 'dpdp', label: 'DPDP Consents' },
];

export default function SecurityAndComplianceModal({ isOpen, onClose }) {
  const [activeTab, setActiveTab] = useState('2fa'); // 2fa, audit, dpdp
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(true);
  const [biometricsEnabled, setBiometricsEnabled] = useState(true);
  const [revokedSetuConsent, setRevokedSetuConsent] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState('');

  if (!isOpen) return null;

  function handleDownloadData() {
    setDownloadSuccess('✓ Personal Data Archive requested under DPDP Act Section 11. Download started.');
    setTimeout(() => setDownloadSuccess(''), 4000);
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section className="modal-card" role="dialog" aria-modal="true" style={{ maxWidth: '640px', maxHeight: '90vh', overflowY: 'auto' }}>
        <div className="panel-header">
          <div>
            <p className="eyebrow">Enterprise Fintech Trust</p>
            <h2>Security & DPDP Compliance Center</h2>
          </div>
          <button type="button" className="link-btn" onClick={onClose}>Close</button>
        </div>

        {downloadSuccess && (
          <div style={{ background: '#ecfdf5', color: '#065f46', padding: '0.65rem 0.9rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid #a7f3d0', fontSize: '0.84rem' }}>
            {downloadSuccess}
          </div>
        )}

        {/* Tab pills */}
        <div className="cc-tabs" style={{ marginBottom: '1.25rem' }}>
          <button
            type="button"
            className={`cc-tab ${activeTab === '2fa' ? 'active' : ''}`}
            onClick={() => setActiveTab('2fa')}
          >
            🔐 2FA & Biometrics
          </button>
          <button
            type="button"
            className={`cc-tab ${activeTab === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveTab('audit')}
          >
            📋 Security Audit Trail
          </button>
          <button
            type="button"
            className={`cc-tab ${activeTab === 'dpdp' ? 'active' : ''}`}
            onClick={() => setActiveTab('dpdp')}
          >
            🛡️ DPDP Act 2023 Consents
          </button>
        </div>

        {/* TAB 1: 2FA & Biometrics */}
        {activeTab === '2fa' && (
          <div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.85rem 1rem', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>Two-Factor Authentication (TOTP / SMS)</strong>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Required for high-value transactions (&gt; ₹25,000) and card unfreeze.</span>
                </div>
                <button
                  type="button"
                  className={twoFactorEnabled ? 'primary-btn small' : 'secondary-btn small'}
                  onClick={() => setTwoFactorEnabled(!twoFactorEnabled)}
                >
                  {twoFactorEnabled ? 'Active (ON)' : 'Disabled'}
                </button>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.85rem 1rem', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>Passkeys & Device Biometrics (FIDO2)</strong>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Authenticate using Windows Hello, Face ID, or Fingerprint sensor.</span>
                </div>
                <button
                  type="button"
                  className={biometricsEnabled ? 'primary-btn small' : 'secondary-btn small'}
                  onClick={() => setBiometricsEnabled(!biometricsEnabled)}
                >
                  {biometricsEnabled ? 'Enrolled' : 'Enable'}
                </button>
              </div>

              <div style={{ padding: '0.85rem 1rem', background: '#f0fdf4', borderRadius: '10px', border: '1px solid #bbf7d0', fontSize: '0.82rem', color: '#166534' }}>
                🔒 <strong>Hardware-Backed Cryptography:</strong> All sessions are cryptographically bound to this device using SHA-256 tokens and TLS 1.3 encryption at rest and in transit.
              </div>
            </div>
            <TabStepNav tabs={COMPLIANCE_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
          </div>
        )}

        {/* TAB 2: Audit Logs */}
        {activeTab === 'audit' && (
          <div>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Action</th>
                    <th>IP / Geolocation</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { time: 'Today, 02:41 AM', action: 'Critical Anomaly Blocked', ip: '185.220.101.44 (Cyprus)', status: 'BLOCKED' },
                    { time: 'Today, 02:29 AM', action: 'User Sign In via JWT', ip: '127.0.0.1 (Localhost)', status: 'SUCCESS' },
                    { time: 'Yesterday, 11:15 PM', action: 'UPI Collect Flagged', ip: '49.204.112.89 (Jaipur)', status: 'FLAGGED' },
                    { time: '08-Oct-26, 09:30 PM', action: 'AA Consent Approved', ip: '106.51.24.18 (Bangalore)', status: 'VERIFIED' },
                  ].map((log, idx) => (
                    <tr key={idx}>
                      <td style={{ fontSize: '0.78rem' }}>{log.time}</td>
                      <td><strong>{log.action}</strong></td>
                      <td style={{ fontSize: '0.78rem', color: '#64748b' }}>{log.ip}</td>
                      <td>
                        <span className={`tag ${log.status === 'BLOCKED' || log.status === 'FLAGGED' ? 'danger' : 'success'}`}>
                          {log.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <TabStepNav tabs={COMPLIANCE_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
          </div>
        )}

        {/* TAB 3: DPDP Act 2023 Compliance */}
        {activeTab === 'dpdp' && (
          <div>
            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '10px', border: '1px solid #e2e8f0', marginBottom: '1.25rem' }}>
              <strong style={{ fontSize: '0.92rem', color: '#0f172a' }}>Digital Personal Data Protection Act (DPDP Act 2023)</strong>
              <p style={{ fontSize: '0.82rem', color: '#64748b', margin: '0.3rem 0 0' }}>
                You have the fundamental right to access, export, or revoke consent for any financial data processed by PulseWallet at any time.
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.88rem' }}>Setu / Finvu Account Aggregator Consent</strong>
                  <span style={{ fontSize: '0.75rem', color: revokedSetuConsent ? '#dc2626' : '#15803d' }}>
                    {revokedSetuConsent ? 'REVOKED (Access Terminated)' : 'ACTIVE (Expires: 10-Oct-2027)'}
                  </span>
                </div>
                {!revokedSetuConsent && (
                  <button
                    type="button"
                    className="secondary-btn small"
                    onClick={() => setRevokedSetuConsent(true)}
                  >
                    Revoke Consent
                  </button>
                )}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.88rem' }}>Right to Data Portability (JSON / CSV Archive)</strong>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Download a complete cryptographic copy of all your financial ledgers and profiles.</span>
                </div>
                <button
                  type="button"
                  className="secondary-btn small"
                  onClick={handleDownloadData}
                >
                  Download My Data
                </button>
              </div>
            </div>
            <TabStepNav tabs={COMPLIANCE_TABS} activeTab={activeTab} onTabChange={setActiveTab} />
          </div>
        )}
      </section>
    </div>
  );
}
