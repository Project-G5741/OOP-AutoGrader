import { Routes, Route, Navigate } from "react-router-dom";
import StudentDashboard from "./pages/StudentDashboard";
import RequireRole from "./components/auth/RequireRole";
import DesktopPracticeStatusBanner from "./components/student/DesktopPracticeStatusBanner";
import { ROUTES } from "./utils/authRoutes";

const DESKTOP_USER = {
  accessToken: "desktop-local",
  email: "practice@desktop.local",
  fullName: "",
  roles: ["STUDENT"],
  inCurrentTerm: true,
  studentCode: "DESKTOP",
};

/** Always replace any stale web JWT in WebView2 storage before the router runs. */
function applyDesktopSession() {
  sessionStorage.setItem("accessToken", DESKTOP_USER.accessToken);
  sessionStorage.setItem("user", JSON.stringify(DESKTOP_USER));
}

function quitPracticeApp() {
  const webview = window.chrome?.webview;
  if (webview && typeof webview.postMessage === "function") {
    webview.postMessage({ type: "practice-quit" });
  }
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
