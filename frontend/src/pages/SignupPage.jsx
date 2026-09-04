import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import './auth.css';

export default function SignupPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  function validate() {
    const next = {};
    const name = form.name.trim();
    if (!name) {
      next.name = 'Name is required';
    } else if (name.length > 120) {
      next.name = 'Name must be 120 characters or fewer';
    }

    const email = form.email.trim();
    if (!email) {
      next.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      next.email = 'Enter a valid email address';
    } else if (email.length > 255) {
      next.email = 'Email must be 255 characters or fewer';
    }

    if (!form.password) {
      next.password = 'Password is required';
    } else if (form.password.length < 8) {
      next.password = 'Password must be at least 8 characters';
    } else if (form.password.length > 72) {
      next.password = 'Password must be 72 characters or fewer';
    }

    return next;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setApiError('');

    const fieldErrors = validate();
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setLoading(true);
    try {
      const data = await authApi.register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      login(data);
      navigate('/', { replace: true });
    } catch (err) {
      if (err.fieldErrors) {
        setErrors(err.fieldErrors);
      }
      setApiError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  function onChange(field) {
    return (e) => {
      setForm((prev) => ({ ...prev, [field]: e.target.value }));
      if (errors[field]) setErrors((prev) => ({ ...prev, [field]: '' }));
    };
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="brand-mark">P</div>
          <h1>PulseWallet</h1>
        </div>

        <h2>Create your account</h2>
        <p className="subtitle">Start managing your finances</p>

        {apiError && <div className="auth-error" role="alert">{apiError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="signup-name">Full name</label>
            <input
              id="signup-name"
              type="text"
              autoComplete="name"
              value={form.name}
              onChange={onChange('name')}
              className={errors.name ? 'field-error' : ''}
              placeholder="Jane Doe"
            />
            {errors.name && <p className="error-text">{errors.name}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="signup-email">Email</label>
            <input
              id="signup-email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={onChange('email')}
              className={errors.email ? 'field-error' : ''}
              placeholder="you@example.com"
            />
            {errors.email && <p className="error-text">{errors.email}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="signup-password">Password</label>
            <input
              id="signup-password"
              type="password"
              autoComplete="new-password"
              value={form.password}
              onChange={onChange('password')}
              className={errors.password ? 'field-error' : ''}
              placeholder="Minimum 8 characters"
            />
            {errors.password && <p className="error-text">{errors.password}</p>}
          </div>

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
