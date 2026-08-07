import React, { useState, useMemo } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  "901862485743-on3umlivpedse7hosvjtjqdpqr57s69i.apps.googleusercontent.com";
import { ThemeProvider } from "./context/ThemeContext";
import Login from "./pages/Login";
import ResetPasswordUI from "./pages/ResetPasswordUI";
import LecturerDashboard from "./pages/LecturerDashboard";
import StudentDashboard from "./pages/StudentDashboard";

function readResetTokenFromUrl() {
  return new URLSearchParams(window.location.search).get("resetToken");
}

function clearResetTokenFromUrl() {
  const url = new URL(window.location.href);
  url.searchParams.delete("resetToken");
  const next = url.pathname + (url.search ? url.search : "") + url.hash;
  window.history.replaceState({}, "", next);
}

export default function App() {
  const initialResetToken = useMemo(() => readResetTokenFromUrl(), []);
  const [resetToken, setResetToken] = useState(initialResetToken);
  const [loginMessage, setLoginMessage] = useState(null);
  const [user, setUser] = useState(() => {
    try {
      const saved = sessionStorage.getItem("user");
      if (!saved) return null;
      const parsed = JSON.parse(saved);
      return Array.isArray(parsed?.roles) ? parsed : null;
    } catch {
      return null;
    }
  });

  const handleLoginSuccess = (data) => {
    sessionStorage.setItem("accessToken", data.accessToken);
    sessionStorage.setItem("user", JSON.stringify(data));
    setUser(data);
  };

  const handleLogout = () => {
    console.log('handleLogout called');
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("user");
    setUser(null);
  };

  const handleResetComplete = () => {
    clearResetTokenFromUrl();
    setResetToken(null);
    setLoginMessage("Password updated. Sign in with your new password.");
  };

  const renderDashboard = () => {
    if (!user?.roles?.length) return <div>Unknown role</div>;
    if (user.roles.includes("LECTURER")) return <LecturerDashboard user={user} onLogout={handleLogout} />;
    if (user.roles.includes("STUDENT")) return <StudentDashboard user={user} onLogout={handleLogout} />;
    return <div>Unknown role</div>;
  };

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <ThemeProvider>
        {user ? (
          renderDashboard()
        ) : resetToken ? (
          <ResetPasswordUI token={resetToken} onComplete={handleResetComplete} />
        ) : (
          <Login
            onLoginSuccess={handleLoginSuccess}
            loginMessage={loginMessage}
            onDismissLoginMessage={() => setLoginMessage(null)}
          />
        )}
      </ThemeProvider>
    </GoogleOAuthProvider>
  );
}