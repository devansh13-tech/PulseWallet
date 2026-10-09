import React, { useState } from 'react';
import { PageNavHeader, PageNavFooter } from '../components/PageNavigation.jsx';
import { useSelectedCurrency, formatMoneyAmount } from '../utils/currency';
import './transaction.css';

const AMFI_FUNDS = [
  {
    code: '122639',
    name: 'Parag Parikh Flexi Cap Fund - Direct Growth',
    category: 'Flexi Cap',
    nav: '74.28',
    cagr1y: '+26.4%',
    cagr3y: '+19.8%',
    cagr5y: '+21.5%',
    risk: 'Very High',
    expenseRatio: '0.62%',
    aum: '₹68,400 Cr',
  },
  {
    code: '120503',
    name: 'Nippon India Small Cap Fund - Direct Growth',
    category: 'Small Cap',
    nav: '168.14',
    cagr1y: '+34.2%',
    cagr3y: '+28.6%',
    cagr5y: '+31.2%',
    risk: 'Very High',
    expenseRatio: '0.67%',
    aum: '₹55,200 Cr',
  },
  {
    code: '119598',
    name: 'SBI Bluechip Fund - Direct Growth',
    category: 'Large Cap',
    nav: '92.45',
    cagr1y: '+18.5%',
    cagr3y: '+14.2%',
    cagr5y: '+15.8%',
    risk: 'Moderate High',
    expenseRatio: '0.84%',
    aum: '₹46,100 Cr',
  },
  {
    code: '120716',
    name: 'Mirae Asset Large & Midcap Fund - Direct',
    category: 'Large & Mid Cap',
    nav: '142.60',
    cagr1y: '+24.1%',
    cagr3y: '+17.9%',
    cagr5y: '+19.2%',
    risk: 'Very High',
    expenseRatio: '0.59%',
    aum: '₹38,900 Cr',
  },
  {
    code: '118989',
    name: 'HDFC Balanced Advantage Fund - Direct Growth',
    category: 'Hybrid / Balanced',
    nav: '462.10',
    cagr1y: '+21.8%',
    cagr3y: '+16.5%',
    cagr5y: '+17.4%',
    risk: 'Moderate',
    expenseRatio: '0.72%',
    aum: '₹84,500 Cr',
  },
];

