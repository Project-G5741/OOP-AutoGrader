import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  build: {
    outDir: mode === 'desktop' ? 'dist-desktop' : 'dist',
  },
  server: {
    port: 5173
  }
}))
