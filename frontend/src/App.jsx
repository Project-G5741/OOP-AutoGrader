import React, { useState } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";

const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  "901862485743-on3umlivpedse7hosvjtjqdpqr57s69i.apps.googleusercontent.com";
import { ThemeProvider } from "./context/ThemeContext";
import Login from "./pages/Login";
import LecturerDashboard from "./pages/LecturerDashboard";
import StudentDashboard from "./pages/StudentDashboard";

export default function App() {
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

  const renderDashboard = () => {
    if (!user?.roles?.length) return <div>Unknown role</div>;
    if (user.roles.includes("LECTURER")) return <LecturerDashboard user={user} />;
    if (user.roles.includes("STUDENT")) return <StudentDashboard user={user} />;
    return <div>Unknown role</div>;
  };

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <ThemeProvider>
        {user ? renderDashboard() : <Login onLoginSuccess={handleLoginSuccess} />}
      </ThemeProvider>
    </GoogleOAuthProvider>
  );
}