import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { isAuthenticated } from './utils/auth';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import PatientsPage from './pages/PatientsPage';
import DoctorsPage from './pages/DoctorsPage';
import AppointmentsPage from './pages/AppointmentsPage';
import Layout from './components/Layout';

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                        APP ROUTING                                   ║
 * ║                                                                       ║
 * ║  React Router v6 setup:                                               ║
 * ║  - <BrowserRouter>   → Uses browser history API (real URLs)           ║
 * ║  - <Routes>          → Renders first matching route                   ║
 * ║  - <Route>           → Maps URL path to component                    ║
 * ║  - <Navigate>        → Redirect component                             ║
 * ║                                                                       ║
 * ║  ProtectedRoute pattern:                                              ║
 * ║  If user is NOT authenticated → redirect to /login                   ║
 * ║  If user IS authenticated → render the requested component            ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */

// Higher-order component for route protection
const ProtectedRoute = ({ children }) => {
  return isAuthenticated() ? children : <Navigate to="/login" replace />;
};

// Redirect authenticated users away from login/register
const PublicRoute = ({ children }) => {
  return !isAuthenticated() ? children : <Navigate to="/dashboard" replace />;
};

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={
          <PublicRoute><LoginPage /></PublicRoute>
        } />
        <Route path="/register" element={
          <PublicRoute><RegisterPage /></PublicRoute>
        } />

        {/* Protected routes wrapped in shared Layout */}
        <Route path="/" element={
          <ProtectedRoute><Layout /></ProtectedRoute>
        }>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="doctors" element={<DoctorsPage />} />
          <Route path="appointments" element={<AppointmentsPage />} />
        </Route>

        {/* Catch-all: redirect to dashboard */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
