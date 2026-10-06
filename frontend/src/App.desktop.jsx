import { Routes, Route, Navigate } from "react-router-dom";
import StudentDashboard from "./pages/StudentDashboard";
import RequireRole from "./components/auth/RequireRole";
import DesktopPracticeStatusBanner from "./components/student/DesktopPracticeStatusBanner";
import { persistAuthSession, ROUTES } from "./utils/authRoutes";
import { quitPracticeApp } from "./utils/desktopQuit";

const DESKTOP_USER = {
  accessToken: "desktop-local",
  email: "practice@desktop.local",
  fullName: "",
  roles: ["STUDENT"],
  inCurrentTerm: true,
  studentCode: "DESKTOP",
};

/** Seed the synthetic student session only inside the desktop WebView2 build. */
function applyDesktopSession() {
  if (import.meta.env.VITE_APP_MODE !== 'desktop') {
    return;
  }
  persistAuthSession(DESKTOP_USER.accessToken, DESKTOP_USER);
}

applyDesktopSession();

export default function AppDesktop() {
  const user = DESKTOP_USER;

  return (
    <>
      <DesktopPracticeStatusBanner />
      <Routes>
      <Route path="/" element={<Navigate to={ROUTES.studentDashboard} replace />} />
      <Route
        path={ROUTES.studentDashboard}
        element={
          <RequireRole anyOf={["STUDENT"]}>
            <StudentDashboard user={user} onLogout={quitPracticeApp} view="dashboard" />
          </RequireRole>
        }
      />
      <Route
        path={ROUTES.studentHistory}
        element={
          <RequireRole anyOf={["STUDENT"]}>
            <StudentDashboard user={user} onLogout={quitPracticeApp} view="history" />
          </RequireRole>
        }
      />
      <Route path="*" element={<Navigate to={ROUTES.studentDashboard} replace />} />
    </Routes>
    </>
  );
}
