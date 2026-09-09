import { useEffect, useState } from 'react'
import { financialPlanApi } from '../lib/api'
import { formatMoneyAmount, useSelectedCurrency } from '../utils/currency'

const INITIAL_PROFILE = { riskPreference: '', timeHorizonYears: '' }
const RISK_OPTIONS = ['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE']

function money(value, currency) { return formatMoneyAmount(value ?? 0, currency) }
function label(value) { return value.replaceAll('_', ' ') }

export default function FinancialPlanPage() {
    const currency = useSelectedCurrency()
    const [plan, setPlan] = useState(null)
    const [profile, setProfile] = useState(INITIAL_PROFILE)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    async function load() {
        setLoading(true); setError('')
        try {
            const [planData, profileData] = await Promise.all([financialPlanApi.get(), financialPlanApi.profile()])
            setPlan(planData)
            setProfile(profileData || INITIAL_PROFILE)
        } catch (err) { setError(err?.message || 'Unable to load the financial plan.') }
        finally { setLoading(false) }
    }

    useEffect(() => { load() }, [])

    function change(field) {
        return (event) => setProfile((current) => ({ ...current, [field]: event.target.value }))
    }

    async function save(event) {
        event.preventDefault(); setError(''); setSuccess('')
        if (!profile.riskPreference || !profile.timeHorizonYears || Number(profile.timeHorizonYears) <= 0) {
            setError('Risk preference and a positive time horizon are required.')
            return
        }
        setSaving(true)
        try {
            const saved = await financialPlanApi.save({ riskPreference: profile.riskPreference, timeHorizonYears: Number(profile.timeHorizonYears) })
            setProfile(saved)
            setPlan(await financialPlanApi.get())
            setSuccess('Investment profile saved.')
        } catch (err) { setError(err?.message || 'Unable to save the investment profile.') }
        finally { setSaving(false) }
    }

    return <div className="page-shell">
        <div className="page-header"><div><p className="eyebrow">PulseWallet</p><h1>Financial Plan</h1></div><button type="button" className="secondary-btn" onClick={load}>Refresh</button></div>
        {error && <div className="inline-error" role="alert">{error}</div>}
        {success && <div className="inline-success" role="status">{success}</div>}
        <div className="page-card plan-profile-card">
            <div className="panel-header"><div><p className="eyebrow">Investment Profile</p><h2>Set your planning preferences</h2></div></div>
            <form className="plan-form" onSubmit={save}>
                <label>Risk preference<select required value={profile.riskPreference || ''} onChange={change('riskPreference')}><option value="">Select preference</option>{RISK_OPTIONS.map((risk) => <option key={risk} value={risk}>{label(risk)}</option>)}</select></label>
                <label>Time horizon (years)<input required type="number" min="1" step="1" value={profile.timeHorizonYears || ''} onChange={change('timeHorizonYears')} /></label>
                <button className="primary-btn" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button>
            </form>
            {!profile.riskPreference && <p className="muted-copy">Configure a profile to receive an educational allocation. No investment preference is assumed.</p>}
        </div>
        {loading ? <div className="loader-row">Loading financial plan...</div> : plan && <>
            <section className="plan-metrics-grid" aria-label="Financial plan summary">
                {[['Monthly income', plan.monthlyIncome], ['Monthly expenses', plan.monthlyExpenses], ['Monthly left', plan.monthlyLeft], ['Goal savings', plan.goalSavingsRequired], ['Emergency/buffer', plan.recommendedEmergencyAllocation], ['Recommended investment', plan.recommendedInvestmentAmount], ['Flexible spending', plan.flexibleSpendingAmount]].map(([name, value]) => <article className="stat-card" key={name}><p>{name}</p><strong>{money(value, currency)}</strong><span className="trend neutral">Calculated from live data</span></article>)}
            </section>
            <div className="two-column-layout">
                <article className="page-card"><div className="panel-header"><h2>Educational allocation</h2><span className="tag">{plan.riskPreference ? label(plan.riskPreference) : 'Not configured'}</span></div>{plan.allocation.map((item) => <div className="plan-allocation-row" key={item.category}><div><strong>{item.category}</strong><span>{item.percentage}%</span></div><strong>{money(item.amount, currency)}</strong><div className="progress-track"><div className="progress-fill" style={{ width: `${item.percentage}%` }} /></div></div>)}<p className="muted-copy">Illustrative categories only. This is educational planning information, not a promise of returns or personalized regulated advice.</p></article>
                <article className="page-card"><div className="panel-header"><h2>Why this plan?</h2></div><ul className="plan-copy-list">{plan.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>{plan.warnings.length > 0 && <><h3>Warnings</h3><ul className="plan-copy-list warning-list">{plan.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></>}</article>
            </div>
        </>}
    </div>
}
