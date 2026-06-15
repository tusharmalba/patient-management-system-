import { useState, useEffect } from 'react';
import { dashboardAPI } from '../services/api';

/**
 * DASHBOARD PAGE
 *
 * Fetches analytics from GET /api/dashboard/stats
 * Displays metric cards, risk distribution, common diseases.
 *
 * useEffect(() => { fetchData() }, [])
 *   → [] dependency array = run ONCE after component mounts
 *   → Equivalent to componentDidMount in class components
 */
export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const res = await dashboardAPI.getStats();
      setStats(res.data.data);
    } catch (err) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 60 }}>
      <div className="spinner" style={{ width: 40, height: 40 }} />
    </div>
  );
  if (error) return <div className="alert alert-error">{error}</div>;

  const metricCards = [
    { label: 'Total Patients',       value: stats?.totalPatients,       icon: '🧑‍⚕️', color: '#2563eb' },
    { label: 'Total Doctors',        value: stats?.totalDoctors,        icon: '👨‍⚕️', color: '#7c3aed' },
    { label: "Today's Appointments", value: stats?.todaysAppointments,  icon: '📅', color: '#0891b2' },
    { label: 'Total Appointments',   value: stats?.totalAppointments,   icon: '📋', color: '#16a34a' },
    { label: 'High Risk Patients',   value: stats?.highRiskPatients,    icon: '🚨', color: '#dc2626' },
    { label: 'Scheduled Today',      value: stats?.scheduledAppointments, icon: '✅', color: '#d97706' },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Dashboard</h1>
        <p className="text-muted" style={{ marginTop: 4 }}>
          Overview of your Patient Management System
        </p>
      </div>

      {/* ── METRIC CARDS ── */}
      <div className="grid-3" style={{ marginBottom: 28 }}>
        {metricCards.map(({ label, value, icon, color }) => (
          <div key={label} className="card" style={{ padding: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 6 }}>{label}</p>
                <p style={{ fontSize: 32, fontWeight: 700, color }}>{value ?? 0}</p>
              </div>
              <span style={{ fontSize: 36 }}>{icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* ── RISK DISTRIBUTION + DISEASE CHART ── */}
      <div className="grid-2">
        {/* Risk Distribution */}
        <div className="card">
          <div className="card-header">🎯 Patient Risk Distribution</div>
          <div className="card-body">
            {[
              { label: 'High Risk',   count: stats?.highRiskPatients,   color: '#dc2626', bg: '#fee2e2' },
              { label: 'Medium Risk', count: stats?.mediumRiskPatients, color: '#d97706', bg: '#fef3c7' },
              { label: 'Low Risk',    count: stats?.lowRiskPatients,    color: '#16a34a', bg: '#dcfce7' },
            ].map(({ label, count, color, bg }) => {
              const total = (stats?.highRiskPatients || 0)
                          + (stats?.mediumRiskPatients || 0)
                          + (stats?.lowRiskPatients || 0);
              const pct = total > 0 ? Math.round((count / total) * 100) : 0;
              return (
                <div key={label} style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span style={{ fontWeight: 500, color }}>{label}</span>
                    <span style={{ fontWeight: 700 }}>{count ?? 0} ({pct}%)</span>
                  </div>
                  <div style={{ height: 8, background: '#f1f5f9', borderRadius: 4, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 4, transition: 'width .5s' }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Most Common Diseases */}
        <div className="card">
          <div className="card-header">🦠 Most Common Diagnoses</div>
          <div className="card-body">
            {stats?.mostCommonDiseases?.length > 0 ? (
              stats.mostCommonDiseases.map((d, i) => (
                <div key={i} style={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  padding: '10px 0', borderBottom: i < stats.mostCommonDiseases.length - 1
                    ? '1px solid var(--border)' : 'none'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{
                      width: 24, height: 24, borderRadius: '50%', background: '#dbeafe',
                      color: '#1d4ed8', fontSize: 12, fontWeight: 700,
                      display: 'inline-flex', alignItems: 'center', justifyContent: 'center'
                    }}>{i + 1}</span>
                    <span style={{ fontWeight: 500 }}>{d.disease}</span>
                  </div>
                  <span className="badge badge-info">{d.count} patients</span>
                </div>
              ))
            ) : (
              <p className="text-muted text-center" style={{ padding: 20 }}>
                No diagnosis data available yet
              </p>
            )}
          </div>
        </div>
      </div>

      {/* ── APPOINTMENT STATUS ── */}
      <div className="card" style={{ marginTop: 28 }}>
        <div className="card-header">📊 Appointment Overview</div>
        <div className="card-body" style={{ display: 'flex', gap: 32 }}>
          {[
            { label: 'Total',     value: stats?.totalAppointments,     color: 'var(--primary)' },
            { label: 'Scheduled', value: stats?.scheduledAppointments, color: '#16a34a' },
            { label: 'Cancelled', value: stats?.cancelledAppointments, color: '#dc2626' },
            { label: 'Today',     value: stats?.todaysAppointments,    color: '#d97706' },
          ].map(({ label, value, color }) => (
            <div key={label} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color }}>{value ?? 0}</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{label}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
