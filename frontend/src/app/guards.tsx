import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../shared/auth/AuthContext";
import { PageLoader } from "../shared/ui";
import type { Role } from "../shared/api/types";

export function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <PageLoader label="Проверяем сессию…" />;
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return <Outlet />;
}

export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

export function PublicOnly({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return <PageLoader label="Загрузка…" />;
  }
  if (user) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
