import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { getUser, logout } from '../utils/auth';

/**
 * Shared layout wrapper for all protected pages.
 * Uses React Router's <Outlet /> to render child routes.
 */
export default function Layout() {
  const user = getUser();
  const navigate = useNavigate();

  const navItems = [
    { to: '/dashboard',    icon: '📊', label: 'Dashboard' },
    { to: '/patients',     icon: '🧑‍⚕️', label: 'Patients' },
    { to: '/doctors',      icon: '👨‍⚕️', label: 'Doctors' },
    { to: '/appointments', icon: '📅', label: 'Appointments' },
  ];

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* ── SIDEBAR ── */}
      <aside style={{
        width: 240, background: '#1e293b', color: '#fff',
        display: 'flex', flexDirection: 'column', padding: '24px 0', flexShrink: 0
      }}>
        {/* Logo */}
        <div style={{ padding: '0 20px 24px', borderBottom: '1px solid #334155' }}>
          <div style={{ fontSize: 20, fontWeight: 700 }}>🏥 PMS</div>
          <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 4 }}>
            Patient Management
          </div>
        </div>

        {/* Nav links */}
        <nav style={{ flex: 1, padding: '16px 12px' }}>
          {navItems.map(({ to, icon, label }) => (
            <NavLink key={to} to={to} style={({ isActive }) => ({
              display: 'flex', alignItems: 'center', gap: 10,
              padding: '10px 12px', borderRadius: 8, marginBottom: 4,
              textDecoration: 'none', fontSize: 14, fontWeight: 500,
              color: isActive ? '#fff' : '#94a3b8',
              background: isActive ? '#2563eb' : 'transparent',
              transition: 'all .15s',
            })}>
              <span>{icon}</span>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* User info + logout */}
        <div style={{ padding: '16px 20px', borderTop: '1px solid #334155' }}>
          <div style={{ fontSize: 13, color: '#94a3b8', marginBottom: 4 }}>
            {user?.fullName}
          </div>
          <div style={{
            display: 'inline-block', fontSize: 11, fontWeight: 600,
            background: '#2563eb', color: '#fff', borderRadius: 999,
            padding: '2px 8px', marginBottom: 12
          }}>
            {user?.role}
          </div>
          <br />
          <button
            onClick={logout}
            style={{
              background: 'none', border: '1px solid #475569', color: '#94a3b8',
              borderRadius: 6, padding: '6px 12px', cursor: 'pointer', fontSize: 13,
              width: '100%'
            }}>
            🚪 Logout
          </button>
        </div>
      </aside>

      {/* ── MAIN CONTENT ── */}
      <main style={{ flex: 1, padding: 28, overflow: 'auto' }}>
        <Outlet />
      </main>
    </div>
  );
}
