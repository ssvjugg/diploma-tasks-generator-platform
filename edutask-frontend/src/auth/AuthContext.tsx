import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { configureApiAuth } from '../api/client';
import { registerCurrentUser } from '../api/users';
import type { UserProfile } from '../types/user';
import {
  clearAuth,
  getAccessToken,
  getStoredUser,
  handleAuthCallback,
  login as redirectToLogin,
  logout as redirectToLogout,
  type AuthenticatedUser,
} from './keycloakClient';

type AuthContextValue = {
  isInitializing: boolean;
  isAuthenticated: boolean;
  authError: string | null;
  user: AuthenticatedUser | null;
  profile: UserProfile | null;
  login: () => Promise<void>;
  logout: () => void;
  hasRole: (role: UserProfile['role']) => boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [isInitializing, setIsInitializing] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const [user, setUser] = useState<AuthenticatedUser | null>(() => getStoredUser());
  const [profile, setProfile] = useState<UserProfile | null>(null);

  const resetAuth = useCallback(() => {
    clearAuth();
    setUser(null);
    setProfile(null);
  }, []);

  const login = useCallback(async () => {
    setAuthError(null);
    try {
      await redirectToLogin();
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Не удалось перейти к Keycloak.');
    }
  }, []);

  useEffect(() => {
    configureApiAuth({
      getAccessToken,
      onUnauthorized: resetAuth,
    });
  }, [resetAuth]);

  useEffect(() => {
    let isActive = true;

    async function initializeAuth() {
      setIsInitializing(true);
      setAuthError(null);

      try {
        const currentUser = await handleAuthCallback();
        if (!isActive) {
          return;
        }

        setUser(currentUser);

        if (currentUser) {
          const currentProfile = await registerCurrentUser();
          if (isActive) {
            setProfile(currentProfile);
          }
        }
      } catch (error) {
        resetAuth();
        if (isActive) {
          setAuthError(error instanceof Error ? error.message : 'Не удалось выполнить вход.');
        }
      } finally {
        if (isActive) {
          setIsInitializing(false);
        }
      }
    }

    void initializeAuth();

    return () => {
      isActive = false;
    };
  }, [resetAuth]);

  const value = useMemo<AuthContextValue>(() => ({
    isInitializing,
    isAuthenticated: Boolean(user),
    authError,
    user,
    profile,
    login,
    logout: redirectToLogout,
    hasRole: (role) => user?.roles.includes(role) ?? false,
  }), [authError, isInitializing, login, profile, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return value;
}
