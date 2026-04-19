import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth }  from './context/AuthContext';
import { ThemeProvider }          from './context/ThemeContext';
import { CatalogProvider }        from './context/CatalogContext';

import AuthPage             from './pages/AuthPage';
import CatalogPage          from './pages/CatalogPage';
import AnimeDetailPage      from './pages/AnimeDetailPage';
import CollectionPage       from './pages/CollectionPage';
import CollectionDetailPage from './pages/CollectionDetailPage';
import AccountPage          from './pages/AccountPage';
import SettingsPage         from './pages/SettingsPage';

function ProtectedRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/" replace />;
}

function AppRoutes() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/"                   element={user ? <Navigate to="/catalog" replace /> : <AuthPage />} />
      <Route path="/catalog"            element={<ProtectedRoute><CatalogPage /></ProtectedRoute>} />
      <Route path="/anime/:id"          element={<ProtectedRoute><AnimeDetailPage /></ProtectedRoute>} />
      <Route path="/collection"         element={<ProtectedRoute><CollectionPage /></ProtectedRoute>} />
      <Route path="/collection/:animeId" element={<ProtectedRoute><CollectionDetailPage /></ProtectedRoute>} />
      <Route path="/account"            element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
      <Route path="/settings"           element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
      <Route path="*"                   element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ThemeProvider>
          <CatalogProvider>
            <AppRoutes />
          </CatalogProvider>
        </ThemeProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
