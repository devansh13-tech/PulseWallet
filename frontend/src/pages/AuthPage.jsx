import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import './auth.css';

// Clean SVG Icons (no emojis on auth screen)
function LockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  );
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
    </svg>
  );
}

function PhoneIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
    </svg>
  );
}

export default function AuthPage({ defaultTab = 'login' }) {
  const navigate = useNavigate();
  const { login } = useAuth();

  // Active tab: 'login' | 'signup'
  const [tab, setTab] = useState(defaultTab);

  // Password visibility toggles
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showSignupPassword, setShowSignupPassword] = useState(false);

  // Feedback states
  const [apiError, setApiError] = useState('');
  const [apiSuccess, setApiSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // Dialog/Modal states
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotSubmitted, setForgotSubmitted] = useState(false);

  const [showPhoneModal, setShowPhoneModal] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [phoneOtpStep, setPhoneOtpStep] = useState('phone'); // 'phone' | 'otp'
  const [phoneOtpCode, setPhoneOtpCode] = useState('123456');

  // Login form state
  const [loginForm, setLoginForm] = useState({
    email: '',
    password: '',
  });
  const [loginErrors, setLoginErrors] = useState({});

  // Signup multi-step wizard state
  const [signupStep, setSignupStep] = useState(1); // 1: Account, 2: Baseline/Goals, 3: Risk Profile
  const [signupForm, setSignupForm] = useState({
    name: '',
    email: '',
    password: '',
    monthlyIncome: '75000',
    topGoal: 'Emergency Reserve',
    targetSavings: '20000',
    riskProfile: 'MODERATE', // CONSERVATIVE, MODERATE, AGGRESSIVE
  });
  const [signupErrors, setSignupErrors] = useState({});

  // Tab change handler
  function handleTabChange(nextTab) {
    setTab(nextTab);
    setApiError('');
    setApiSuccess('');
    window.history.replaceState(null, '', nextTab === 'signup' ? '/signup' : '/login');
  }

  // --- Login Handling ---
  async function handleLoginSubmit(e) {
    e.preventDefault();
    setApiError('');
    const errors = {};
    if (!loginForm.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(loginForm.email.trim())) {
      errors.email = 'Enter a valid email address';
    }
    if (!loginForm.password) {
      errors.password = 'Password is required';
    }
    setLoginErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setLoading(true);
    try {
      const data = await authApi.login({
        email: loginForm.email.trim(),
        password: loginForm.password,
      });
      login(data);
      navigate('/', { replace: true });
    } catch (err) {
      if (err.fieldErrors) setLoginErrors(err.fieldErrors);
      setApiError(err.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  }

  // Demo Google Sign-In Simulation
  async function handleGoogleLogin() {
    setLoading(true);
    setApiError('');
    try {
      const data = await authApi.login({
        email: 'testuser@pulsewallet.com',
        password: 'Password123!',
      });
      login(data);
      navigate('/', { replace: true });
    } catch (err) {
      setApiError('Google sign-in simulation: ensure test user is active or enter credentials.');
    } finally {
      setLoading(false);
    }
  }

  // Demo Phone OTP Verify
  async function handlePhoneOtpVerify(e) {
    e.preventDefault();
    setLoading(true);
    setApiError('');
    try {
      const data = await authApi.login({
        email: 'testuser@pulsewallet.com',
        password: 'Password123!',
      });
      login(data);
      setShowPhoneModal(false);
      navigate('/', { replace: true });
    } catch (err) {
      setApiError('Phone OTP verification failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  // Forgot password submit
  function handleForgotSubmit(e) {
    e.preventDefault();
    if (!forgotEmail.trim()) return;
    setForgotSubmitted(true);
  }

  // --- Signup Wizard Step Validation ---
  function validateSignupStep1() {
    const errors = {};
    if (!signupForm.name.trim()) {
      errors.name = 'Full name is required';
    }
    if (!signupForm.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(signupForm.email.trim())) {
      errors.email = 'Enter a valid email address';
    }
    if (!signupForm.password) {
      errors.password = 'Password is required';
    } else if (signupForm.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    setSignupErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSignupNext() {
    if (signupStep === 1) {
      if (!validateSignupStep1()) return;
      setSignupStep(2);
    } else if (signupStep === 2) {
      setSignupStep(3);
    }
  }

  async function handleSignupFinalSubmit(e) {
    e.preventDefault();
    setApiError('');
    setLoading(true);

    try {
      const data = await authApi.register({
        name: signupForm.name.trim(),
        email: signupForm.email.trim(),
        password: signupForm.password,
      });
      login(data);
      navigate('/', { replace: true });
    } catch (err) {
      if (err.fieldErrors) setSignupErrors(err.fieldErrors);
      setApiError(err.message || 'Registration failed. Try again.');
      setSignupStep(1);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Brand Logo & Name */}
        <div className="auth-brand">
          <div className="brand-mark">P</div>
          <h1>PulseWallet</h1>
        </div>

        {/* Tab Switcher: Sign in / Create Account */}
        <div className="auth-tabs" role="tablist" aria-label="Authentication tabs">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'login'}
            className={`auth-tab-btn ${tab === 'login' ? 'active' : ''}`}
            onClick={() => handleTabChange('login')}
          >
            Sign in
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'signup'}
            className={`auth-tab-btn ${tab === 'signup' ? 'active' : ''}`}
            onClick={() => handleTabChange('signup')}
          >
            Create account
          </button>
        </div>

        {/* Feedback alerts */}
        {apiError && <div className="auth-error" role="alert">{apiError}</div>}
        {apiSuccess && <div className="auth-success" role="status">{apiSuccess}</div>}

        {/* =========================================================
            TAB 1: SIGN IN
            ========================================================= */}
        {tab === 'login' && (
          <div>
            <h2>Welcome back</h2>
            <p className="subtitle">Sign in to your financial account</p>

            <form onSubmit={handleLoginSubmit} noValidate>
              <div className="form-group">
                <label htmlFor="auth-email">Email address</label>
                <input
                  id="auth-email"
                  type="email"
                  autoComplete="email"
                  value={loginForm.email}
                  onChange={(e) => {
                    setLoginForm((prev) => ({ ...prev, email: e.target.value }));
                    if (loginErrors.email) setLoginErrors((prev) => ({ ...prev, email: '' }));
                  }}
                  className={loginErrors.email ? 'field-error' : ''}
                  placeholder="name@example.com"
                  autoFocus
                />
                {loginErrors.email && <p className="error-text">{loginErrors.email}</p>}
              </div>

              <div className="form-group">
                <div className="label-with-link">
                  <label htmlFor="auth-password">Password</label>
                  <button
                    type="button"
                    className="label-link"
                    onClick={() => {
                      setForgotSubmitted(false);
                      setForgotEmail(loginForm.email);
                      setShowForgotModal(true);
                    }}
                  >
                    Forgot password?
                  </button>
                </div>
                <div className="input-with-action">
                  <input
                    id="auth-password"
                    type={showLoginPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={loginForm.password}
                    onChange={(e) => {
                      setLoginForm((prev) => ({ ...prev, password: e.target.value }));
                      if (loginErrors.password) setLoginErrors((prev) => ({ ...prev, password: '' }));
                    }}
                    className={loginErrors.password ? 'field-error' : ''}
                    placeholder="••••••••"
                  />
                  <button
                    type="button"
                    className="toggle-pwd-btn"
                    onClick={() => setShowLoginPassword((v) => !v)}
                    title={showLoginPassword ? 'Hide password' : 'Show password'}
                    aria-label={showLoginPassword ? 'Hide password' : 'Show password'}
                  >
                    {showLoginPassword ? <EyeOffIcon /> : <EyeIcon />}
                  </button>
                </div>
                {loginErrors.password && <p className="error-text">{loginErrors.password}</p>}
              </div>

              <button type="submit" className="auth-submit" disabled={loading}>
                {loading ? 'Signing in…' : 'Sign in'}
              </button>
            </form>

            <div className="auth-divider">Or continue with</div>

            <div className="auth-alt-row">
              <button
                type="button"
                className="auth-alt-btn"
                onClick={handleGoogleLogin}
                title="Sign in with Google"
                disabled={loading}
              >
                <GoogleIcon />
                <span>Google</span>
              </button>
              <button
                type="button"
                className="auth-alt-btn"
                onClick={() => {
                  setPhoneOtpStep('phone');
                  setShowPhoneModal(true);
                }}
                title="Sign in with Indian Phone OTP"
                disabled={loading}
              >
                <PhoneIcon />
                <span>Phone OTP</span>
              </button>
            </div>

            <p className="auth-footer">
              Don&apos;t have an account?
              <button
                type="button"
                className="auth-switch-link"
                onClick={() => handleTabChange('signup')}
              >
                Create one
              </button>
            </p>
          </div>
        )}

        {/* =========================================================
            TAB 2: CREATE ACCOUNT (MULTI-STEP ONBOARDING WIZARD)
            ========================================================= */}
        {tab === 'signup' && (
          <div>
            <div className="signup-stepper">
              <div className="stepper-header">
                <span className="stepper-badge">
                  {signupStep === 1 && 'Step 1 of 3: Account Credentials'}
                  {signupStep === 2 && 'Step 2 of 3: Financial Baseline'}
                  {signupStep === 3 && 'Step 3 of 3: Risk Profile'}
                </span>
                <span className="stepper-step-title">
                  {signupStep === 1 && 'Credentials'}
                  {signupStep === 2 && 'Cash Flow'}
                  {signupStep === 3 && 'Risk Appetite'}
                </span>
              </div>
              <div className="stepper-progress-track">
                <div
                  className="stepper-progress-fill"
                  style={{
                    width: signupStep === 1 ? '33.3%' : signupStep === 2 ? '66.6%' : '100%',
                  }}
                />
              </div>
            </div>

            {/* Step 1: Account credentials */}
            {signupStep === 1 && (
              <div>
                <h2>Create your account</h2>
                <p className="subtitle">Enter your details to register</p>

                <div className="form-group">
                  <label htmlFor="signup-name">Full name</label>
                  <input
                    id="signup-name"
                    type="text"
                    autoComplete="name"
                    value={signupForm.name}
                    onChange={(e) => {
                      setSignupForm((prev) => ({ ...prev, name: e.target.value }));
                      if (signupErrors.name) setSignupErrors((prev) => ({ ...prev, name: '' }));
                    }}
                    className={signupErrors.name ? 'field-error' : ''}
                    placeholder="Aditi Sharma"
                    autoFocus
                  />
                  {signupErrors.name && <p className="error-text">{signupErrors.name}</p>}
                </div>

                <div className="form-group">
                  <label htmlFor="signup-email">Email address</label>
                  <input
                    id="signup-email"
                    type="email"
                    autoComplete="email"
                    value={signupForm.email}
                    onChange={(e) => {
                      setSignupForm((prev) => ({ ...prev, email: e.target.value }));
                      if (signupErrors.email) setSignupErrors((prev) => ({ ...prev, email: '' }));
                    }}
                    className={signupErrors.email ? 'field-error' : ''}
                    placeholder="aditi@example.com"
                  />
                  {signupErrors.email && <p className="error-text">{signupErrors.email}</p>}
                </div>

                <div className="form-group">
                  <label htmlFor="signup-password">Password</label>
                  <div className="input-with-action">
                    <input
                      id="signup-password"
                      type={showSignupPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      value={signupForm.password}
                      onChange={(e) => {
                        setSignupForm((prev) => ({ ...prev, password: e.target.value }));
                        if (signupErrors.password) setSignupErrors((prev) => ({ ...prev, password: '' }));
                      }}
                      className={signupErrors.password ? 'field-error' : ''}
                      placeholder="Minimum 8 characters"
                    />
                    <button
                      type="button"
                      className="toggle-pwd-btn"
                      onClick={() => setShowSignupPassword((v) => !v)}
                      title={showSignupPassword ? 'Hide password' : 'Show password'}
                      aria-label={showSignupPassword ? 'Hide password' : 'Show password'}
                    >
                      {showSignupPassword ? <EyeOffIcon /> : <EyeIcon />}
                    </button>
                  </div>
                  {signupErrors.password && <p className="error-text">{signupErrors.password}</p>}
                </div>

                <button
                  type="button"
                  className="auth-submit"
                  onClick={handleSignupNext}
                >
                  Continue to Financial Profile →
                </button>

                <p className="auth-footer">
                  Already have an account?
                  <button
                    type="button"
                    className="auth-switch-link"
                    onClick={() => handleTabChange('login')}
                  >
                    Sign in
                  </button>
                </p>
              </div>
            )}

            {/* Step 2: Income and goals */}
            {signupStep === 2 && (
              <div>
                <h2>Financial Baseline</h2>
                <p className="subtitle">Helps Pulse AI tailor your budget and advisory</p>

                <div className="form-group">
                  <label htmlFor="signup-income">Monthly In-Hand Income (₹)</label>
                  <input
                    id="signup-income"
                    type="number"
                    step="1000"
                    value={signupForm.monthlyIncome}
                    onChange={(e) => setSignupForm((prev) => ({ ...prev, monthlyIncome: e.target.value }))}
                    placeholder="e.g. 75000"
                  />
                </div>

                <div className="form-group">
                  <label>Primary Financial Goal</label>
                  <div className="wizard-selection-grid">
                    {[
                      { id: 'Emergency Reserve', label: '🛡️ Emergency Reserve (₹2 Lakhs)', desc: 'Peace of mind buffer' },
                      { id: 'Wealth Creation', label: '📈 Wealth Creation (₹10 Lakhs)', desc: 'Long-term equity & SIP growth' },
                      { id: 'Home Downpayment', label: '🏠 Home Downpayment (₹15 Lakhs)', desc: 'Real estate milestone' },
                      { id: 'Debt Free Plan', label: '💳 Debt Clearance', desc: 'Snowball EMI payoff' },
                    ].map((g) => (
                      <div
                        key={g.id}
                        className={`wizard-choice-card ${signupForm.topGoal === g.id ? 'selected' : ''}`}
                        onClick={() => setSignupForm((prev) => ({ ...prev, topGoal: g.id }))}
                      >
                        <div>
                          <div className="choice-title">{g.label}</div>
                          <div className="choice-desc">{g.desc}</div>
                        </div>
                        <div className="choice-radio">
                          {signupForm.topGoal === g.id && <div className="choice-radio-inner" />}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="wizard-nav-actions">
                  <button
                    type="button"
                    className="wizard-back-btn"
                    onClick={() => setSignupStep(1)}
                  >
                    ← Back
                  </button>
                  <button
                    type="button"
                    className="wizard-next-btn"
                    onClick={handleSignupNext}
                  >
                    Continue to Risk Profile →
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Risk profile */}
            {signupStep === 3 && (
              <div>
                <h2>Investor Risk Profile</h2>
                <p className="subtitle">Determines asset allocation and mutual fund picks</p>

                <div className="wizard-selection-grid">
                  {[
                    {
                      id: 'CONSERVATIVE',
                      title: 'Conservative (Low Risk)',
                      desc: 'Priority on capital safety. FDs, Liquid & Arbitrage funds.',
                    },
                    {
                      id: 'MODERATE',
                      title: 'Moderate (Balanced)',
                      desc: 'Recommended: 60% Large Cap Index funds, 40% Debt & Gold.',
                    },
                    {
                      id: 'AGGRESSIVE',
                      title: 'Aggressive (High Growth)',
                      desc: 'Focus on wealth compounders: Mid & Small Cap mutual funds.',
                    },
                  ].map((r) => (
                    <div
                      key={r.id}
                      className={`wizard-choice-card ${signupForm.riskProfile === r.id ? 'selected' : ''}`}
                      onClick={() => setSignupForm((prev) => ({ ...prev, riskProfile: r.id }))}
                    >
                      <div>
                        <div className="choice-title">{r.title}</div>
                        <div className="choice-desc">{r.desc}</div>
                      </div>
                      <div className="choice-radio">
                        {signupForm.riskProfile === r.id && <div className="choice-radio-inner" />}
                      </div>
                    </div>
                  ))}
                </div>

                <div className="wizard-nav-actions">
                  <button
                    type="button"
                    className="wizard-back-btn"
                    onClick={() => setSignupStep(2)}
                    disabled={loading}
                  >
                    ← Back
                  </button>
                  <button
                    type="button"
                    className="wizard-next-btn"
                    onClick={handleSignupFinalSubmit}
                    disabled={loading}
                  >
                    {loading ? 'Creating account…' : 'Create Account & Launch ✓'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* =========================================================
            INLINE MODAL: FORGOT PASSWORD
            ========================================================= */}
        {showForgotModal && (
          <div className="auth-inline-modal" role="dialog" aria-modal="true">
            <div>
              <div className="modal-header-row">
                <h3>Reset Password</h3>
                <button
                  type="button"
                  className="modal-close-btn"
                  onClick={() => setShowForgotModal(false)}
                >
                  ✕
                </button>
              </div>

              {!forgotSubmitted ? (
                <form onSubmit={handleForgotSubmit}>
                  <p style={{ fontSize: '0.86rem', color: '#64748b', margin: '0 0 16px' }}>
                    Enter your registered email address. We will dispatch a secure 256-bit password recovery link.
                  </p>
                  <div className="form-group">
                    <label htmlFor="forgot-email">Account Email</label>
                    <input
                      id="forgot-email"
                      type="email"
                      required
                      value={forgotEmail}
                      onChange={(e) => setForgotEmail(e.target.value)}
                      placeholder="name@example.com"
                      autoFocus
                    />
                  </div>
                  <button type="submit" className="auth-submit">
                    Send Reset Link
                  </button>
                </form>
              ) : (
                <div style={{ textAlign: 'center', padding: '12px 0' }}>
                  <div style={{ fontSize: '2rem', marginBottom: '8px' }}>✉️</div>
                  <h4 style={{ margin: '0 0 6px', color: '#0f172a' }}>Check your inbox</h4>
                  <p style={{ fontSize: '0.84rem', color: '#64748b' }}>
                    A recovery link has been dispatched to <strong>{forgotEmail}</strong>.
                  </p>
                  <button
                    type="button"
                    className="auth-submit"
                    style={{ marginTop: '14px' }}
                    onClick={() => setShowForgotModal(false)}
                  >
                    Return to Sign In
                  </button>
                </div>
              )}
            </div>
            <div style={{ fontSize: '0.74rem', color: '#94a3b8', textAlign: 'center' }}>
              Protected by PulseWallet Security Protocol
            </div>
          </div>
        )}

        {/* =========================================================
            INLINE MODAL: PHONE OTP SIGN IN
            ========================================================= */}
        {showPhoneModal && (
          <div className="auth-inline-modal" role="dialog" aria-modal="true">
            <div>
              <div className="modal-header-row">
                <h3>Phone OTP Login</h3>
                <button
                  type="button"
                  className="modal-close-btn"
                  onClick={() => setShowPhoneModal(false)}
                >
                  ✕
                </button>
              </div>

              {phoneOtpStep === 'phone' ? (
                <div>
                  <p style={{ fontSize: '0.86rem', color: '#64748b', margin: '0 0 16px' }}>
                    Quick sign-in via Indian mobile number with RBI-compliant SMS OTP verification.
                  </p>
                  <div className="form-group">
                    <label htmlFor="phone-number">Mobile Number (+91)</label>
                    <input
                      id="phone-number"
                      type="tel"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder="98765 43210"
                      autoFocus
                    />
                  </div>
                  <button
                    type="button"
                    className="auth-submit"
                    onClick={() => setPhoneOtpStep('otp')}
                  >
                    Send 6-Digit OTP →
                  </button>
                </div>
              ) : (
                <form onSubmit={handlePhoneOtpVerify}>
                  <p style={{ fontSize: '0.86rem', color: '#64748b', margin: '0 0 14px' }}>
                    Enter the 6-digit verification code sent to +91 {phoneNumber || '98765 43210'}.
                  </p>
                  <div className="form-group">
                    <label htmlFor="otp-code">Verification Code</label>
                    <input
                      id="otp-code"
                      type="text"
                      maxLength={6}
                      value={phoneOtpCode}
                      onChange={(e) => setPhoneOtpCode(e.target.value)}
                      style={{ letterSpacing: '0.3em', textAlign: 'center', fontSize: '1.2rem', fontWeight: 700 }}
                      autoFocus
                    />
                  </div>
                  <button type="submit" className="auth-submit" disabled={loading}>
                    {loading ? 'Verifying…' : 'Verify & Sign In ✓'}
                  </button>
                  <button
                    type="button"
                    className="label-link"
                    style={{ display: 'block', margin: '12px auto 0', textAlign: 'center' }}
                    onClick={() => setPhoneOtpStep('phone')}
                  >
                    Change phone number
                  </button>
                </form>
              )}
            </div>
            <div style={{ fontSize: '0.74rem', color: '#94a3b8', textAlign: 'center' }}>
              Instant verification enabled for Indian Telecom carriers
            </div>
          </div>
        )}

        {/* =========================================================
            TRUST CUES FOOTER
            ========================================================= */}
        <div className="auth-trust-banner">
          <div className="auth-trust-icon">
            <LockIcon />
          </div>
          <div className="auth-trust-text">
            <strong>Bank-grade 256-bit SSL encryption</strong>
            Your financial data is private & never sold • RBI DPDP Act compliant
          </div>
        </div>
      </div>
    </div>
  );
}
