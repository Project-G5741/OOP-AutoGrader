import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import './index.css'

import { ThemeProvider } from './context/ThemeContext.jsx'
import { ToastProvider } from './components/ui/Toast.jsx'
import { applyBrandFavicon } from './theme/applyBrandFavicon.js'

applyBrandFavicon()

const isDesktopMode = import.meta.env.VITE_APP_MODE === 'desktop'

async function bootstrap() {
  // Dynamic import so web never evaluates App.desktop.jsx (its module top-level
  // would overwrite a real JWT with the synthetic desktop-local session).
  const App = (
    isDesktopMode
      ? await import('./App.desktop.jsx')
      : await import('./App.jsx')
  ).default

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <BrowserRouter>
        <ThemeProvider>
          <ToastProvider>
            <App />
          </ToastProvider>
        </ThemeProvider>
      </BrowserRouter>
    </React.StrictMode>,
  )
}

bootstrap()
