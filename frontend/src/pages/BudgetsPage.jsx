import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { budgetApi, transactionApi } from '../lib/api';
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency';

function formatMoney(value, currencyCode) {
    return formatMoneyAmount(value, currencyCode);
}

function formatDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
    }).format(date);
}

export default function BudgetsPage() {
    const navigate = useNavigate();
    const selectedCurrency = useSelectedCurrency();
    const [budgets, setBudgets] = useState([]);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let mounted = true;
        async function load() {
            setLoading(true);
            setError('');
            try {
                const [budgetResult, transactionResult] = await Promise.all([
                    budgetApi.list(),
                    transactionApi.list({ page: 0, size: 100, sort: 'transactionDate,desc' }),
                ]);
                if (!mounted) return;
                setBudgets(Array.isArray(budgetResult) ? budgetResult : []);
                setTransactions(Array.isArray(transactionResult?.content) ? transactionResult.content : []);
            } catch (err) {
                if (!mounted) return;
                setError(err?.message || 'Unable to load budgets. Try again.');
            } finally {
                if (mounted) setLoading(false);
            }
        }

        load();
        return () => { mounted = false; };
    }, []);

    const budgetCards = budgets.map((budget) => {
        const start = budget.startDate ? new Date(budget.startDate) : null;
        const end = budget.endDate ? new Date(budget.endDate) : null;
        const spent = transactions.reduce((sum, transaction) => {
            if (transaction.type !== 'EXPENSE') return sum;
            const matchesCategory = !budget.categoryId || String(transaction.categoryId) === String(budget.categoryId);
            const txDate = transaction.transactionDate ? new Date(transaction.transactionDate) : null;
            const inRange = (!start || !txDate || txDate >= start) && (!end || !txDate || txDate <= end);
            return matchesCategory && inRange ? sum + Number(transaction.amount ?? 0) : sum;
        }, 0);

        const amount = Number(budget.amount ?? 0);
        const remaining = amount - spent;
        const percent = amount > 0 ? Math.min(100, Math.max(0, (spent / amount) * 100)) : 0;

        return { ...budget, spent, remaining, percent };
    });

    return (
        <div className="page-shell">
            <div className="page-header">
                <div>
                    <p className="eyebrow">PulseWallet</p>
                    <h1>Budgets</h1>
                </div>
                <button type="button" className="primary-btn" onClick={() => navigate('/transactions/new')}>Add transaction</button>
            </div>

            {error && <div className="inline-error" role="alert">{error}</div>}

            <div className="page-card">
                {loading ? (
                    <div className="loader-row">Loading budgets…</div>
                ) : budgetCards.length === 0 ? (
                    <div className="empty-card">No budgets created yet.</div>
                ) : (
                    <div className="budget-list">
                        {budgetCards.map((budget) => (
                            <div key={budget.id} className={`budget-item ${budget.remaining < 0 ? 'over' : ''}`}>
                                <div className="budget-head">
                                    <div>
                                        <strong>{budget.categoryName || 'General budget'}</strong>
                                        <span>{formatDate(budget.startDate)} to {formatDate(budget.endDate)}</span>
                                    </div>
                                    <strong className={budget.remaining < 0 ? 'overrun' : ''}>{formatMoney(budget.remaining, selectedCurrency)} left</strong>
                                </div>
                                <div className="budget-row">
                                    <span>{formatMoney(budget.spent, selectedCurrency)} spent</span>
                                    <span>{formatMoney(budget.amount, selectedCurrency)} total</span>
                                </div>
                                <div className="progress-track" aria-label={`Progress for ${budget.categoryName || 'General budget'}`}>
                                    <div className={`progress-fill ${budget.remaining < 0 ? 'over' : ''}`} style={{ width: `${Math.min(100, budget.percent)}%` }} />
                                </div>
                                <div className="budget-row muted">
                                    <span>{budget.remaining < 0 ? 'Exceeded by' : 'Remaining'} {formatMoney(Math.abs(budget.remaining), selectedCurrency)}</span>
                                    <span>{Math.round(budget.percent)}%</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
