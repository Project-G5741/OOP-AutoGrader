import React, { useState } from 'react';
import { GoogleOAuthProvider } from '@react-oauth/google';

// Read Google client id from Vite env (frontend/.env.example)
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '901862485743-on3umlivpedse7hosvjtjqdpqr57s69i.apps.googleusercontent.com';
import { ThemeProvider } from './context/ThemeContext';
import Login from './pages/Login';
import LecturerDashboard from './pages/LecturerDashboard';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const handleLoginSuccess = (accessToken) => {
    setIsAuthenticated(true);
  };

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <ThemeProvider>
        {isAuthenticated ? <LecturerDashboard /> : <Login onLoginSuccess={handleLoginSuccess} />}
      </ThemeProvider>
    </GoogleOAuthProvider>
  );
}