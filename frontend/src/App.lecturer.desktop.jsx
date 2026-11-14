import { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { Check, X } from "lucide-react";
import RequireRole from "./components/auth/RequireRole";
import ThemeToggle from "./components/ThemeToggle";
import DesktopRubricPackImport from "./components/student/DesktopRubricPackImport";
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

function ReadyBadge({ label, ready, known }) {
  const readyState = known ? ready : false;
  const Icon = readyState ? Check : X;
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-surface px-4 py-3">
      <span
        className={`inline-flex h-8 w-8 items-center justify-center rounded-full ${
          readyState ? "bg-success-bg text-success-text" : "bg-warning-bg text-warning-text"
        }`}
        aria-hidden
      >
        <Icon className="h-4 w-4" strokeWidth={2.5} />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wide text-text-secondary">{label}</p>
        <p className={`text-sm font-semibold ${readyState ? "text-success" : "text-warning-text"}`}>
          {!known ? "Checking…" : readyState ? "Ready" : "Not ready"}
        </p>
      </div>
    </div>
  );
}

function LecturerOfflineBulkShell() {
  const showToast = useToast();
  const [labs, setLabs] = useState([]);
  const [labsError, setLabsError] = useState(null);
  const [labsLoading, setLabsLoading] = useState(true);
  const [membershipEpoch, setMembershipEpoch] = useState(0);
  const [rosterReady, setRosterReady] = useState(false);
  const [rosterStatusKnown, setRosterStatusKnown] = useState(false);

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

  const rubricReady = !labsLoading && !labsError && labs.length > 0;

  return (
    <div className="min-h-screen bg-surface text-text">
      <header className="flex items-center justify-between gap-3 border-b border-border px-4 py-3 sm:px-6">
        <div>
          <p className="text-xs uppercase tracking-wide text-text-secondary">Offline lecturer</p>
          <h1 className="text-lg font-semibold text-navy">Bulk Grading</h1>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <DesktopRubricPackImport onToast={showToast} />
          <DesktopRosterImport
            compact
            onToast={showToast}
            onRosterChange={(ready) => {
              setRosterReady(Boolean(ready));
              setRosterStatusKnown(true);
              setMembershipEpoch((value) => value + 1);
            }}
          />
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
      <main className="space-y-5 px-4 py-5 sm:px-6">
        <div className="grid gap-3 sm:grid-cols-2">
          <ReadyBadge
            label="Current-term roster"
            ready={rosterReady}
            known={rosterStatusKnown}
          />
          <ReadyBadge
            label="Lab rubric"
            ready={rubricReady}
            known={!labsLoading}
          />
        </div>

        {labsError && (
          <p className="rounded-r-xl border-l-4 border-error bg-error-bg px-3.5 py-2.5 text-sm text-error-text">
            {labsError}
          </p>
        )}
        {!labsLoading && !labsError && labs.length === 0 && (
          <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
            Import a rubric pack in the header, then restart. Labs appear here after that.
          </p>
        )}
        {rosterStatusKnown && !rosterReady && (
          <p className="rounded-r-xl border-l-4 border-warning bg-warning-bg px-3.5 py-2.5 text-sm text-warning-text">
            Import the Term page <span className="font-semibold">Download roster</span> file before grading.
            Importing a rubric pack clears the local roster — import roster again after pack import.
          </p>
        )}

        {!labsLoading && labs.length > 0 && (
          <BulkGradingPanel key={membershipEpoch} labs={labs} />
        )}
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
