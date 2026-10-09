import React from 'react';

const TICKER_ITEMS = [
  { symbol: 'NIFTY 50', value: '25,014.60', change: '+0.42%', positive: true, icon: '▲' },
  { symbol: 'SENSEX', value: '81,765.30', change: '+0.38%', positive: true, icon: '▲' },
  { symbol: 'BANK NIFTY', value: '51,420.15', change: '+0.65%', positive: true, icon: '▲' },
  { symbol: 'INDIA VIX', value: '12.84', change: '-2.14%', positive: false, icon: '▼' },
  { symbol: 'HDFC BANK', value: '₹1,682.40', change: '+1.20%', positive: true, icon: '▲' },
  { symbol: 'RELIANCE', value: '₹2,940.00', change: '-0.35%', positive: false, icon: '▼' },
  { symbol: 'ICICI BANK', value: '₹1,245.50', change: '+1.45%', positive: true, icon: '▲' },
  { symbol: 'TCS', value: '₹4,180.50', change: '+0.75%', positive: true, icon: '▲' },
  { symbol: 'INFOSYS', value: '₹1,912.00', change: '+0.90%', positive: true, icon: '▲' },
  { symbol: 'GOLD 24K', value: '₹76,450/10g', change: '+0.55%', positive: true, icon: '▲' },
  { symbol: 'USD/INR', value: '₹83.92', change: '0.00%', positive: true, icon: '◆' },
  { symbol: 'RBI REPO', value: '6.50%', change: 'STABLE', positive: true, icon: '🏛️' },
];

export default function FinancialTickerTape({ theme = 'money' }) {
  // Double the list for infinite seamless marquee loop
  const displayItems = [...TICKER_ITEMS, ...TICKER_ITEMS];

  return (
    <div className={`financial-ticker-tape theme-${theme}`} aria-label="Real-time Indian financial market ticker">
      <div className="ticker-label-badge">
        <span className="live-pulsing-dot" aria-hidden="true" />
        <span className="ticker-badge-text">LIVE DALAL ST</span>
      </div>
      <div className="ticker-track-container">
        <div className="ticker-track">
          {displayItems.map((item, index) => (
            <div key={`${item.symbol}-${index}`} className="ticker-item">
              <span className="ticker-symbol">{item.symbol}</span>
              <strong className="ticker-value">{item.value}</strong>
              <span className={`ticker-change ${item.positive ? 'pos' : 'neg'}`}>
                {item.icon} {item.change}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
