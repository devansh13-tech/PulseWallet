import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { categoryApi, transactionApi } from '../lib/api';
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

export default function TransactionsPage() {
    const navigate = useNavigate();
    const selectedCurrency = useSelectedCurrency();
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [categories, setCategories] = useState([]);

    useEffect(() => {
        let mounted = true;
        async function load() {
            setLoading(true);
            setError('');
            try {
                const [transactionResult, categoryResult] = await Promise.all([
                    transactionApi.list({ page: 0, size: 50, sort: 'transactionDate,desc' }),
                    categoryApi.list(),
                ]);
                if (!mounted) return;
                setTransactions(Array.isArray(transactionResult?.content) ? transactionResult.content : []);
                setCategories(Array.isArray(categoryResult) ? categoryResult : []);
            } catch (err) {
                if (!mounted) return;
                setError(err?.message || 'Unable to load transactions. Try again.');
            } finally {
                if (mounted) setLoading(false);
            }
        }

        load();
        return () => { mounted = false; };
    }, []);

    const categoryMap = useMemo(
        () => Object.fromEntries(categories.map((category) => [String(category.id), category.name])),
        [categories],
    );

    async function handleDelete(transactionId) {
        try {
            await transactionApi.delete(transactionId);
            setTransactions((current) => current.filter((item) => item.id !== transactionId));
        } catch (err) {
            setError(err?.message || 'Unable to delete transaction.');
        }
    }

    return (
        <div className="page-shell">
            <div className="page-header">
                <div>
                    <p className="eyebrow">PulseWallet</p>
                    <h1>Transactions</h1>
                </div>
                <button type="button" className="primary-btn" onClick={() => navigate('/transactions/new')}>Add transaction</button>
            </div>

            {error && <div className="inline-error" role="alert">{error}</div>}

            <div className="page-card">
                {loading ? (
                    <div className="loader-row">Loading transactions…</div>
                ) : transactions.length === 0 ? (
                    <div className="empty-card">
                        <p>No transactions yet.</p>
                        <Link to="/transactions/new">Create one</Link>
                    </div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Type</th>
                                    <th>Category</th>
                                    <th>Merchant</th>
                                    <th>Description</th>
                                    <th>Amount</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.map((transaction) => (
                                    <tr key={transaction.id}>
                                        <td>{formatDate(transaction.transactionDate)}</td>
                                        <td><span className={`tag ${transaction.type === 'EXPENSE' ? 'warning' : 'success'}`}>{transaction.type}</span></td>
                                        <td>{transaction.categoryName || categoryMap[String(transaction.categoryId)] || 'Uncategorized'}</td>
                                        <td>{transaction.merchant || '—'}</td>
                                        <td>{transaction.description || '—'}</td>
                                        <td className={transaction.type === 'INCOME' ? 'amount-positive' : 'amount-negative'}>
                                            {transaction.type === 'INCOME' ? '+' : '-'}{formatMoney(transaction.amount, selectedCurrency)}
                                        </td>
                                        <td>
                                            <div className="table-actions">
                                                <button type="button" className="secondary-btn small" onClick={() => navigate('/transactions/new')}>New</button>
                                                <button type="button" className="mini-btn" onClick={() => handleDelete(transaction.id)}>Delete</button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
