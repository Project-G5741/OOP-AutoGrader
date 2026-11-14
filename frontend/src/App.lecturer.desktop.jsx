import { Routes, Route, Navigate } from "react-router-dom";
import RequireRole from "./components/auth/RequireRole";
import ThemeToggle from "./components/ThemeToggle";
import { persistAuthSession } from "./utils/authRoutes";
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

function applyDesktopLecturerSession() {
  if (import.meta.env.VITE_APP_MODE !== "desktop-lecturer") {
    return;
  }
  persistAuthSession(DESKTOP_LECTURER_USER.accessToken, DESKTOP_LECTURER_USER);
}

applyDesktopLecturerSession();

function LecturerOfflineBulkShell() {
  return (
    <div className="min-h-screen bg-surface text-text">
      <header className="flex items-center justify-between border-b border-border px-4 py-3">
        <div>
          <p className="text-xs uppercase tracking-wide text-text-secondary">Offline lecturer</p>
          <h1 className="text-lg font-semibold text-navy">Bulk Grading</h1>
        </div>
        <div className="flex items-center gap-2">
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
        <section className="rounded-lg border border-border bg-surface-secondary p-4">
          <h2 className="text-sm font-semibold text-navy">Rubric packs</h2>
          <p className="mt-1 text-sm text-text-secondary">
            Import practice <code className="font-mono text-xs">.agpack</code> files (wired in a later unit).
          </p>
        </section>
        <section className="rounded-lg border border-border bg-surface-secondary p-4">
          <h2 className="text-sm font-semibold text-navy">Current-term roster</h2>
          <p className="mt-1 text-sm text-text-secondary">
            Import the enrollment file before grading (wired in a later unit).
          </p>
        </section>
        <section className="rounded-lg border border-border bg-surface-secondary p-4">
          <h2 className="text-sm font-semibold text-navy">Grade a folder dump</h2>
          <p className="mt-1 text-sm text-text-secondary">
            Full Bulk Grading panel mounts here after packs and roster are ready.
          </p>
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
