import { useEffect, useState } from 'react';
import { linkedPaymentAccountApi } from '../lib/api';
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency';
import { PageNavHeader, PageNavFooter } from '../components/PageNavigation.jsx';

const INDIAN_BANK_PRESETS = [
    { name: 'HDFC Bank', code: 'HDFC', color: '#004c8f' },
    { name: 'State Bank of India', code: 'SBIN', color: '#1f4788' },
    { name: 'ICICI Bank', code: 'ICIC', color: '#b83a1b' },
    { name: 'Axis Bank', code: 'UTIB', color: '#97144d' },
    { name: 'Kotak Mahindra Bank', code: 'KKBK', color: '#ed1c24' },
    { name: 'Punjab National Bank', code: 'PUNB', color: '#a21d22' },
    { name: 'Bank of Baroda', code: 'BARB', color: '#f26522' },
];

const ACCOUNT_SUBTYPES = [
    'Savings Account',
    'Salary Account',
    'Current Account',
    'Fixed Deposit',
    'Recurring Deposit',
    'NRE / NRO Account',
];

const EMPTY_FORM = {
    providerName: '',
    accountType: 'BANK',
    accountName: '',
    accountSubtype: 'Savings Account',
    maskedIdentifier: '',
    ifscCode: '',
    balance: '0',
};

const ACCOUNT_TYPES = [
    ['BANK', 'Bank Account'],
    ['UPI', 'UPI Account / VPA'],
    ['PAYMENT_APP', 'Payment App Wallet'],
];

function accountTypeLabel(value) {
    return ACCOUNT_TYPES.find(([type]) => type === value)?.[1] || value;
}

