import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { transactionApi, categoryApi } from '../lib/api';
import './transaction.css';

const EMPTY_FORM = {
  amount: '',
  type: '',
  transactionDate: new Date().toISOString().slice(0, 10),
  categoryId: '',
  description: '',
  merchant: '',
  paymentChannel: '',
};

export default function TransactionPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // Categories from backend
  const [categories, setCategories] = useState([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setCategoriesLoading(true);
    categoryApi
      .list()
      .then((data) => {
        if (!cancelled) setCategories(data || []);
      })
      .catch(() => {
        if (!cancelled) setCategories([]);
      })
      .finally(() => {
        if (!cancelled) setCategoriesLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  // Filter categories by selected transaction type
  const filteredCategories = form.type
    ? categories.filter((c) => c.type === form.type)
    : categories;

  // Reset categoryId when type changes if the current selection is no longer valid
  useEffect(() => {
    if (form.categoryId) {
      const stillValid = filteredCategories.some(
        (c) => String(c.id) === String(form.categoryId),
      );
      if (!stillValid) {
        setForm((prev) => ({ ...prev, categoryId: '' }));
      }
    }
  }, [form.type]); // eslint-disable-line react-hooks/exhaustive-deps

  function validate() {
    const next = {};

    const amount = parseFloat(form.amount);
    if (!form.amount) {
      next.amount = 'Amount is required';
    } else if (isNaN(amount) || amount < 0.01) {
      next.amount = 'Amount must be greater than 0';
    }

    if (!form.type) {
      next.type = 'Type is required';
    } else if (form.type !== 'INCOME' && form.type !== 'EXPENSE') {
      next.type = 'Type must be INCOME or EXPENSE';
    }

    if (!form.transactionDate) {
      next.transactionDate = 'Date is required';
    }

    if (form.description && form.description.length > 255) {
      next.description = 'Description must be 255 characters or fewer';
    }

    if (form.merchant && form.merchant.length > 160) {
      next.merchant = 'Merchant must be 160 characters or fewer';
    }

    if (form.paymentChannel && form.paymentChannel.length > 40) {
      next.paymentChannel = 'Payment channel must be 40 characters or fewer';
    }

    return next;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setApiError('');
    setSuccess('');

    const fieldErrors = validate();
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setLoading(true);
    try {
      const body = {
        amount: parseFloat(form.amount),
        type: form.type,
        transactionDate: form.transactionDate,
        description: form.description || null,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        merchant: form.merchant || null,
        paymentChannel: form.paymentChannel || null,
      };

      await transactionApi.create(body);
      setSuccess('Transaction created successfully.');
      setForm({ ...EMPTY_FORM });
      setErrors({});
    } catch (err) {
      if (err.fieldErrors) {
        setErrors(err.fieldErrors);
      }
      setApiError(err.message || 'Failed to create transaction');
    } finally {
      setLoading(false);
    }
  }

  function onChange(field) {
    return (e) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      if (errors[field]) setErrors((prev) => ({ ...prev, [field]: '' }));
      if (success) setSuccess('');
    };
  }

  return (
    <div className="txn-page">
      <button type="button" className="txn-back" onClick={() => navigate('/')}>
        ← Back to dashboard
      </button>

      <h2>New transaction</h2>
      <p className="subtitle">Record an income or expense</p>

      <div className="txn-card">
        {apiError && <div className="txn-error" role="alert">{apiError}</div>}
        {success && <div className="txn-success" role="status">{success}</div>}

        <form onSubmit={handleSubmit} noValidate>
          {/* Amount + Type row */}
          <div className="txn-row">
            <div className="txn-form-group">
              <label htmlFor="txn-amount">Amount</label>
              <input
                id="txn-amount"
                type="number"
                step="0.01"
                min="0.01"
                value={form.amount}
                onChange={onChange('amount')}
                className={errors.amount ? 'field-error' : ''}
                placeholder="0.00"
              />
              {errors.amount && <p className="error-text">{errors.amount}</p>}
            </div>

            <div className="txn-form-group">
              <label htmlFor="txn-type">Type</label>
              <select
                id="txn-type"
                value={form.type}
                onChange={onChange('type')}
                className={errors.type ? 'field-error' : ''}
              >
                <option value="">Select type</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </select>
              {errors.type && <p className="error-text">{errors.type}</p>}
            </div>
          </div>

          {/* Date */}
          <div className="txn-form-group">
            <label htmlFor="txn-date">Date</label>
            <input
              id="txn-date"
              type="date"
              value={form.transactionDate}
              onChange={onChange('transactionDate')}
              className={errors.transactionDate ? 'field-error' : ''}
            />
            {errors.transactionDate && (
              <p className="error-text">{errors.transactionDate}</p>
            )}
          </div>

          {/* Category */}
          <div className="txn-form-group">
            <label htmlFor="txn-category">Category (optional)</label>
            {categoriesLoading ? (
              <p className="txn-loading">Loading categories…</p>
            ) : (
              <select
                id="txn-category"
                value={form.categoryId}
                onChange={onChange('categoryId')}
              >
                <option value="">No category</option>
                {filteredCategories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            )}
            {!categoriesLoading && filteredCategories.length === 0 && form.type && (
              <p className="hint-text">
                No categories available for {form.type.toLowerCase()}.
              </p>
            )}
          </div>

          {/* Description */}
          <div className="txn-form-group">
            <label htmlFor="txn-description">Description (optional)</label>
            <input
              id="txn-description"
              type="text"
              maxLength={255}
              value={form.description}
              onChange={onChange('description')}
              className={errors.description ? 'field-error' : ''}
              placeholder="What was this for?"
            />
            {errors.description && (
              <p className="error-text">{errors.description}</p>
            )}
          </div>

          {/* Merchant */}
          <div className="txn-form-group">
            <label htmlFor="txn-merchant">Merchant (optional)</label>
            <input
              id="txn-merchant"
              type="text"
              maxLength={160}
              value={form.merchant}
              onChange={onChange('merchant')}
              className={errors.merchant ? 'field-error' : ''}
              placeholder="e.g. Amazon, Whole Foods"
            />
            {errors.merchant && (
              <p className="error-text">{errors.merchant}</p>
            )}
          </div>

          {/* Payment Channel */}
          <div className="txn-form-group">
            <label htmlFor="txn-channel">Payment channel (optional)</label>
            <input
              id="txn-channel"
              type="text"
              maxLength={40}
              value={form.paymentChannel}
              onChange={onChange('paymentChannel')}
              className={errors.paymentChannel ? 'field-error' : ''}
              placeholder="e.g. credit card, bank transfer"
            />
            {errors.paymentChannel && (
              <p className="error-text">{errors.paymentChannel}</p>
            )}
          </div>

          <button type="submit" className="txn-submit" disabled={loading}>
            {loading ? 'Creating…' : 'Create transaction'}
          </button>
        </form>
      </div>
    </div>
  );
}
