'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User } from './types';
import { api } from './api';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  loginWithGitHub: () => Promise<void>;
  linkGitHub: () => Promise<void>;
  handleGitHubCallback: (code: string, isLinking?: boolean) => Promise<void>;
  unlinkGitHub: () => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = api.getToken();
      if (token) {
        try {
          const userData = await api.getCurrentUser();
          setUser(userData);
        } catch {
          api.logout();
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await api.login(email, password);
    setUser(response.user);
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await api.register(name, email, password);
    setUser(response.user);
  };

  const loginWithGitHub = async () => {
    const url = await api.getGitHubLoginUrl();
    window.location.href = url;
  };

  const linkGitHub = async () => {
    // Store a flag so callback knows this is for linking, not login
    if (typeof window !== 'undefined') {
      localStorage.setItem('github_action', 'link');
    }
    const url = await api.getGitHubLinkUrl();
    window.location.href = url;
  };

  const handleGitHubCallback = async (code: string, isLinking?: boolean) => {
    // Check if this is a link action
    const action = typeof window !== 'undefined' ? localStorage.getItem('github_action') : null;
    const shouldLink = isLinking || action === 'link';
    
    if (shouldLink && user) {
      // Link to existing account
      const updatedUser = await api.linkGitHub(code);
      setUser(updatedUser);
    } else {
      // Login/register with GitHub
      const response = await api.handleGitHubCallback(code);
      setUser(response.user);
    }
    
    // Clear the action flag
    if (typeof window !== 'undefined') {
      localStorage.removeItem('github_action');
    }
  };

  const unlinkGitHub = async () => {
    const updatedUser = await api.unlinkGitHub();
    setUser(updatedUser);
  };

  const logout = () => {
    api.logout();
    setUser(null);
  };

  const refreshUser = async () => {
    const userData = await api.getCurrentUser();
    setUser(userData);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        register,
        loginWithGitHub,
        linkGitHub,
        handleGitHubCallback,
        unlinkGitHub,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