export default function InvestmentsAmfiPage() {
  const currency = useSelectedCurrency();
  const [selectedFund, setSelectedFund] = useState(AMFI_FUNDS[0]);
  const [sipAmount, setSipAmount] = useState('5000');
  const [sipFrequency, setSipFrequency] = useState('MONTHLY');
  const [sipDate, setSipDate] = useState('5');
  const [sipSuccess, setSipSuccess] = useState('');

  // Portfolio tracking state
  const totalInvested = 320000;
  const currentValuation = 412000;
  const absoluteGain = currentValuation - totalInvested;
  const xirr = 18.4; // %

  function handleStartSip(e) {
    e.preventDefault();
    setSipSuccess(`✓ Registered automated NACH mandate of ₹${sipAmount}/month for ${selectedFund.name} on the ${sipDate}th of each month!`);
    setTimeout(() => setSipSuccess(''), 5000);
  }

  return (
    <div className="page-shell">
      <PageNavHeader title="Investments & AMFI Mutual Funds" />

      <div className="page-header">
        <div>
          <p className="eyebrow">AMFI Data • Portfolio XIRR • Goal-Linked SIP Execution</p>
          <h1>Investments & Mutual Funds</h1>
        </div>
      </div>

      {sipSuccess && (
        <div style={{ background: '#ecfdf5', color: '#065f46', padding: '0.85rem 1.1rem', borderRadius: '10px', marginBottom: '1.25rem', border: '1px solid #a7f3d0', fontWeight: 600 }}>
          {sipSuccess}
        </div>
      )}

      {/* Portfolio Performance Summary */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div style={{ background: 'linear-gradient(135deg, #1e1b4b, #312e81)', color: '#ffffff', padding: '1.25rem', borderRadius: '14px' }}>
          <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#c7d2fe', fontWeight: 700 }}>PORTFOLIO VALUE</span>
          <div style={{ fontSize: '2rem', fontWeight: 800, marginTop: '0.3rem' }}>
            {formatMoneyAmount(currentValuation, currency)}
          </div>
          <span style={{ fontSize: '0.78rem', color: '#86efac' }}>XIRR: {xirr}% (Annualized Return)</span>
        </div>

        <div className="page-card">
          <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>TOTAL INVESTED</span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0f172a', marginTop: '0.3rem' }}>
            {formatMoneyAmount(totalInvested, currency)}
          </div>
          <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Across 4 Direct Mutual Funds</span>
        </div>

        <div className="page-card">
          <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: 700 }}>TOTAL UNREALIZED GAINS</span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#16a34a', marginTop: '0.3rem' }}>
            +{formatMoneyAmount(absoluteGain, currency)}
          </div>
          <span style={{ fontSize: '0.78rem', color: '#16a34a' }}>+28.75% Absolute Gain</span>
        </div>
      </div>

      {/* AMFI Funds Catalog */}
      <div className="page-card" style={{ marginBottom: '1.5rem' }}>
        <div className="panel-header">
          <div>
            <p className="eyebrow">Association of Mutual Funds in India (AMFI)</p>
            <h2>Top Recommended Direct Mutual Funds</h2>
          </div>
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Scheme Name</th>
                <th>Category</th>
                <th>Live NAV</th>
                <th>3Y CAGR</th>
                <th>5Y CAGR</th>
                <th>Expense Ratio</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {AMFI_FUNDS.map((fund) => (
                <tr key={fund.code} style={{ background: selectedFund.code === fund.code ? '#f0f9ff' : 'transparent' }}>
                  <td>
                    <strong style={{ display: 'block', color: '#0f172a' }}>{fund.name}</strong>
                    <span style={{ fontSize: '0.72rem', color: '#64748b' }}>AUM: {fund.aum} • {fund.risk}</span>
                  </td>
                  <td><span className="tag" style={{ background: '#e0f2fe', color: '#0369a1' }}>{fund.category}</span></td>
                  <td><strong>₹{fund.nav}</strong></td>
                  <td style={{ color: '#16a34a', fontWeight: 700 }}>{fund.cagr3y}</td>
                  <td style={{ color: '#16a34a', fontWeight: 700 }}>{fund.cagr5y}</td>
                  <td>{fund.expenseRatio}</td>
                  <td>
                    <button
                      type="button"
                      className="secondary-btn small"
                      onClick={() => setSelectedFund(fund)}
                    >
                      {selectedFund.code === fund.code ? 'Selected' : 'Select'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* SIP Execution Flow */}
      <div className="page-card">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Direct Execution Simulator</p>
            <h2>Start SIP Mandate in {selectedFund.name}</h2>
          </div>
        </div>

        <form onSubmit={handleStartSip} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Monthly SIP Amount (₹):</label>
            <input
              type="number"
              min="500"
              step="500"
              value={sipAmount}
              onChange={(e) => setSipAmount(e.target.value)}
              style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
              required
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Debit Date of Month:</label>
            <select
              value={sipDate}
              onChange={(e) => setSipDate(e.target.value)}
              style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}
            >
              <option value="1">1st of month</option>
              <option value="5">5th of month (Recommended after salary)</option>
              <option value="10">10th of month</option>
              <option value="15">15th of month</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>Source Bank Mandate:</label>
            <select style={{ width: '100%', padding: '0.6rem', borderRadius: '8px', border: '1px solid #cbd5e1' }}>
              <option value="HDFC">HDFC Bank Salary A/C (**4102)</option>
              <option value="SBI">State Bank of India (**8812)</option>
            </select>
          </div>

          <div>
            <button type="submit" className="primary-btn" style={{ width: '100%' }}>
              ✓ Authorize ECS / NACH SIP
            </button>
          </div>
        </form>
      </div>

      <PageNavFooter />
    </div>
  );
}
