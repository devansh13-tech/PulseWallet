import { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { getToken, setToken, clearToken } from '../lib/api';

const TOKEN_KEY = 'pw_token';
const USER_KEY = 'pw_user';

const AuthContext = createContext(null);

function loadUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * Provides auth state and actions to the component tree.
 *
 * Token storage is fully isolated here — nothing else should read/write
 * localStorage auth keys directly.
 */
export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => getToken());
  const [user, setUser] = useState(() => loadUser());

  const login = useCallback((authResponse) => {
    // authResponse matches AuthResponse: { token, tokenType, expiresInSeconds, user }
    setToken(authResponse.token);
    setTokenState(authResponse.token);
    localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
    setUser(authResponse.user);
  }, []);

  const logout = useCallback(() => {
    clearToken();
    localStorage.removeItem(USER_KEY);
    setTokenState(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: !!token,
      login,
      logout,
    }),
    [token, user, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return ctx;
}

export default AuthContext;
