import { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import RequireRole from "./components/auth/RequireRole";
import ThemeToggle from "./components/ThemeToggle";
import DesktopRubricPackImport from "./components/student/DesktopRubricPackImport";
import DesktopPracticeStatusBanner from "./components/student/DesktopPracticeStatusBanner";
import DesktopRosterImport from "./components/lecturer/DesktopRosterImport";
import BulkGradingPanel from "./components/lecturer/BulkGradingPanel";
import { useToast } from "./components/ui/Toast";
import { apiFetch } from "./utils/apiFetch";
import { authHeaders } from "./utils/authHeaders";
import { readFriendlyApiError, toFriendlyError } from "./utils/apiError";
import { persistAuthSession } from "./utils/authRoutes";
import { desktopApiBase } from "./utils/desktopMode";
import { quitPracticeApp } from "./utils/desktopQuit";

const DESKTOP_LECTURER_USER = {
  accessToken: "desktop-lecturer-local",
  email: "lecturer@desktop.local",
  fullName: "Offline Lecturer",
  roles: ["LECTURER"],
  inCurrentTerm: true,
  studentCode: "LECTURER",
};

const GRADING_PATH = "/lecturer/grading";
const API_BASE = desktopApiBase();

function applyDesktopLecturerSession() {
  if (import.meta.env.VITE_APP_MODE !== "desktop-lecturer") {
    return;
  }
  persistAuthSession(DESKTOP_LECTURER_USER.accessToken, DESKTOP_LECTURER_USER);
}

applyDesktopLecturerSession();

function LecturerOfflineBulkShell() {
  const showToast = useToast();
  const [labs, setLabs] = useState([]);
  const [labsError, setLabsError] = useState(null);
  const [labsLoading, setLabsLoading] = useState(true);
  const [membershipEpoch, setMembershipEpoch] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLabsLoading(true);
      setLabsError(null);
      try {
        const response = await apiFetch(`${API_BASE}/api/lecturer/bootstrap/grading`, {
          headers: authHeaders(),
        });
        if (!response.ok) {
          throw new Error(await readFriendlyApiError(response, "read"));
        }
        const data = await response.json();
        if (!cancelled) {
          setLabs(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          setLabs([]);
          setLabsError(toFriendlyError(err, "read"));
        }
      } finally {
        if (!cancelled) {
          setLabsLoading(false);
        }
      }
    })();
    return () => { cancelled = true; };
  }, []);

  return (
    <div className="min-h-screen bg-surface text-text">
      <DesktopPracticeStatusBanner />
      <header className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-text-secondary">Offline lecturer</p>
          <h1 className="text-lg font-semibold text-navy">Bulk Grading</h1>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <DesktopRubricPackImport onToast={showToast} />
          <ThemeToggle />
          <button
            type="button"
            onClick={quitPracticeApp}
            className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-on-primary"
          >
            Exit
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl space-y-6 px-4 py-6">
        <section className="space-y-3 rounded-lg border border-border bg-surface-secondary p-4">
          <div>
            <h2 className="text-sm font-semibold text-navy">Current-term roster</h2>
            <p className="mt-1 text-sm text-text-secondary">
              Import the CSV/Excel enrollment download from the website (student_code column).
            </p>
          </div>
          <DesktopRosterImport
            onToast={showToast}
            onRosterChange={() => setMembershipEpoch((value) => value + 1)}
          />
        </section>
        <section className="space-y-3 rounded-lg border border-border bg-surface-secondary p-4">
          <div>
            <h2 className="text-sm font-semibold text-navy">Grade a folder dump</h2>
            <p className="mt-1 text-sm text-text-secondary">
              Same Lab/Exam Bulk Grading flow as the website. Results stay on this machine only.
            </p>
          </div>
          {labsLoading && (
            <p className="text-sm text-text-secondary">Loading labs from imported packs…</p>
          )}
          {labsError && (
            <p className="rounded-r-xl border-l-4 border-error bg-error-bg px-3.5 py-2.5 text-sm text-error-text">
              {labsError}
            </p>
          )}
          {!labsLoading && !labsError && labs.length === 0 && (
            <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
              Import a rubric pack before Bulk Grading. Labs appear here after pack import and restart.
            </p>
          )}
          {!labsLoading && labs.length > 0 && (
            <BulkGradingPanel key={membershipEpoch} labs={labs} />
          )}
        </section>
      </main>
    </div>
  );
}

export default function AppLecturerDesktop() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={GRADING_PATH} replace />} />
      <Route
        path={GRADING_PATH}
        element={
          <RequireRole anyOf={["LECTURER"]}>
            <LecturerOfflineBulkShell />
          </RequireRole>
        }
      />
      <Route path="*" element={<Navigate to={GRADING_PATH} replace />} />
    </Routes>
  );
}
