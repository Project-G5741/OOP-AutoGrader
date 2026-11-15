import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MotionConfig } from 'framer-motion'

import '@fontsource/plus-jakarta-sans/400.css'
import '@fontsource/plus-jakarta-sans/500.css'
import '@fontsource/plus-jakarta-sans/600.css'
import '@fontsource/plus-jakarta-sans/700.css'
import '@fontsource/plus-jakarta-sans/800.css'
import '@fontsource/jetbrains-mono/400.css'
import '@fontsource/jetbrains-mono/500.css'
import '@fontsource/jetbrains-mono/600.css'
import './index.css'

import { ThemeProvider } from './context/ThemeContext.jsx'
import { ToastProvider } from './components/ui/Toast.jsx'
import { applyBrandFavicon } from './theme/applyBrandFavicon.js'

applyBrandFavicon()

const appMode = import.meta.env.VITE_APP_MODE
const isDesktopMode = appMode === 'desktop'
const isDesktopLecturerMode = appMode === 'desktop-lecturer'

async function bootstrap() {
  // Dynamic import so web never evaluates desktop entry modules (their top-level
  // session seeds would overwrite a real JWT with a synthetic desktop session).
  let appModule
  if (isDesktopLecturerMode) {
    appModule = await import('./App.lecturer.desktop.jsx')
  } else if (isDesktopMode) {
    appModule = await import('./App.desktop.jsx')
  } else {
    appModule = await import('./App.jsx')
  }
  const App = appModule.default

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <MotionConfig reducedMotion="user">
        <BrowserRouter>
          <ThemeProvider>
            <ToastProvider>
              <App />
            </ToastProvider>
          </ThemeProvider>
        </BrowserRouter>
      </MotionConfig>
    </React.StrictMode>,
  )
}

bootstrap()
