Vercel deployment notes

Production app: https://oop-autograder.vercel.app/

This is a **Vite** SPA, not Next.js. Client env vars must start with `VITE_` (or `NEXT_PUBLIC_` — both prefixes are enabled). They are **inlined at build time**. Changing a value in the Vercel dashboard does nothing until you **Redeploy**.

## First-time deploy

1. Import the GitHub repo into Vercel. Set **Root Directory** to `frontend` if the project root is the monorepo.
2. On Vercel dashboard, open the project and go to **Settings → Environment Variables**.
3. Add the following variables (for Production):
   - `VITE_API_URL` = https://<your-render-service>.onrender.com
   - `VITE_GOOGLE_CLIENT_ID` = <google client id>
   - `VITE_MAINTENANCE_MODE` = `false` (or `true` to lock the site)
4. Deploy. Framework: Vite. Build command: `npm run build`. Output: `dist`.
5. Confirm `vercel.json` SPA rewrite is present (`/(.*) → /`).

Password reset: backend emails link to `https://oop-autograder.vercel.app?resetToken=...`. The SPA reads that query param on load and shows the reset-password screen (no extra Vercel rewrites needed).

## Toggle maintenance (lock / unlock)

Vite cannot read a live server flag. Flip the env, then rebuild.

1. Vercel → project → **Settings → Environment Variables**.
2. Set `VITE_MAINTENANCE_MODE` to `true` (students and guests see maintenance; lecturers can still sign in) or `false` (normal app). `1` / `yes` / `on` also lock. You may use `NEXT_PUBLIC_MAINTENANCE_MODE` instead; either flag being true locks the site for non-lecturers.
3. **Deployments → … on the latest Production deploy → Redeploy** (or push an empty commit). Leave **Use existing Build Cache** off if the new env is not picked up.
4. Hard-refresh the site. Students are signed out and stay on the maintenance page. Lecturers use **Lecturer sign in** (or an existing lecturer session) to open the dashboard.
5. To open the site again for everyone: set the flag back to `false` and **Redeploy**.

Desktop offline practice builds ignore this flag.

## Local preview of the lock

In `frontend/.env`:

```
VITE_MAINTENANCE_MODE=true
```

Restart `npm run dev`. Set it back to `false` (or remove it) and restart to resume login.

Local dev:
 - Copy `.env.example` to `.env` in `frontend/` and edit values.
 - Run `npm install` and `npm run dev`.
