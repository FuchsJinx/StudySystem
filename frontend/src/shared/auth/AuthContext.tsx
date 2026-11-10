import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api } from "../api/client";
import { session } from "../api/session";
import type { AuthResponse, UserMe } from "../api/types";

interface RegisterStudentPayload {
  email: string;
  password: string;
  fullName: string;
  facultyId: number;
  groupId: number;
}

interface RegisterTeacherPayload {
  email: string;
  password: string;
  fullName: string;
  departmentId: number;
}

interface AuthContextValue {
  user: UserMe | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<UserMe>;
  registerStudent: (payload: RegisterStudentPayload) => Promise<UserMe>;
  registerTeacher: (payload: RegisterTeacherPayload) => Promise<UserMe>;
  logout: () => void;
  refreshUser: () => Promise<UserMe | null>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserMe | null>(null);
  const [loading, setLoading] = useState(true);

  const loadMe = useCallback(async (): Promise<UserMe | null> => {
    if (!session.getToken()) {
      return null;
    }
    try {
      return await api<UserMe>("/auth/me");
    } catch {
      session.clear();
      return null;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    loadMe()
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [loadMe]);

  useEffect(() => {
    const handler = () => setUser(null);
    window.addEventListener("unirate:unauthorized", handler);
    return () => window.removeEventListener("unirate:unauthorized", handler);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<AuthResponse>("/auth/login", {
      method: "POST",
      body: { email, password },
    });
    session.setToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const registerStudent = useCallback(async (payload: RegisterStudentPayload) => {
    const response = await api<AuthResponse>("/auth/register/student", {
      method: "POST",
      body: payload,
    });
    session.setToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const registerTeacher = useCallback(async (payload: RegisterTeacherPayload) => {
    const response = await api<AuthResponse>("/auth/register/teacher", {
      method: "POST",
      body: payload,
    });
    session.setToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const logout = useCallback(() => {
    session.clear();
    setUser(null);
  }, []);

  const refreshUser = useCallback(async () => {
    const me = await loadMe();
    setUser(me);
    return me;
  }, [loadMe]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, registerStudent, registerTeacher, logout, refreshUser }),
    [user, loading, login, registerStudent, registerTeacher, logout, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
