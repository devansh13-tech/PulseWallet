import React, { useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './PageNavigation.css';

export const APP_ROUTES = [
  { path: '/', label: 'Dashboard', short: 'Dashboard', icon: '📊', desc: 'Financial Overview & AI Recommendations' },
  { path: '/bank-ingestion', label: 'Bank & Transaction Ingestion', short: 'Bank Ingest', icon: '🏛️', desc: 'AA Consents, SMS Parser & ML Classifier' },
  { path: '/fraud-center', label: 'Fraud Security Center', short: 'Fraud Radar', icon: '🛡️', desc: 'Anomaly Detector, Phishing & Forensic Report' },
  { path: '/insights', label: 'Proactive Insights', short: 'Insights', icon: '💡', desc: 'Safe to Spend Today & Subscription Tracker' },
  { path: '/credit-networth', label: 'Credit & Net Worth Hub', short: 'Net Worth', icon: '💰', desc: 'CIBIL Score, Debt Payoff & Section 80C' },
  { path: '/investments', label: 'AMFI Investments Hub', short: 'Investments', icon: '📈', desc: 'Top Mutual Funds, Portfolio XIRR & SIPs' },
  { path: '/social-family', label: 'Gamification & Family', short: 'Social Hub', icon: '👥', desc: 'Health Score, Streaks, Bill Split & Wallets' },
  { path: '/transactions', label: 'Transactions', short: 'Transactions', icon: '💳', desc: 'History, ledgers & spending breakdown' },
  { path: '/transactions/new', label: 'New Transaction', short: 'New Txn', icon: '➕', desc: 'Record an income or expense entry' },
  { path: '/budgets', label: 'Budgets', short: 'Budgets', icon: '🎯', desc: 'Monthly limits & category budget tracking' },
  { path: '/goals', label: 'Financial Goals', short: 'Goals', icon: '🏆', desc: 'Savings milestones & deadline targets' },
  { path: '/financial-plan', label: 'Financial Plan', short: 'Plan', icon: '📈', desc: 'AI wealth strategy & horizon allocation' },
  { path: '/credit-cards', label: 'Credit Cards', short: 'Cards', icon: '🪪', desc: 'Card intelligence, utilization & health' },
  { path: '/linked-accounts', label: 'Linked Accounts', short: 'Bank Accounts', icon: '🏦', desc: 'Indian bank accounts & liquid balances' },
  { path: '/alerts', label: 'Alerts', short: 'Alerts', icon: '🔔', desc: 'Notifications, anomalies & risk alerts' },
];

export function usePageNavigation() {
  const location = useLocation();
  const navigate = useNavigate();
  const pathname = location.pathname;

  const list = APP_ROUTES;
  let idx = list.findIndex((r) => r.path === pathname);

  if (idx === -1) {
    idx = APP_ROUTES.findIndex((r) => pathname.startsWith(r.path) && r.path !== '/');
    if (idx === -1) {
      idx = 0;
    }
  }

  const prevIdx = (idx - 1 + list.length) % list.length;
  const nextIdx = (idx + 1) % list.length;

  const current = list[idx];
  const prev = list[prevIdx];
  const next = list[nextIdx];

  const goBack = () => {
    navigate(prev.path);
  };

  const goNext = () => {
    navigate(next.path);
  };

  // Keyboard shortcut listener: Alt + ArrowLeft / Alt + ArrowRight
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Ignore if user is typing in input/textarea/select
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) {
        return;
      }

      if (e.altKey && e.key === 'ArrowLeft') {
        e.preventDefault();
        goBack();
      } else if (e.altKey && e.key === 'ArrowRight') {
        e.preventDefault();
        goNext();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [prev.path, next.path]);

  return {
    current,
    prev,
    next,
    currentIndex: idx,
    totalCount: list.length,
    allRoutes: list,
    goBack,
    goNext,
    navigate,
  };
}

/**
 * Top Header Back & Next Navigation Bar with breadcrumbs and step dots.
 */
export function PageNavHeader({ title, showBreadcrumbs = true }) {
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated || location.pathname === '/login' || location.pathname === '/signup') {
    return null;
  }
  const { current, prev, next, currentIndex, totalCount, allRoutes, goBack, goNext, navigate } = usePageNavigation();

  return (
    <nav className="page-nav-bar top" aria-label="Page navigation header">
      <div className="page-nav-left">
        <button
          type="button"
          className="nav-btn nav-back-btn"
          onClick={goBack}
          title={`Go back to ${prev.label} (Alt + ←)`}
          id="nav-btn-back-header"
        >
          <span className="nav-arrow">←</span>
          <span className="nav-label">Back</span>
          <span className="nav-target-badge">{prev.icon} {prev.short}</span>
        </button>
      </div>

      <div className="page-nav-center">
        {showBreadcrumbs && (
          <div className="page-nav-breadcrumbs">
            <Link to="/" className="nav-crumb-home">🏠 PulseWallet</Link>
            <span className="crumb-sep">/</span>
            <span className="nav-crumb-current">{title || current.label}</span>
            <span className="nav-step-pill">Step {currentIndex + 1} of {totalCount}</span>
          </div>
        )}

        <div className="nav-dots" role="tablist" aria-label="Workflow progress steps">
          {allRoutes.map((r, i) => (
            <button
              key={r.path}
              type="button"
              className={`nav-dot ${i === currentIndex ? 'active' : ''}`}
              onClick={() => navigate(r.path)}
              title={`Jump to ${r.label} (Step ${i + 1})`}
              aria-label={`Step ${i + 1}: ${r.label}`}
            />
          ))}
        </div>
      </div>

      <div className="page-nav-right">
        <button
          type="button"
          className="nav-btn nav-next-btn"
          onClick={goNext}
          title={`Go next to ${next.label} (Alt + →)`}
          id="nav-btn-next-header"
        >
          <span className="nav-label">Next</span>
          <span className="nav-target-badge">{next.icon} {next.short}</span>
          <span className="nav-arrow">→</span>
        </button>
      </div>
    </nav>
  );
}

