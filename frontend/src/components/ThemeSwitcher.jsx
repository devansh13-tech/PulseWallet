import React from 'react';

export const THEMES = [
  { id: 'money', label: 'Wealth & Gold', icon: '🪙', color: '#10b981' },
  { id: 'stock', label: 'Cyber Stock', icon: '📈', color: '#00f59b' },
  { id: 'nse', label: 'Dalal St NSE', icon: '🇮🇳', color: '#f97316' },
  { id: 'bank', label: 'Titanium Bank', icon: '🏛️', color: '#38bdf8' },
];

export default function ThemeSwitcher({
  currentTheme,
  onThemeChange,
  particlesEnabled,
  onToggleParticles,
  particlesSubtle = true,
  onToggleSubtle,
}) {
  return (
    <div className="theme-switcher-group" aria-label="Dashboard Theme Selector">
      <div className="theme-pills">
        {THEMES.map((t) => {
          const isActive = currentTheme === t.id;
          return (
            <button
              key={t.id}
              type="button"
              className={`theme-pill-btn ${isActive ? 'active' : ''}`}
              onClick={() => onThemeChange(t.id)}
              title={`Switch to ${t.label} Theme`}
              style={{
                borderColor: isActive ? t.color : 'transparent',
              }}
            >
              <span className="theme-pill-icon">{t.icon}</span>
              <span className="theme-pill-label">{t.label}</span>
            </button>
          );
        })}
      </div>

      <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
        <button
          type="button"
          className={`particles-toggle-btn ${particlesEnabled ? 'active' : ''}`}
          onClick={onToggleParticles}
          title={particlesEnabled ? 'Disable floating animated particles' : 'Enable floating animated particles'}
        >
          <span>{particlesEnabled ? '✨ Float: ON' : '💤 Float: OFF'}</span>
        </button>

        {particlesEnabled && onToggleSubtle && (
          <button
            type="button"
            className={`particles-toggle-btn ${particlesSubtle ? 'active' : ''}`}
            onClick={onToggleSubtle}
            title={particlesSubtle ? 'Switch to Dynamic full intensity animation' : 'Switch to Subtle clean trust background'}
            style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem' }}
          >
            <span>{particlesSubtle ? '🛡️ Subtle' : '⚡ Dynamic'}</span>
          </button>
        )}
      </div>
    </div>
  );
}
