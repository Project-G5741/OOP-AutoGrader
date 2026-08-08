import React, { useState, useMemo, useCallback } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  "901862485743-on3umlivpedse7hosvjtjqdpqr57s69i.apps.googleusercontent.com";
import { ThemeProvider } from "./context/ThemeContext";
import Login from "./pages/Login";
import ResetPasswordUI from "./pages/ResetPasswordUI";
import LecturerDashboard from "./pages/LecturerDashboard";
import StudentDashboard from "./pages/StudentDashboard";
import RequireRole from "./components/auth/RequireRole";
import { defaultDashboardPath, ROUTES } from "./utils/authRoutes";

function readResetTokenFromUrl() {
  return new URLSearchParams(window.location.search).get("resetToken");
}

function clearResetTokenFromUrl() {
  const url = new URL(window.location.href);
  url.searchParams.delete("resetToken");
  const next = url.pathname + (url.search ? url.search : "") + url.hash;
  window.history.replaceState({}, "", next);
}

function readStoredUser() {
  try {
    const saved = sessionStorage.getItem("user");
    if (!saved) return null;
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed?.roles) ? parsed : null;
  } catch {
    return null;
  }
}

function AuthenticatedLanding({ user }) {
  return <Navigate to={defaultDashboardPath(user.roles)} replace />;
}

export default function App() {
  const initialResetToken = useMemo(() => readResetTokenFromUrl(), []);
  const navigate = useNavigate();
  const [resetToken, setResetToken] = useState(initialResetToken);
  const [loginMessage, setLoginMessage] = useState(null);
  const [user, setUser] = useState(readStoredUser);

  const handleLoginSuccess = useCallback((data) => {
    sessionStorage.setItem("accessToken", data.accessToken);
    sessionStorage.setItem("user", JSON.stringify(data));
    setUser(data);
    navigate(defaultDashboardPath(data.roles));
  }, [navigate]);

  const handleLogout = useCallback(() => {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("user");
    setUser(null);
    navigate(ROUTES.login);
  }, [navigate]);

  const handleResetComplete = () => {
    clearResetTokenFromUrl();
    setResetToken(null);
    setLoginMessage("Password updated. Sign in with your new password.");
    navigate(ROUTES.login);
  };

  if (resetToken && !user) {
    return (
      <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
        <ThemeProvider>
          <ResetPasswordUI token={resetToken} onComplete={handleResetComplete} />
        </ThemeProvider>
      </GoogleOAuthProvider>
    );
  }

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <ThemeProvider>
        <Routes>
          <Route
            path={ROUTES.login}
            element={
              user ? (
                <AuthenticatedLanding user={user} />
              ) : (
                <Login
                  onLoginSuccess={handleLoginSuccess}
                  loginMessage={loginMessage}
                  onDismissLoginMessage={() => setLoginMessage(null)}
                />
              )
            }
          />

          <Route
            path={ROUTES.lecturerDashboard}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={user} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerGrading}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={user} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerUsers}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={user} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerSolution}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={user} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerReport}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={user} onLogout={handleLogout} />
              </RequireRole>
            }
          />

          <Route
            path={ROUTES.studentDashboard}
            element={
              <RequireRole anyOf={["STUDENT"]}>
                <StudentDashboard user={user} onLogout={handleLogout} view="dashboard" />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.studentHistory}
            element={
              <RequireRole anyOf={["STUDENT"]}>
                <StudentDashboard user={user} onLogout={handleLogout} view="history" />
              </RequireRole>
            }
          />

          <Route
            path="*"
            element={
              user ? (
                <AuthenticatedLanding user={user} />
              ) : (
                <Navigate to={ROUTES.login} replace />
              )
            }
          />
        </Routes>
      </ThemeProvider>
    </GoogleOAuthProvider>
  );
}