/**
 * Bottom Footer Navigation with dual interactive cards.
 */
export function PageNavFooter() {
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated || location.pathname === '/login' || location.pathname === '/signup') {
    return null;
  }
  const { current, prev, next, currentIndex, totalCount, goBack, goNext } = usePageNavigation();

  return (
    <footer className="page-nav-footer" aria-label="Bottom page navigation">
      <button
        type="button"
        className="nav-footer-card prev-card"
        onClick={goBack}
        title={`Navigate to ${prev.label}`}
        id="nav-btn-back-footer"
      >
        <div className="nav-footer-arrow">←</div>
        <div className="nav-footer-text">
          <span className="nav-footer-hint">PREVIOUS PAGE</span>
          <div className="nav-footer-title">{prev.icon} {prev.label}</div>
          <span className="nav-footer-desc">{prev.desc}</span>
        </div>
      </button>

      <div className="nav-footer-center-summary">
        <span className="step-count">Step {currentIndex + 1} of {totalCount}</span>
        <div className="progress-bar-mini" title={`Progress: ${Math.round(((currentIndex + 1) / totalCount) * 100)}%`}>
          <div
            className="progress-fill-mini"
            style={{ width: `${((currentIndex + 1) / totalCount) * 100}%` }}
          />
        </div>
        <span className="active-page-name">{current.label}</span>
      </div>

      <button
        type="button"
        className="nav-footer-card next-card"
        onClick={goNext}
        title={`Navigate to ${next.label}`}
        id="nav-btn-next-footer"
      >
        <div className="nav-footer-text">
          <span className="nav-footer-hint">NEXT PAGE</span>
          <div className="nav-footer-title">{next.label} {next.icon}</div>
          <span className="nav-footer-desc">{next.desc}</span>
        </div>
        <div className="nav-footer-arrow">→</div>
      </button>
    </footer>
  );
}

/**
 * Floating Quick-Nav pill fixed at the bottom of the screen.
 */
export function FloatingPageNav() {
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated || location.pathname === '/login' || location.pathname === '/signup') {
    return null;
  }
  const { current, prev, next, currentIndex, totalCount, goBack, goNext } = usePageNavigation();

  return (
    <div className="floating-page-nav" role="navigation" aria-label="Quick page navigation pill">
      <button
        type="button"
        className="floating-nav-btn back"
        onClick={goBack}
        title={`Back to ${prev.label} (Alt + ←)`}
        id="nav-btn-back-float"
      >
        ← Back
      </button>

      <span className="floating-step-badge" title={`Currently on ${current.label}`}>
        {current.icon} {currentIndex + 1}/{totalCount}
      </span>

      <button
        type="button"
        className="floating-nav-btn next"
        onClick={goNext}
        title={`Next to ${next.label} (Alt + →)`}
        id="nav-btn-next-float"
      >
        Next →
      </button>
    </div>
  );
}

/**
 * Contextual Back and Next buttons for sub-tabs and wizard steps within a single page or modal.
 */
export function TabStepNav({ tabs, activeTab, onTabChange }) {
  const currentIndex = tabs.findIndex((t) => (typeof t === 'string' ? t === activeTab : t.id === activeTab));
  if (currentIndex === -1) return null;

  const prevTab = currentIndex > 0 ? tabs[currentIndex - 1] : null;
  const nextTab = currentIndex < tabs.length - 1 ? tabs[currentIndex + 1] : null;

  const prevId = prevTab ? (typeof prevTab === 'string' ? prevTab : prevTab.id) : null;
  const prevLabel = prevTab ? (typeof prevTab === 'string' ? prevTab : prevTab.label) : null;

  const nextId = nextTab ? (typeof nextTab === 'string' ? nextTab : nextTab.id) : null;
  const nextLabel = nextTab ? (typeof nextTab === 'string' ? nextTab : nextTab.label) : null;

  return (
    <div className="tab-step-nav" aria-label="Tab step navigation">
      {prevTab ? (
        <button
          type="button"
          className="tab-step-btn prev"
          onClick={() => {
            onTabChange(prevId);
            window.scrollTo({ top: 120, behavior: 'smooth' });
          }}
          title={`Back to ${prevLabel}`}
        >
          <span className="tab-step-arrow">←</span>
          <span>Previous: {prevLabel}</span>
        </button>
      ) : (
        <div className="tab-step-placeholder" />
      )}

      <div className="tab-step-counter">
        <span>Step {currentIndex + 1} of {tabs.length}</span>
      </div>

      {nextTab ? (
        <button
          type="button"
          className="tab-step-btn next"
          onClick={() => {
            onTabChange(nextId);
            window.scrollTo({ top: 120, behavior: 'smooth' });
          }}
          title={`Continue to ${nextLabel}`}
        >
          <span>Next: {nextLabel}</span>
          <span className="tab-step-arrow">→</span>
        </button>
      ) : (
        <div className="tab-step-placeholder" />
      )}
    </div>
  );
}

export default function PageNavigation({ position = 'both', title }) {
  return (
    <>
      {(position === 'top' || position === 'both') && <PageNavHeader title={title} />}
      {(position === 'bottom' || position === 'both') && <PageNavFooter />}
    </>
  );
}
