import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { authApi } from '../api/authApi';
import type { User, LoginRequest, RegisterRequest } from '../types';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // On mount, if token exists, fetch user profile
  useEffect(() => {
    const token = localStorage.getItem('customerToken');
    if (token) {
      authApi
        .getProfile()
        .then((profile) => setUser(profile))
        .catch(() => {
          localStorage.removeItem('customerToken');
          localStorage.removeItem('customerRefreshToken');
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const data: LoginRequest = { username, password };
    const res = await authApi.login(data);
    localStorage.setItem('customerToken', res.accessToken);
    if (res.refreshToken) {
      localStorage.setItem('customerRefreshToken', res.refreshToken);
    }
    setUser(res.user);
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    const res = await authApi.register(data);
    localStorage.setItem('customerToken', res.accessToken);
    if (res.refreshToken) {
      localStorage.setItem('customerRefreshToken', res.refreshToken);
    }
    setUser(res.user);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('customerToken');
    localStorage.removeItem('customerRefreshToken');
    setUser(null);
  }, []);

  const updateUser = useCallback((updated: User) => {
    setUser(updated);
  }, []);

  return (
    <AuthContext.Provider
      value={{ isAuthenticated: !!user, isLoading, user, login, register, logout, updateUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
