import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  // Vercel-style NEXT_PUBLIC_* aliases plus the Vite VITE_* contract.
  // LECTURER_* exposes web Bulk folder release URL (LECTURER_BULK_BUNDLE_URL).
  envPrefix: ['VITE_', 'NEXT_PUBLIC_', 'LECTURER_'],
  build: {
    outDir:
      mode === 'desktop-lecturer'
        ? 'dist-desktop-lecturer'
        : mode === 'desktop'
          ? 'dist-desktop'
          : 'dist',
  },
  server: {
    port: 5173
  }
}))
