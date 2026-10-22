import React, { useState, useMemo, useCallback, useEffect } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { Routes, Route, Navigate, useNavigate } from "react-router-dom";

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  (import.meta.env.DEV
    ? "901862485743-on3umlivpedse7hosvjtjqdpqr57s69i.apps.googleusercontent.com"
    : "");
import Login from "./pages/Login";
import ResetPasswordUI from "./pages/ResetPasswordUI";
import LecturerDashboard from "./pages/LecturerDashboard";
import StudentDashboard from "./pages/StudentDashboard";
import NoAccessPage from "./pages/NoAccessPage";
import RateLimitedPage from "./pages/RateLimitedPage";
import MaintenancePage from "./pages/MaintenancePage";
import RequireRole from "./components/auth/RequireRole";
import {
  clearAuthSession,
  defaultDashboardPath,
  hasRole,
  normalizeRoleList,
  persistAuthSession,
  readStoredUser,
  ROUTES,
} from "./utils/authRoutes";
import { leavePresence } from "./utils/presence";
import { clearLecturerBootstrapStore } from "./utils/lecturerBootstrapStore";
import { consumeFeedbackRepliesOnLeave } from "./utils/feedbackReplyConsume";
import { isWebMaintenanceMode } from "./utils/maintenanceMode";

function readResetTokenFromUrl() {
  return new URLSearchParams(window.location.search).get("resetToken");
}

function clearResetTokenFromUrl() {
  const url = new URL(window.location.href);
  url.searchParams.delete("resetToken");
  const next = url.pathname + (url.search ? url.search : "") + url.hash;
  window.history.replaceState({}, "", next);
}

function AuthenticatedLanding({ user }) {
  return <Navigate to={defaultDashboardPath(user.roles, user.inCurrentTerm)} replace />;
}

export default function App() {
  const initialResetToken = useMemo(() => readResetTokenFromUrl(), []);
  const navigate = useNavigate();
  const [resetToken, setResetToken] = useState(initialResetToken);
  const [loginMessage, setLoginMessage] = useState(null);
  const sessionUser = readStoredUser();
  const lecturerCanEnter = hasRole(sessionUser?.roles, "LECTURER");

  useEffect(() => {
    if (!isWebMaintenanceMode()) {
      return undefined;
    }
    const user = readStoredUser();
    if (!user || hasRole(user.roles, "LECTURER")) {
      return undefined;
    }
    consumeFeedbackRepliesOnLeave();
    leavePresence();
    clearAuthSession();
    clearLecturerBootstrapStore();
    return undefined;
  }, []);

  const handleLoginSuccess = useCallback((data) => {
    const roles = normalizeRoleList(data.roles);
    if (isWebMaintenanceMode() && !hasRole(roles, "LECTURER")) {
      setLoginMessage("The site is under maintenance. Only lecturers can sign in right now.");
      return;
    }
    const userPayload = { ...data, roles };
    persistAuthSession(data.accessToken, userPayload);
    navigate(defaultDashboardPath(roles, data.inCurrentTerm));
  }, [navigate]);

  const handleLogout = useCallback(() => {
    consumeFeedbackRepliesOnLeave();
    leavePresence();
    clearAuthSession();
    clearLecturerBootstrapStore();
    navigate(ROUTES.login);
  }, [navigate]);

  const handleResetComplete = useCallback(() => {
    clearResetTokenFromUrl();
    setResetToken(null);
    setLoginMessage("Password updated. Sign in with your new password.");
    navigate(ROUTES.login);
  }, [navigate]);

  if (!GOOGLE_CLIENT_ID) {
    return (
      <div className="flex min-h-screen items-center justify-center p-8 text-center text-foreground">
        <p>
          Missing <code className="font-mono">VITE_GOOGLE_CLIENT_ID</code>. Copy{' '}
          <code className="font-mono">frontend/.env.example</code> to{' '}
          <code className="font-mono">frontend/.env</code> and set the Google client ID.
        </p>
      </div>
    );
  }

  if (isWebMaintenanceMode() && !lecturerCanEnter) {
    return (
      <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
        <MaintenancePage
          onLoginSuccess={handleLoginSuccess}
          loginMessage={loginMessage}
          onDismissLoginMessage={() => setLoginMessage(null)}
        />
      </GoogleOAuthProvider>
    );
  }

  if (resetToken && !sessionUser) {
    return (
      <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
        <ResetPasswordUI token={resetToken} onComplete={handleResetComplete} />
      </GoogleOAuthProvider>
    );
  }

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <Routes>
          <Route
            path={ROUTES.login}
            element={
              sessionUser ? (
                <AuthenticatedLanding user={sessionUser} />
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
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerGrading}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerBulkGrading}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerUsers}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerSolution}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerReport}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerFeedback}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />

          <Route
            path={ROUTES.lecturerTerms}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />
          <Route
            path={ROUTES.lecturerRanking}
            element={
              <RequireRole anyOf={["LECTURER"]}>
                <LecturerDashboard user={sessionUser} onLogout={handleLogout} />
              </RequireRole>
            }
          />

          <Route
            path={ROUTES.studentDashboard}
            element={
              isWebMaintenanceMode() ? (
                <Navigate to={ROUTES.lecturerDashboard} replace />
              ) : (
                <RequireRole anyOf={["STUDENT"]}>
                  <StudentDashboard user={sessionUser} onLogout={handleLogout} view="dashboard" />
                </RequireRole>
              )
            }
          />
          <Route
            path={ROUTES.studentHistory}
            element={
              isWebMaintenanceMode() ? (
                <Navigate to={ROUTES.lecturerDashboard} replace />
              ) : (
                <RequireRole anyOf={["STUDENT"]}>
                  <StudentDashboard user={sessionUser} onLogout={handleLogout} view="history" />
                </RequireRole>
              )
            }
          />
          <Route
            path={ROUTES.studentRanking}
            element={
              isWebMaintenanceMode() ? (
                <Navigate to={ROUTES.lecturerDashboard} replace />
              ) : (
                <RequireRole anyOf={["STUDENT"]}>
                  <StudentDashboard user={sessionUser} onLogout={handleLogout} view="ranking" />
                </RequireRole>
              )
            }
          />

          <Route
            path={ROUTES.noAccess}
            element={
              sessionUser ? (
                <NoAccessPage user={sessionUser} />
              ) : (
                <Navigate to={ROUTES.login} replace />
              )
            }
          />

          <Route path={ROUTES.rateLimited} element={<RateLimitedPage />} />

          <Route
            path="*"
            element={
              sessionUser ? (
                <AuthenticatedLanding user={sessionUser} />
              ) : (
                <Navigate to={ROUTES.login} replace />
              )
            }
          />
        </Routes>
    </GoogleOAuthProvider>
  );
}
