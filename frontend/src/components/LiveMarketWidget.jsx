import React, { useEffect, useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { marketApi } from '../lib/api';

const RANGES = [
  { key: '1D', label: '1D (Intraday)' },
  { key: '1W', label: '1W' },
  { key: '1M', label: '1M' },
  { key: '1Y', label: '1Y' },
];

const MOVER_CATEGORIES = [
  { key: 'IPO', label: 'Indian IPOs', badge: 'Listings' },
  { key: 'MUTUAL_FUND', label: 'Mutual Funds', badge: 'Top Schemes' },
  { key: 'SIP', label: 'SIP Champions', badge: 'Wealth Builders' },
];

function formatIndexNumber(val) {
  if (val == null || isNaN(val)) return '—';
  return Number(val).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function CustomTooltip({ active, payload, label, selectedIndex, isCompare }) {
  if (!active || !payload || !payload.length) return null;

  if (isCompare) {
    return (
      <div className="market-tooltip">
        <p className="tooltip-time">{label}</p>
        {payload.map((entry) => (
          <div key={entry.name} className="tooltip-row">
            <span className="tooltip-dot" style={{ backgroundColor: entry.color }} />
            <span className="tooltip-name">{entry.name}:</span>
            <strong className="tooltip-val" style={{ color: entry.color }}>
              {entry.value >= 0 ? `+${entry.value}%` : `${entry.value}%`}
            </strong>
          </div>
        ))}
      </div>
    );
  }

  const dataPoint = payload[0]?.payload;
  const price = payload[0]?.value;
  const changePct = dataPoint?.changeFromPrevClosePercent;
  const isPositive = Number(changePct ?? 0) >= 0;

  return (
    <div className="market-tooltip">
      <p className="tooltip-time">{label}</p>
      <div className="tooltip-row">
        <span className="tooltip-name">{selectedIndex}:</span>
        <strong className="tooltip-val">{formatIndexNumber(price)} pts</strong>
      </div>
      {changePct != null && (
        <div className={`tooltip-change ${isPositive ? 'positive' : 'negative'}`}>
          {isPositive ? '▲ +' : '▼ '}
          {Number(changePct).toFixed(2)}% vs Prev Close
        </div>
      )}
    </div>
  );
}

export default function LiveMarketWidget() {
  const [selectedRange, setSelectedRange] = useState('1D');
  const [selectedView, setSelectedView] = useState('NIFTY'); // 'NIFTY' | 'SENSEX' | 'COMPARE'
  const [marketData, setMarketData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastRefreshedAt, setLastRefreshedAt] = useState(null);

  // Movers state (Top 10 Gainers & Losers)
  const [moversData, setMoversData] = useState(null);
  const [loadingMovers, setLoadingMovers] = useState(true);
  const [selectedMoverCategory, setSelectedMoverCategory] = useState('IPO'); // 'IPO' | 'MUTUAL_FUND' | 'SIP'
  const [selectedMoverMode, setSelectedMoverMode] = useState('GAINERS'); // 'GAINERS' | 'LOSERS'
  const [searchMoverQuery, setSearchMoverQuery] = useState('');

  const fetchIndices = async (isBackground = false) => {
    if (!isBackground) setLoading(true);
    else setRefreshing(true);
    setError('');

    try {
      const response = await marketApi.indices(selectedRange);
      if (response?.data) {
        setMarketData(response.data);
      } else if (response) {
        setMarketData(response);
      }
      setLastRefreshedAt(new Date());
    } catch (err) {
      if (!marketData) {
        setError('Market data feed temporarily unavailable. Displaying cached benchmarks.');
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const fetchMovers = async () => {
    setLoadingMovers(true);
    try {
      const response = await marketApi.movers();
      if (response?.data) {
        setMoversData(response.data);
      } else if (response) {
        setMoversData(response);
      }
    } catch (err) {
      // Graceful error handling
    } finally {
      setLoadingMovers(false);
    }
  };

  useEffect(() => {
    fetchIndices();
  }, [selectedRange]);

  useEffect(() => {
    fetchMovers();
  }, []);

  // Polling every 20 seconds for indices if auto-refresh is active
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => {
      fetchIndices(true);
    }, 20000);
    return () => clearInterval(interval);
  }, [autoRefresh, selectedRange]);

  const nifty = marketData?.indices?.NIFTY;
  const sensex = marketData?.indices?.SENSEX;
  const aiInsight = marketData?.aiMarketInsight;
  const isMarketOpen = marketData?.isMarketOpen ?? false;

  const activeIndex = selectedView === 'SENSEX' ? sensex : nifty;
  const isPositive = Number(activeIndex?.change ?? 0) >= 0;

  // Prepare chart dataset
  const chartData = useMemo(() => {
    if (selectedView === 'COMPARE') {
      if (!nifty?.history || !sensex?.history) return [];
      const len = Math.min(nifty.history.length, sensex.history.length);
      const combined = [];
      for (let i = 0; i < len; i++) {
        combined.push({
          label: nifty.history[i]?.label || sensex.history[i]?.label || '',
          timestamp: nifty.history[i]?.timestamp,
          niftyPct: Number(nifty.history[i]?.changeFromPrevClosePercent ?? 0),
          sensexPct: Number(sensex.history[i]?.changeFromPrevClosePercent ?? 0),
        });
      }
      return combined;
    }

    if (!activeIndex?.history) return [];
    return activeIndex.history.map((pt) => ({
      ...pt,
      price: Number(pt.price),
    }));
  }, [selectedView, activeIndex, nifty, sensex]);

  const yDomain = useMemo(() => {
    if (selectedView === 'COMPARE' || !chartData.length) return ['auto', 'auto'];
    const prices = chartData.map((d) => d.price).filter((p) => !isNaN(p) && p > 0);
    if (!prices.length) return ['auto', 'auto'];
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const padding = (max - min) * 0.1 || min * 0.005;
    return [Math.floor(min - padding), Math.ceil(max + padding)];
  }, [selectedView, chartData]);

  const dayRangePct = useMemo(() => {
    if (!activeIndex?.dayLow || !activeIndex?.dayHigh || !activeIndex?.currentPrice) return 50;
    const low = Number(activeIndex.dayLow);
    const high = Number(activeIndex.dayHigh);
    const current = Number(activeIndex.currentPrice);
    if (high <= low) return 50;
    const pct = ((current - low) / (high - low)) * 100;
    return Math.max(0, Math.min(100, pct));
  }, [activeIndex]);

  // Current active movers list (top 10 gainers or top 10 losers)
  const activeMoversList = useMemo(() => {
    if (!moversData) return [];
    let list = [];
    if (selectedMoverCategory === 'IPO') {
      list = selectedMoverMode === 'GAINERS' ? moversData.ipoGainers : moversData.ipoLosers;
    } else if (selectedMoverCategory === 'MUTUAL_FUND') {
      list = selectedMoverMode === 'GAINERS' ? moversData.mutualFundGainers : moversData.mutualFundLosers;
    } else {
      list = selectedMoverMode === 'GAINERS' ? moversData.sipGainers : moversData.sipLosers;
    }

    if (!list) return [];
    if (!searchMoverQuery.trim()) return list;

    const q = searchMoverQuery.toLowerCase().trim();
    return list.filter(
      (item) =>
        item.name.toLowerCase().includes(q) ||
        item.subCategory?.toLowerCase().includes(q) ||
        item.aiVerdict?.toLowerCase().includes(q) ||
        item.aiAnalysis?.toLowerCase().includes(q)
    );
  }, [moversData, selectedMoverCategory, selectedMoverMode, searchMoverQuery]);

  const strokeColor = isPositive ? '#10b981' : '#f43f5e';
  const gradientId = `marketGradient-${selectedView}-${isPositive ? 'up' : 'down'}`;

  return (
    <div className="live-market-container">
      {/* Top Banner / Tickers */}
      <div className="market-header-bar">
        <div className="market-live-indicator">
          <span className={`pulse-dot ${isMarketOpen ? 'live' : 'closed'}`} />
          <span className="market-session-tag">
            {isMarketOpen ? 'NSE / BSE LIVE SESSION' : 'MARKET CLOSED (LAST CLOSE)'}
          </span>
          {lastRefreshedAt && (
            <span className="last-sync-tag">
              Updated {lastRefreshedAt.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </span>
          )}
        </div>

        <div className="market-header-actions">
          <label className="toggle-label" title="Toggle automatic updates every 20 seconds">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            <span>Auto-live</span>
          </label>
          <button
            type="button"
            className={`market-refresh-btn ${refreshing ? 'spinning' : ''}`}
            onClick={() => {
              fetchIndices(true);
              fetchMovers();
            }}
            title="Refresh market quotes and movers"
            disabled={refreshing}
          >
            ↻ {refreshing ? 'Updating...' : 'Refresh'}
          </button>
        </div>
      </div>

      {/* Index Selector Cards */}
      <div className="market-index-cards">
        {/* NIFTY 50 CARD */}
        <div
          role="button"
          tabIndex={0}
          className={`index-card ${selectedView === 'NIFTY' ? 'active' : ''} ${Number(nifty?.change ?? 0) >= 0 ? 'bull' : 'bear'}`}
          onClick={() => setSelectedView('NIFTY')}
          onKeyDown={(e) => e.key === 'Enter' && setSelectedView('NIFTY')}
        >
          <div className="index-card-head">
            <span className="index-chip nse">NSE</span>
            <h4>NIFTY 50</h4>
            <span className={`trend-badge ${Number(nifty?.change ?? 0) >= 0 ? 'up' : 'down'}`}>
              {Number(nifty?.change ?? 0) >= 0 ? '▲ BULLISH' : '▼ BEARISH'}
            </span>
          </div>
          <div className="index-price-row">
            <span className="current-points">{formatIndexNumber(nifty?.currentPrice)}</span>
            <div className={`price-delta ${Number(nifty?.change ?? 0) >= 0 ? 'up' : 'down'}`}>
              <strong>{Number(nifty?.change ?? 0) >= 0 ? `+${formatIndexNumber(nifty?.change)}` : formatIndexNumber(nifty?.change)}</strong>
              <span>({Number(nifty?.changePercent ?? 0) >= 0 ? `+${Number(nifty?.changePercent ?? 0).toFixed(2)}%` : `${Number(nifty?.changePercent ?? 0).toFixed(2)}%`})</span>
            </div>
          </div>
          <div className="index-meta-stats">
            <span>Low: {formatIndexNumber(nifty?.dayLow)}</span>
            <span>High: {formatIndexNumber(nifty?.dayHigh)}</span>
          </div>
        </div>

        {/* BSE SENSEX CARD */}
        <div
          role="button"
          tabIndex={0}
          className={`index-card ${selectedView === 'SENSEX' ? 'active' : ''} ${Number(sensex?.change ?? 0) >= 0 ? 'bull' : 'bear'}`}
          onClick={() => setSelectedView('SENSEX')}
          onKeyDown={(e) => e.key === 'Enter' && setSelectedView('SENSEX')}
        >
          <div className="index-card-head">
            <span className="index-chip bse">BSE</span>
            <h4>SENSEX</h4>
            <span className={`trend-badge ${Number(sensex?.change ?? 0) >= 0 ? 'up' : 'down'}`}>
              {Number(sensex?.change ?? 0) >= 0 ? '▲ BULLISH' : '▼ BEARISH'}
            </span>
          </div>
          <div className="index-price-row">
            <span className="current-points">{formatIndexNumber(sensex?.currentPrice)}</span>
            <div className={`price-delta ${Number(sensex?.change ?? 0) >= 0 ? 'up' : 'down'}`}>
              <strong>{Number(sensex?.change ?? 0) >= 0 ? `+${formatIndexNumber(sensex?.change)}` : formatIndexNumber(sensex?.change)}</strong>
              <span>({Number(sensex?.changePercent ?? 0) >= 0 ? `+${Number(sensex?.changePercent ?? 0).toFixed(2)}%` : `${Number(sensex?.changePercent ?? 0).toFixed(2)}%`})</span>
            </div>
          </div>
          <div className="index-meta-stats">
            <span>Low: {formatIndexNumber(sensex?.dayLow)}</span>
            <span>High: {formatIndexNumber(sensex?.dayHigh)}</span>
          </div>
        </div>

        {/* COMPARE BUTTON */}
        <div
          role="button"
          tabIndex={0}
          className={`index-card compare-card ${selectedView === 'COMPARE' ? 'active' : ''}`}
          onClick={() => setSelectedView('COMPARE')}
          onKeyDown={(e) => e.key === 'Enter' && setSelectedView('COMPARE')}
        >
          <div className="index-card-head">
            <span className="index-chip duo">DUAL</span>
            <h4>Compare %</h4>
          </div>
          <p className="compare-desc">Superimpose NIFTY 50 vs SENSEX performance curve</p>
          <div className="compare-indicators">
            <span className="legend-item nifty"><i /> Nifty 50</span>
            <span className="legend-item sensex"><i /> Sensex</span>
          </div>
        </div>
      </div>

      {/* Chart Toolbar: Timeframe selector and Day Range Bar */}
      <div className="market-chart-controls">
        <div className="range-pills" role="tablist">
          {RANGES.map((r) => (
            <button
              key={r.key}
              type="button"
              role="tab"
              aria-selected={selectedRange === r.key}
              className={`range-pill ${selectedRange === r.key ? 'active' : ''}`}
              onClick={() => setSelectedRange(r.key)}
            >
              {r.label}
            </button>
          ))}
        </div>

        {selectedView !== 'COMPARE' && activeIndex?.dayLow && activeIndex?.dayHigh && (
          <div className="day-range-wrapper">
            <span className="range-bound-label">L: {formatIndexNumber(activeIndex.dayLow)}</span>
            <div className="day-range-track">
              <div className="range-fill" style={{ width: `${dayRangePct}%` }} />
              <div className="range-marker" style={{ left: `${dayRangePct}%` }} title={`Current: ${formatIndexNumber(activeIndex.currentPrice)}`} />
            </div>
            <span className="range-bound-label">H: {formatIndexNumber(activeIndex.dayHigh)}</span>
          </div>
        )}
      </div>

      {/* Main Chart Area */}
      <div className="market-graph-viewport">
        {loading && !marketData ? (
          <div className="market-loading-skeleton">
            <div className="spinner-orbit" />
            <p>Streaming live index data from NSE / BSE...</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            {selectedView === 'COMPARE' ? (
              <LineChart data={chartData} margin={{ top: 10, right: 15, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.07)" vertical={false} />
                <XAxis
                  dataKey="label"
                  stroke="rgba(255,255,255,0.4)"
                  fontSize={11}
                  tickLine={false}
                  minTickGap={25}
                />
                <YAxis
                  stroke="rgba(255,255,255,0.4)"
                  fontSize={11}
                  tickLine={false}
                  domain={['auto', 'auto']}
                  tickFormatter={(val) => `${val >= 0 ? '+' : ''}${val.toFixed(1)}%`}
                />
                <Tooltip
                  content={<CustomTooltip isCompare selectedIndex={selectedView} />}
                />
                <ReferenceLine y={0} stroke="rgba(255,255,255,0.25)" strokeDasharray="3 3" />
                <Line
                  type="monotone"
                  dataKey="niftyPct"
                  name="NIFTY 50"
                  stroke="#06b6d4"
                  strokeWidth={2.2}
                  dot={false}
                  activeDot={{ r: 5, fill: '#06b6d4', stroke: '#fff', strokeWidth: 2 }}
                />
                <Line
                  type="monotone"
                  dataKey="sensexPct"
                  name="BSE SENSEX"
                  stroke="#f59e0b"
                  strokeWidth={2.2}
                  dot={false}
                  activeDot={{ r: 5, fill: '#f59e0b', stroke: '#fff', strokeWidth: 2 }}
                />
              </LineChart>
            ) : (
              <AreaChart data={chartData} margin={{ top: 10, right: 15, left: -5, bottom: 0 }}>
                <defs>
                  <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={strokeColor} stopOpacity={0.35} />
                    <stop offset="95%" stopColor={strokeColor} stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.07)" vertical={false} />
                <XAxis
                  dataKey="label"
                  stroke="rgba(255,255,255,0.4)"
                  fontSize={11}
                  tickLine={false}
                  minTickGap={25}
                />
                <YAxis
                  stroke="rgba(255,255,255,0.4)"
                  fontSize={11}
                  tickLine={false}
                  domain={yDomain}
                  tickFormatter={(val) => Math.round(val).toLocaleString('en-IN')}
                />
                <Tooltip
                  content={<CustomTooltip isCompare={false} selectedIndex={selectedView === 'SENSEX' ? 'BSE SENSEX' : 'NIFTY 50'} />}
                />
                {activeIndex?.previousClose && (
                  <ReferenceLine
                    y={Number(activeIndex.previousClose)}
                    stroke="rgba(255,255,255,0.3)"
                    strokeDasharray="4 4"
                    label={{
                      value: `Prev Close: ${formatIndexNumber(activeIndex.previousClose)}`,
                      position: 'top',
                      fill: 'rgba(255,255,255,0.5)',
                      fontSize: 10,
                    }}
                  />
                )}
                <Area
                  type="monotone"
                  dataKey="price"
                  name={selectedView}
                  stroke={strokeColor}
                  strokeWidth={2.5}
                  fillOpacity={1}
                  fill={`url(#${gradientId})`}
                  dot={false}
                  activeDot={{ r: 6, fill: strokeColor, stroke: '#ffffff', strokeWidth: 2 }}
                />
              </AreaChart>
            )}
          </ResponsiveContainer>
        )}
      </div>

      {/* AI Market Context & Financial Recommendation Callout */}
      {aiInsight && (
        <div className="ai-market-insight-card">
          <div className="ai-insight-top">
            <div className="ai-insight-pill-group">
              <span className={`sentiment-badge ${aiInsight.sentiment?.toLowerCase()}`}>
                ● Sentiment: {aiInsight.sentiment?.replace('_', ' ')}
              </span>
              <span className="insight-category-tag">Live Market Advisory</span>
            </div>
            <strong className="ai-insight-headline">{aiInsight.headline}</strong>
          </div>

          <div className="ai-insight-body-grid">
            <div className="ai-advice-item">
              <span className="advice-label">💡 AI Strategy for Your Plan:</span>
              <p>{aiInsight.recommendation}</p>
            </div>
            <div className="ai-advice-item">
              <span className="advice-label">🛡️ Asset Allocation Rule:</span>
              <p>{aiInsight.assetAllocationAdvice}</p>
            </div>
          </div>

          {aiInsight.keyObservations?.length > 0 && (
            <div className="market-takeaways-bar">
              {aiInsight.keyObservations.slice(0, 3).map((obs, idx) => (
                <span key={idx} className="takeaway-pill">
                  {obs}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ==============================================================================
          INDIA TOP 10 GAINERS & LOSERS (IPOs, Mutual Funds & SIPs)
          ============================================================================== */}
      <div className="market-movers-section">
        <div className="movers-header">
          <div>
            <div className="movers-eyebrow-row">
              <span className="movers-icon">🇮🇳</span>
              <span className="eyebrow">MARKET MOVER INTELLIGENCE</span>
            </div>
            <h4 className="movers-title">
              India Top 10 {selectedMoverMode === 'GAINERS' ? 'Gainers & High Performers' : 'Loss Making & Laggards'}
            </h4>
            <p className="movers-subtitle">
              Comprehensive performance screening across Indian IPO listings, mutual fund schemes, and systematic SIP allocations.
            </p>
          </div>

          <div className="movers-mode-toggle">
            <button
              type="button"
              className={`mode-btn gainers ${selectedMoverMode === 'GAINERS' ? 'active' : ''}`}
              onClick={() => setSelectedMoverMode('GAINERS')}
            >
              🚀 Top 10 Gainers
            </button>
            <button
              type="button"
              className={`mode-btn losers ${selectedMoverMode === 'LOSERS' ? 'active' : ''}`}
              onClick={() => setSelectedMoverMode('LOSERS')}
            >
              🔻 Top 10 Loss Making
            </button>
          </div>
        </div>

        {/* Category Segmented Tabs & Search Bar */}
        <div className="movers-nav-bar">
          <div className="category-tabs" role="tablist">
            {MOVER_CATEGORIES.map((cat) => (
              <button
                key={cat.key}
                type="button"
                role="tab"
                aria-selected={selectedMoverCategory === cat.key}
                className={`category-tab-btn ${selectedMoverCategory === cat.key ? 'active' : ''}`}
                onClick={() => setSelectedMoverCategory(cat.key)}
              >
                <span>{cat.label}</span>
                <span className="category-badge">{cat.badge}</span>
              </button>
            ))}
          </div>

          <div className="movers-search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder={`Search ${selectedMoverCategory.toLowerCase().replace('_', ' ')}...`}
              value={searchMoverQuery}
              onChange={(e) => setSearchMoverQuery(e.target.value)}
            />
            {searchMoverQuery && (
              <button
                type="button"
                className="clear-search-btn"
                onClick={() => setSearchMoverQuery('')}
              >
                ✕
              </button>
            )}
          </div>
        </div>

        {/* Movers Grid / List */}
        {loadingMovers && !moversData ? (
          <div className="movers-loading-state">
            <div className="spinner-orbit" />
            <p>Analyzing top Indian market gainers and drawdowns...</p>
          </div>
        ) : activeMoversList.length === 0 ? (
          <div className="movers-empty-card">
            No instruments found matching "{searchMoverQuery}".
          </div>
        ) : (
          <div className="movers-cards-grid">
            {activeMoversList.map((item, index) => {
              const isGain = Number(item.changePercent ?? 0) >= 0;
              const rankClass = index === 0 ? 'rank-1' : index === 1 ? 'rank-2' : index === 2 ? 'rank-3' : '';

              return (
                <div
                  key={item.id}
                  className={`mover-card ${isGain ? 'gain-card' : 'loss-card'} ${rankClass}`}
                >
                  <div className="mover-card-top">
                    <div className="mover-identity">
                      <span className={`mover-rank-badge ${rankClass}`}>#{index + 1}</span>
                      <div>
                        <h5 className="mover-name">{item.name}</h5>
                        <span className="mover-subcat">{item.subCategory}</span>
                      </div>
                    </div>

                    <div className="mover-return-pill">
                      <strong className={`return-val ${isGain ? 'positive' : 'negative'}`}>
                        {isGain ? `+${Number(item.changePercent).toFixed(2)}%` : `${Number(item.changePercent).toFixed(2)}%`}
                      </strong>
                      <span className="return-label">
                        {selectedMoverCategory === 'IPO' ? 'Listing Gain' : selectedMoverCategory === 'SIP' ? '3Y SIP Return' : '1Y Return'}
                      </span>
                    </div>
                  </div>

                  <div className="mover-pricing-row">
                    <div className="pricing-stat">
                      <span className="stat-label">
                        {selectedMoverCategory === 'IPO' ? 'Current Price (CMP)' : selectedMoverCategory === 'SIP' ? 'SIP Corpus Value' : 'Current NAV'}
                      </span>
                      <strong className="stat-val">
                        ₹{item.currentPriceOrNav?.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </strong>
                    </div>

                    <div className="pricing-stat">
                      <span className="stat-label">
                        {selectedMoverCategory === 'IPO' ? 'Issue Price' : selectedMoverCategory === 'SIP' ? 'Total Invested' : 'Benchmark / 1Y Ago'}
                      </span>
                      <strong className="stat-val muted">
                        ₹{item.benchmarkPrice?.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </strong>
                    </div>

                    {item.return3Y != null && (
                      <div className="pricing-stat">
                        <span className="stat-label">3Y CAGR / XIRR</span>
                        <strong className={`stat-val ${Number(item.return3Y) >= 0 ? 'positive' : 'negative'}`}>
                          {Number(item.return3Y) >= 0 ? `+${Number(item.return3Y).toFixed(1)}%` : `${Number(item.return3Y).toFixed(1)}%`}
                        </strong>
                      </div>
                    )}
                  </div>

                  <div className="mover-meta-row">
                    <div className="meta-left">
                      <span className={`risk-badge ${item.riskLevel?.toLowerCase()}`}>
                        {item.riskLevel?.replace('_', ' ')} RISK
                      </span>
                      <span className="stars-rating">
                        {'★'.repeat(item.rating)}{'☆'.repeat(5 - item.rating)}
                      </span>
                    </div>

                    <span className={`verdict-badge verdict-${item.aiVerdict?.toLowerCase()}`}>
                      AI: {item.aiVerdict?.replace('_', ' ')}
                    </span>
                  </div>

                  <div className="mover-ai-advice-box">
                    <p className="mover-advice-text">
                      <strong>AI Takeaway:</strong> {item.aiAnalysis}
                    </p>
                    {item.suitableProfiles?.length > 0 && (
                      <div className="mover-profiles-tags">
                        <span>Profile fit:</span>
                        {item.suitableProfiles.map((p) => (
                          <span key={p} className="profile-tag">
                            {p}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {error && <div className="market-notice-inline">{error}</div>}
    </div>
  );
}
