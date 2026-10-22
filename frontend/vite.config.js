import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  // Vercel-style NEXT_PUBLIC_* aliases plus the Vite VITE_* contract.
  envPrefix: ['VITE_', 'NEXT_PUBLIC_'],
  build: {
    outDir: mode === 'desktop' ? 'dist-desktop' : 'dist',
  },
  server: {
    port: 5173
  }
}))
