import { useEffect, useState } from 'react';
import { goalApi } from '../lib/api';
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency';

const TYPES = ['BIKE', 'CAR', 'HOUSE', 'TRIP', 'EDUCATION', 'EMERGENCY_FUND', 'CUSTOM'];
const EMPTY_FORM = { name: '', goalType: 'CUSTOM', targetAmount: '', currentAmount: '0', targetDate: '', description: '' };

function money(value, currency) { return formatMoneyAmount(value, currency); }
function label(value) { return value.replaceAll('_', ' '); }

export default function GoalsPage() {
    const currency = useSelectedCurrency();
    const [goals, setGoals] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [editingId, setEditingId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    async function loadGoals() {
        setLoading(true); setError('');
        try { setGoals(await goalApi.list()); }
        catch (err) { setError(err?.message || 'Unable to load goals. Try again.'); }
        finally { setLoading(false); }
    }

    useEffect(() => { loadGoals(); }, []);

    function change(field) {
        return (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
    }

    function edit(goal) {
        setEditingId(goal.id);
        setForm({ name: goal.name, goalType: goal.goalType, targetAmount: goal.targetAmount, currentAmount: goal.currentAmount, targetDate: goal.targetDate, description: goal.description || '' });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function resetForm() { setEditingId(null); setForm(EMPTY_FORM); }

    async function submit(event) {
        event.preventDefault(); setSaving(true); setError('');
        try {
            const payload = { ...form, targetAmount: Number(form.targetAmount), currentAmount: Number(form.currentAmount) };
            const saved = editingId ? await goalApi.update(editingId, payload) : await goalApi.create(payload);
            setGoals((current) => editingId ? current.map((goal) => goal.id === editingId ? saved : goal) : [...current, saved].sort((a, b) => a.targetDate.localeCompare(b.targetDate)));
            resetForm();
        } catch (err) { setError(err?.message || 'Unable to save goal.'); }
        finally { setSaving(false); }
    }

    async function remove(id) {
        if (!window.confirm('Delete this financial goal?')) return;
        try { await goalApi.delete(id); setGoals((current) => current.filter((goal) => goal.id !== id)); }
        catch (err) { setError(err?.message || 'Unable to delete goal.'); }
    }

    return <div className="page-shell">
        <div className="page-header"><div><p className="eyebrow">PulseWallet</p><h1>Financial Goals</h1></div><button type="button" className="secondary-btn" onClick={loadGoals}>Refresh</button></div>
        {error && <div className="inline-error" role="alert">{error}</div>}
        <div className="goal-form-card page-card">
            <div className="panel-header"><h2>{editingId ? 'Edit goal' : 'Add a goal'}</h2>{editingId && <button type="button" className="link-btn" onClick={resetForm}>Cancel edit</button>}</div>
            <form className="goal-form" onSubmit={submit}>
                <label>Goal name<input required value={form.name} onChange={change('name')} placeholder="Bike" /></label>
                <label>Goal type<select value={form.goalType} onChange={change('goalType')}>{TYPES.map((type) => <option key={type} value={type}>{label(type)}</option>)}</select></label>
                <label>Target amount<input required type="number" min="0.01" step="0.01" value={form.targetAmount} onChange={change('targetAmount')} /></label>
                <label>Current saved<input required type="number" min="0" step="0.01" value={form.currentAmount} onChange={change('currentAmount')} /></label>
                <label>Target date<input required type="date" value={form.targetDate} onChange={change('targetDate')} /></label>
                <label className="goal-description">Description<textarea value={form.description} onChange={change('description')} rows="2" /></label>
                <button className="primary-btn" type="submit" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Update goal' : 'Create goal'}</button>
            </form>
        </div>
        <div className="page-card goals-list-card">
            <div className="panel-header"><h2>Your goals</h2><span>{goals.length} total</span></div>
            {loading ? <div className="loader-row">Loading goals…</div> : goals.length === 0 ? <div className="empty-card">No goals created yet.</div> : <div className="goal-list">{goals.map((goal) => <article className="goal-card" key={goal.id}>
                <div className="goal-card-head"><div><span className="eyebrow">{label(goal.goalType)}</span><h3>{goal.name}</h3></div><span className={`tag ${goal.status === 'COMPLETED' || goal.status === 'ON_TRACK' ? 'success' : 'danger'}`}>{label(goal.status)}</span></div>
                <div className="goal-amounts"><div><span>Target</span><strong>{money(goal.targetAmount, currency)}</strong></div><div><span>Saved</span><strong>{money(goal.currentAmount, currency)}</strong></div><div><span>Remaining</span><strong>{money(goal.remainingAmount, currency)}</strong></div></div>
                <div className="progress-track"><div className="progress-fill" style={{ width: `${Math.min(100, Number(goal.progressPercentage ?? 0))}%` }} /></div>
                <div className="goal-card-meta"><span>{Number(goal.progressPercentage ?? 0).toFixed(2)}% complete</span><span>Target {goal.targetDate}</span><span>{money(goal.requiredMonthlySaving, currency)}/month</span></div>
                {goal.description && <p className="muted-copy">{goal.description}</p>}
                <div className="table-actions"><button type="button" className="secondary-btn small" onClick={() => edit(goal)}>Edit</button><button type="button" className="mini-btn" onClick={() => remove(goal.id)}>Delete</button></div>
            </article>)}</div>}
        </div>
    </div>;
}