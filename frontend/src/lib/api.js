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
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
  const headers = isFormData ? {} : { 'Content-Type': 'application/json' };

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
      body: isFormData ? body : (body != null ? JSON.stringify(body) : undefined),
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

/** Financial goals */
export const goalApi = {
  list: () => request('/api/goals'),
  get: (id) => request(`/api/goals/${id}`),
  create: (body) => request('/api/goals', { method: 'POST', body }),
  update: (id, body) => request(`/api/goals/${id}`, { method: 'PUT', body }),
  delete: (id) => request(`/api/goals/${id}`, { method: 'DELETE' }),
};

/** Dashboard summary */
export const dashboardApi = {
  summary: () => request('/api/dashboard-summary'),
};

/** Dynamic, authenticated rules-engine recommendations */
export const recommendationApi = {
  get: () => request('/api/recommendations'),
};

/** Live Indian Market Indices (Sensex & Nifty), Gainers/Losers, and AI context */
export const marketApi = {
  indices: (range = '1D') => request('/api/market/indices', { params: { range } }),
  movers: () => request('/api/market/movers'),
};

/** Savings and investment intelligence */
export const financialPlanApi = {
  get: () => request('/api/financial-plan'),
  profile: () => request('/api/investment-profile'),
  save: (body) => request('/api/investment-profile', { method: 'PUT', body }),
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

/** Credit cards */
export const creditCardApi = {
  list: () => request('/api/credit-cards'),
  get: (id) => request(`/api/credit-cards/${id}`),
  create: (body) => request('/api/credit-cards', { method: 'POST', body }),
  update: (id, body) => request(`/api/credit-cards/${id}`, { method: 'PUT', body }),
  delete: (id) => request(`/api/credit-cards/${id}`, { method: 'DELETE' }),
  intelligence: () => request('/api/credit-cards/intelligence'),
  purchaseCheck: (body) => request('/api/credit-cards/purchase-check', { method: 'POST', body }),
  identify: (body) => request('/api/credit-cards/identify', { method: 'POST', body }),
  parseStatement: (formData) => request('/api/credit-cards/statements/parse', { method: 'POST', body: formData }),
  confirmStatement: (body) => request('/api/credit-cards/statements/confirm', { method: 'POST', body }),
  benefits: (id) => request(`/api/credit-cards/${id}/benefits`),
  bankSyncStatus: () => request('/api/credit-cards/bank-sync/status'),
  bankSyncConsent: (body) => request('/api/credit-cards/bank-sync/consent', { method: 'POST', body }),
};

/** Linked payment account connection records */
export const linkedPaymentAccountApi = {
  list: () => request('/api/linked-accounts'),
  get: (id) => request(`/api/linked-accounts/${id}`),
  create: (body) => request('/api/linked-accounts', { method: 'POST', body }),
  updateBalance: (id, balance) => request(`/api/linked-accounts/${id}/balance`, { method: 'PATCH', body: { balance } }),
  disconnect: (id) => request(`/api/linked-accounts/${id}/disconnect`, { method: 'PATCH' }),
  delete: (id) => request(`/api/linked-accounts/${id}`, { method: 'DELETE' }),
};

/** Exchange rates */
export const exchangeRateApi = {
  rates: (params) => request('/api/exchange-rates', { params }),
};

/** Fraud detection */
export const fraudApi = {
  check: (body) => request('/api/fraud/check', { method: 'POST', body }),
};

/** Bank & Transaction Ingestion (AA, SMS Parser, Auto-Categorization) */
export const bankIngestionApi = {
  parseSms: (body) => request('/api/bank-ingestion/parse-sms', { method: 'POST', body }),
  accountAggregatorConsent: (body) => request('/api/bank-ingestion/account-aggregator/consent', { method: 'POST', body }),
  categoryFeedback: (body) => request('/api/bank-ingestion/category-feedback', { method: 'POST', body }),
  batchImport: (body) => request('/api/bank-ingestion/import-transactions', { method: 'POST', body }),
};

/** Advanced Fraud Security Center */
export const advancedFraudApi = {
  getSecurityCenter: () => request('/api/fraud-center/security-center'),
  scanPhishing: (body) => request('/api/fraud-center/scan-phishing', { method: 'POST', body }),
  takeAction: (body) => request('/api/fraud-center/action', { method: 'POST', body }),
  getInvestigationReport: () => request('/api/fraud-center/investigation-report'),
};

/** Conversational AI Financial Assistant */
export const aiAssistantApi = {
  chat: (body) => request('/api/ai/assistant/chat', { method: 'POST', body }),
};

/** Proactive Insights, CIBIL, Debt Payoff, Net Worth, Tax */
export const financialHubApi = {
  getProactiveInsights: () => request('/api/financial-hub/proactive-insights'),
  getCibilScore: () => request('/api/financial-hub/cibil-score'),
  getDebtPayoff: () => request('/api/financial-hub/debt-payoff'),
  getNetWorth: () => request('/api/financial-hub/net-worth'),
  getTaxTracker: () => request('/api/financial-hub/tax-tracker'),
};