function formatDate(value) {
    if (!value) return 'Never';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'Never' : new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export default function LinkedPaymentAccountsPage() {
    const selectedCurrency = useSelectedCurrency();
    const [accounts, setAccounts] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [showForm, setShowForm] = useState(false);
    const [editingBalanceAccount, setEditingBalanceAccount] = useState(null);
    const [newBalanceInput, setNewBalanceInput] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [updatingBalance, setUpdatingBalance] = useState(false);
    const [error, setError] = useState('');
    const [successMsg, setSuccessMsg] = useState('');

    async function loadAccounts() {
        setLoading(true);
        setError('');
        try {
            setAccounts(await linkedPaymentAccountApi.list());
        } catch (err) {
            setError(err?.message || 'Unable to load linked accounts.');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { loadAccounts(); }, []);

    function change(field) {
        return (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
    }

    function selectPreset(preset) {
        setForm((current) => ({
            ...current,
            providerName: preset.name,
            accountName: current.accountName || `${preset.name} Primary`,
            ifscCode: current.ifscCode || `${preset.code}0000001`,
        }));
    }

    async function submit(event) {
        event.preventDefault();
        setSaving(true);
        setError('');
        setSuccessMsg('');
        try {
            let masked = form.maskedIdentifier.trim();
            if (!masked.startsWith('••••') && !masked.startsWith('****') && masked.length <= 4) {
                masked = `•••• ${masked}`;
            }
            const payload = {
                providerName: form.providerName.trim(),
                accountType: form.accountType,
                accountName: form.accountName.trim(),
                accountSubtype: form.accountSubtype,
                maskedIdentifier: masked,
                ifscCode: form.ifscCode ? form.ifscCode.trim().toUpperCase() : null,
                balance: parseFloat(form.balance || '0'),
            };

            const created = await linkedPaymentAccountApi.create(payload);
            setAccounts((current) => [created, ...current]);
            setForm(EMPTY_FORM);
            setShowForm(false);
            setSuccessMsg(`Successfully linked ${created.providerName}!`);
            setTimeout(() => setSuccessMsg(''), 4000);
        } catch (err) {
            setError(err?.message || 'Unable to link payment account.');
        } finally {
            setSaving(false);
        }
    }

    async function handleUpdateBalance(event) {
        event.preventDefault();
        if (!editingBalanceAccount) return;
        setUpdatingBalance(true);
        setError('');
        try {
            const numeric = parseFloat(newBalanceInput || '0');
            const updated = await linkedPaymentAccountApi.updateBalance(editingBalanceAccount.id, numeric);
            setAccounts((current) => current.map((acc) => acc.id === updated.id ? updated : acc));
            setEditingBalanceAccount(null);
            setSuccessMsg('Account balance updated successfully!');
            setTimeout(() => setSuccessMsg(''), 4000);
        } catch (err) {
            setError(err?.message || 'Failed to update balance.');
        } finally {
            setUpdatingBalance(false);
        }
    }

    async function disconnect(id) {
        try {
            const updated = await linkedPaymentAccountApi.disconnect(id);
            setAccounts((current) => current.map((account) => account.id === id ? updated : account));
            setSuccessMsg('Account disconnected.');
            setTimeout(() => setSuccessMsg(''), 3000);
        } catch (err) {
            setError(err?.message || 'Unable to disconnect account.');
        }
    }

    async function remove(id) {
        if (!window.confirm('Delete this linked bank account?')) return;
        try {
            await linkedPaymentAccountApi.delete(id);
            setAccounts((current) => current.filter((account) => account.id !== id));
            setSuccessMsg('Account deleted.');
            setTimeout(() => setSuccessMsg(''), 3000);
        } catch (err) {
            setError(err?.message || 'Unable to delete account.');
        }
    }

    const totalLiquidBalance = accounts
        .filter((acc) => acc.status === 'CONNECTED')
        .reduce((sum, acc) => sum + Number(acc.balance || 0), 0);

    return (
        <div className="page-shell linked-accounts-page">
            <PageNavHeader title="Linked Accounts" />
            <div className="page-header">
                <div>
                    <p className="eyebrow">Banking & Connections</p>
                    <h1>Linked Bank Accounts</h1>
                </div>
                <div className="topbar-actions">
                    <button type="button" className="secondary-btn" onClick={loadAccounts}>Refresh</button>
                    <button type="button" className="primary-btn" onClick={() => setShowForm(true)}>+ Link Bank Account</button>
                </div>
            </div>

            <div className="info-banner" role="note">
                🔒 <strong>Bank-Grade Security Notice:</strong> PulseWallet records your authorized account metadata, balance, and masked identifiers for portfolio tracking. We <em>never</em> ask for or store net-banking passwords, UPI PINs, OTPs, or CVVs.
            </div>

            {error && <div className="inline-error" role="alert">{error}</div>}
            {successMsg && <div className="inline-success" role="status" style={{ background: '#ecfdf5', color: '#065f46', padding: '0.75rem 1rem', borderRadius: '8px', marginBottom: '1rem', border: '1px solid #a7f3d0' }}>{successMsg}</div>}

            {/* Total Balance Card */}
            <div className="stats-row" style={{ marginBottom: '1.5rem' }}>
                <div className="stat-card" style={{ background: 'linear-gradient(135deg, #1e293b, #0f172a)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)' }}>
                    <span style={{ color: '#94a3b8', fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total Liquid Bank Balance</span>
                    <strong style={{ fontSize: '2rem', color: '#38bdf8', marginTop: '0.25rem' }}>{formatMoneyAmount(totalLiquidBalance, selectedCurrency)}</strong>
                    <span style={{ color: '#64748b', fontSize: '0.8rem', marginTop: '0.25rem' }}>Across {accounts.filter(a => a.status === 'CONNECTED').length} active accounts</span>
                </div>
            </div>

            <section className="page-card">
                <div className="panel-header">
                    <div>
                        <p className="eyebrow">Connected Indian Banks & Wallets</p>
                        <h2>Your Accounts</h2>
                    </div>
                    <span>{accounts.length} total</span>
                </div>

                {loading ? (
                    <div className="loader-row">Loading linked accounts…</div>
                ) : accounts.length === 0 ? (
                    <div className="empty-card" style={{ padding: '3rem 1.5rem', textAlign: 'center' }}>
                        <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🏛️</div>
                        <h3>No Bank Accounts Linked Yet</h3>
                        <p style={{ color: 'var(--text-muted, #64748b)', maxWidth: '420px', margin: '0.5rem auto 1.5rem' }}>
                            Link your Indian savings or salary accounts (e.g. HDFC, SBI, ICICI) to monitor your liquid net worth and streamline transactions.
                        </p>
                        <button type="button" className="primary-btn" onClick={() => setShowForm(true)}>+ Link Your First Bank Account</button>
                    </div>
                ) : (
                    <div className="linked-account-list" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
                        {accounts.map((account) => {
                            const isConnected = account.status === 'CONNECTED';
                            return (
                                <article
                                    className="linked-account-card"
                                    key={account.id}
                                    style={{
                                        border: '1px solid var(--border-color, #e2e8f0)',
                                        borderRadius: '12px',
                                        padding: '1.25rem',
                                        background: isConnected ? 'var(--card-bg, #ffffff)' : '#f8fafc',
                                        opacity: isConnected ? 1 : 0.75,
                                        display: 'flex',
                                        flexDirection: 'column',
                                        justifyContent: 'space-between',
                                        gap: '1rem',
                                        boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
                                    }}
                                >
                                    <div>
                                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                                <div
                                                    style={{
                                                        width: '40px',
                                                        height: '40px',
                                                        borderRadius: '10px',
                                                        background: 'linear-gradient(135deg, #0284c7, #0369a1)',
                                                        color: '#fff',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        fontSize: '1.25rem',
                                                        boxShadow: '0 2px 6px rgba(2, 132, 199, 0.3)',
                                                    }}
                                                    aria-hidden="true"
                                                >
                                                    🏛️
                                                </div>
                                                <div>
                                                    <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600 }}>{account.providerName}</h3>
                                                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-muted, #64748b)' }}>{account.accountName}</p>
                                                </div>
                                            </div>
                                            <span className={`tag ${isConnected ? 'success' : 'danger'}`}>
                                                {isConnected ? 'Active' : 'Disconnected'}
                                            </span>
                                        </div>

                                        <div style={{ background: 'var(--bg-subtle, #f1f5f9)', borderRadius: '8px', padding: '0.75rem 1rem', margin: '0.5rem 0' }}>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #64748b)', textTransform: 'uppercase' }}>Available Balance</div>
                                            <div style={{ fontSize: '1.4rem', fontWeight: 700, color: '#0f172a' }}>
                                                {formatMoneyAmount(account.balance ?? 0, selectedCurrency)}
                                            </div>
                                        </div>

                                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted, #64748b)' }}>
                                            <span style={{ background: '#e2e8f0', padding: '0.2rem 0.5rem', borderRadius: '4px', fontWeight: 500 }}>
                                                {account.maskedIdentifier}
                                            </span>
                                            {account.accountSubtype && (
                                                <span style={{ background: '#e0f2fe', color: '#0369a1', padding: '0.2rem 0.5rem', borderRadius: '4px', fontWeight: 500 }}>
                                                    {account.accountSubtype}
                                                </span>
                                            )}
                                            {account.ifscCode && (
                                                <span style={{ background: '#fef3c7', color: '#92400e', padding: '0.2rem 0.5rem', borderRadius: '4px', fontWeight: 500 }}>
                                                    IFSC: {account.ifscCode}
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    <div style={{ borderTop: '1px solid var(--border-color, #e2e8f0)', paddingTop: '0.75rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)' }}>
                                            Synced: {formatDate(account.lastSyncedAt || account.connectedAt)}
                                        </div>
                                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                                            {isConnected && (
                                                <button
                                                    type="button"
                                                    className="secondary-btn small"
                                                    onClick={() => {
                                                        setEditingBalanceAccount(account);
                                                        setNewBalanceInput(String(account.balance ?? 0));
                                                    }}
                                                >
                                                    Update Balance
                                                </button>
                                            )}
                                            {isConnected && (
                                                <button type="button" className="secondary-btn small" onClick={() => disconnect(account.id)}>
                                                    Disconnect
                                                </button>
                                            )}
                                            <button type="button" className="mini-btn" onClick={() => remove(account.id)}>
                                                Delete
                                            </button>
                                        </div>
                                    </div>
                                </article>
                            );
                        })}
                    </div>
                )}
            </section>

            {/* Link Account Modal */}
            {showForm && (
                <div className="modal-backdrop" role="presentation">
                    <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="link-account-title" style={{ maxWidth: '520px' }}>
                        <div className="panel-header">
                            <div>
                                <p className="eyebrow">Banking Connection</p>
                                <h2 id="link-account-title">Link Indian Bank Account</h2>
                            </div>
                            <button type="button" className="link-btn" onClick={() => setShowForm(false)}>Close</button>
                        </div>

                        {/* Quick Presets */}
                        <div style={{ marginBottom: '1rem' }}>
                            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted, #64748b)', marginBottom: '0.5rem', fontWeight: 500 }}>
                                Popular Indian Banks (Click to auto-fill):
                            </p>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                                {INDIAN_BANK_PRESETS.map((preset) => (
                                    <button
                                        type="button"
                                        key={preset.code}
                                        onClick={() => selectPreset(preset)}
                                        style={{
                                            border: form.providerName === preset.name ? '1px solid #0284c7' : '1px solid #cbd5e1',
                                            background: form.providerName === preset.name ? '#e0f2fe' : '#ffffff',
                                            color: form.providerName === preset.name ? '#0369a1' : '#334155',
                                            padding: '0.35rem 0.65rem',
                                            borderRadius: '6px',
                                            fontSize: '0.8rem',
                                            cursor: 'pointer',
                                            fontWeight: 500,
                                        }}
                                    >
                                        {preset.name}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <form className="goal-form" onSubmit={submit}>
                            <label>
                                Bank / Provider Name *
                                <input
                                    required
                                    value={form.providerName}
                                    onChange={change('providerName')}
                                    placeholder="e.g. HDFC Bank, SBI, ICICI"
                                />
                            </label>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                                <label>
                                    Connection Type
                                    <select value={form.accountType} onChange={change('accountType')}>
                                        {ACCOUNT_TYPES.map(([value, label]) => (
                                            <option key={value} value={value}>{label}</option>
                                        ))}
                                    </select>
                                </label>
                                <label>
                                    Account Category
                                    <select value={form.accountSubtype} onChange={change('accountSubtype')}>
                                        {ACCOUNT_SUBTYPES.map((type) => (
                                            <option key={type} value={type}>{type}</option>
                                        ))}
                                    </select>
                                </label>
                            </div>

                            <label>
                                Account Nickname *
                                <input
                                    required
                                    value={form.accountName}
                                    onChange={change('accountName')}
                                    placeholder="e.g. Primary Salary Account, Emergency Fund"
                                />
                            </label>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                                <label>
                                    Masked Account No. (Last 4 digits) *
                                    <input
                                        required
                                        value={form.maskedIdentifier}
                                        onChange={change('maskedIdentifier')}
                                        placeholder="•••• 4892 or 4892"
                                    />
                                </label>
                                <label>
                                    IFSC Code (optional)
                                    <input
                                        value={form.ifscCode}
                                        onChange={change('ifscCode')}
                                        placeholder="e.g. HDFC0001234"
                                    />
                                </label>
                            </div>

                            <label>
                                Current / Opening Balance (₹)
                                <input
                                    type="number"
                                    step="0.01"
                                    value={form.balance}
                                    onChange={change('balance')}
                                    placeholder="e.g. 50000"
                                />
                            </label>

                            <div className="modal-actions" style={{ marginTop: '1.25rem' }}>
                                <button type="button" className="secondary-btn" onClick={() => setShowForm(false)}>Cancel</button>
                                <button type="submit" className="primary-btn" disabled={saving}>
                                    {saving ? 'Linking…' : 'Link Bank Account'}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}

            {/* Update Balance Modal */}
            {editingBalanceAccount && (
                <div className="modal-backdrop" role="presentation">
                    <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="update-balance-title" style={{ maxWidth: '420px' }}>
                        <div className="panel-header">
                            <div>
                                <p className="eyebrow">{editingBalanceAccount.providerName}</p>
                                <h2 id="update-balance-title">Update Balance</h2>
                            </div>
                            <button type="button" className="link-btn" onClick={() => setEditingBalanceAccount(null)}>Close</button>
                        </div>
                        <form className="goal-form" onSubmit={handleUpdateBalance}>
                            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted, #64748b)' }}>
                                Enter current balance for <strong>{editingBalanceAccount.accountName}</strong> ({editingBalanceAccount.maskedIdentifier}):
                            </p>
                            <label>
                                New Balance ({selectedCurrency})
                                <input
                                    type="number"
                                    step="0.01"
                                    required
                                    autoFocus
                                    value={newBalanceInput}
                                    onChange={(e) => setNewBalanceInput(e.target.value)}
                                    placeholder="e.g. 75000.00"
                                />
                            </label>
                            <div className="modal-actions">
                                <button type="button" className="secondary-btn" onClick={() => setEditingBalanceAccount(null)}>Cancel</button>
                                <button type="submit" className="primary-btn" disabled={updatingBalance}>
                                    {updatingBalance ? 'Saving…' : 'Save Balance'}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}
            <PageNavFooter />
        </div>
    );
}
