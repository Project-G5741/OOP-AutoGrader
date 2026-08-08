Vercel deployment notes

Production app: https://oop-autograder.vercel.app/

1. On Vercel dashboard, open the project and go to Settings → Environment Variables.
2. Add the following variables (for Production):
   - `VITE_API_URL` = https://<your-render-service>.onrender.com
   - `VITE_GOOGLE_CLIENT_ID` = <google client id>
3. Redeploy the project so the `VITE_` variables are baked into the client build.

Password reset: backend emails link to `https://oop-autograder.vercel.app?resetToken=...`. The SPA reads that query param on load and shows the reset-password screen (no extra Vercel rewrites needed).

Local dev:
 - Copy `.env.example` to `.env` in `frontend/` and edit values.
 - Run `npm install` and `npm run dev`.
