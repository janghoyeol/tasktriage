import { createContext, useContext, useState, type ReactNode } from 'react';
import { api, getToken, setToken } from '../api/client';
import type { TokenResponse, UserResponse } from '../api/types';

interface AuthContextValue {
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<UserResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => getToken() !== null);

  async function login(email: string, password: string): Promise<void> {
    const response = await api.post<TokenResponse>('/api/auth/login', { email, password }, { auth: false });
    setToken(response.accessToken);
    setIsAuthenticated(true);
  }

  async function register(name: string, email: string, password: string): Promise<UserResponse> {
    return api.post<UserResponse>('/api/auth/register', { name, email, password }, { auth: false });
  }

  function logout(): void {
    setToken(null);
    setIsAuthenticated(false);
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, register, logout }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
