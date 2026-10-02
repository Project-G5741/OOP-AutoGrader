import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import AppWeb from './App.jsx'
import AppDesktop from './App.desktop.jsx'

import './index.css'

import { ThemeProvider } from './context/ThemeContext.jsx'
import { ToastProvider } from './components/ui/Toast.jsx'
import { applyBrandFavicon } from './theme/applyBrandFavicon.js'

applyBrandFavicon()

const isDesktopMode = import.meta.env.VITE_APP_MODE === 'desktop'
const App = isDesktopMode ? AppDesktop : AppWeb

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
