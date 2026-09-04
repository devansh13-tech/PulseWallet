/**
 * Centralized API client for PulseWallet.
 *
 * Reads the backend URL from the VITE_API_BASE_URL environment variable
 * with a sensible localhost fallback. Every authenticated request attaches
 * the stored JWT as a Bearer token. 401 responses clear the token and
 * redirect to the login page.
 */

const BASE_URL =
  (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(
    /\/$/,
    '',
  );

const TOKEN_KEY = 'pw_token';

// ---------------------------------------------------------------------------
// Token helpers (isolated here so nothing else touches localStorage directly)
// ---------------------------------------------------------------------------

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// ---------------------------------------------------------------------------
// Core request function
// ---------------------------------------------------------------------------

/**
 * @param {string}  path     – e.g. "/api/auth/login"
 * @param {object}  options  – { method, body, params, auth }
 *   - auth defaults to true; set false for public endpoints
 *   - params is a plain object turned into query-string params
 * @returns {Promise<any>}   – the unwrapped `data` field from ApiResponse
 * @throws {ApiError}        – { message, fieldErrors, status }
 */
async function request(path, { method = 'GET', body, params, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };

  if (auth) {
    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  let url = `${BASE_URL}${path}`;
  if (params) {
    const searchParams = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, String(value));
      }
    }
    const qs = searchParams.toString();
    if (qs) url += `?${qs}`;
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body != null ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    throw {
      message: 'Network error — please check your connection and try again.',
      fieldErrors: null,
      status: 0,
    };
  }

  // 401 – clear auth and redirect to login
  if (response.status === 401) {
    clearToken();
    localStorage.removeItem('pw_user');
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    const errorBody = await response.json().catch(() => null);
    throw {
      message: errorBody?.message || 'Session expired — please log in again.',
      fieldErrors: errorBody?.fieldErrors || null,
      status: 401,
    };
  }

  // 204 No Content
  if (response.status === 204) return null;

  // Try to parse JSON
  let json;
  try {
    json = await response.json();
  } catch {
    throw {
      message: 'Unexpected response from server.',
      fieldErrors: null,
      status: response.status,
    };
  }

  // Error responses use the ApiError shape
  if (!response.ok) {
    throw {
      message: json.message || `Request failed (${response.status})`,
      fieldErrors: json.fieldErrors || null,
      status: response.status,
    };
  }

  // Successful ApiResponse envelope — return the data payload
  return json.data !== undefined ? json.data : json;
}

// ---------------------------------------------------------------------------
// Resource-specific API helpers
// ---------------------------------------------------------------------------

/** Auth (public endpoints — auth: false) */
export const authApi = {
  login: (body) => request('/api/auth/login', { method: 'POST', body, auth: false }),
  register: (body) => request('/api/auth/register', { method: 'POST', body, auth: false }),
};

/** Transactions */
export const transactionApi = {
  list: (params) => request('/api/transactions', { params }),
  get: (id) => request(`/api/transactions/${id}`),
  create: (body) => request('/api/transactions', { method: 'POST', body }),
  update: (id, body) => request(`/api/transactions/${id}`, { method: 'PUT', body }),
  delete: (id) => request(`/api/transactions/${id}`, { method: 'DELETE' }),
};

/** Categories */
export const categoryApi = {
  list: () => request('/api/categories'),
  get: (id) => request(`/api/categories/${id}`),
  create: (body) => request('/api/categories', { method: 'POST', body }),
  update: (id, body) => request(`/api/categories/${id}`, { method: 'PUT', body }),
  delete: (id) => request(`/api/categories/${id}`, { method: 'DELETE' }),
};

/** Budgets */
export const budgetApi = {
  list: () => request('/api/budgets'),
  get: (id) => request(`/api/budgets/${id}`),
  create: (body) => request('/api/budgets', { method: 'POST', body }),
  update: (id, body) => request(`/api/budgets/${id}`, { method: 'PUT', body }),
  delete: (id) => request(`/api/budgets/${id}`, { method: 'DELETE' }),
  plan: (body) => request('/api/budgets/plan', { method: 'POST', body }),
};

/** Dashboard summary */
export const dashboardApi = {
  summary: () => request('/api/dashboard-summary'),
};

/** Financial summary */
export const summaryApi = {
  get: (params) => request('/api/transactions/summary', { params }),
};

/** Forecast */
export const forecastApi = {
  get: () => request('/api/forecast'),
};

/** Advisory */
export const advisoryApi = {
  get: () => request('/api/advisory'),
};

/** Notifications */
export const notificationApi = {
  list: () => request('/api/notifications'),
  unread: () => request('/api/notifications/unread'),
  unreadCount: () => request('/api/notifications/unread/count'),
  markRead: (id) => request(`/api/notifications/${id}/read`, { method: 'PATCH' }),
};

/** Exchange rates */
export const exchangeRateApi = {
  rates: (params) => request('/api/exchange-rates', { params }),
};

/** Fraud detection */
export const fraudApi = {
  check: (body) => request('/api/fraud/check', { method: 'POST', body }),
};
