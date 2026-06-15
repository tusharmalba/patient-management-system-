import { useState, useEffect } from 'react';
import { appointmentAPI, patientAPI, doctorAPI } from '../services/api';
import { getUser } from '../utils/auth';

const STATUS_COLORS = {
  SCHEDULED: 'badge-info', CONFIRMED: 'badge-success',
  COMPLETED: 'badge-gray', CANCELLED: 'badge-danger', NO_SHOW: 'badge-warning'
};

export default function AppointmentsPage() {
  const currentUser = getUser();
  const isPatient   = currentUser?.role === 'PATIENT';
  const isDoctor    = currentUser?.role === 'DOCTOR';
  const isAdmin     = currentUser?.role === 'ADMIN';

  const [appointments, setAppointments] = useState([]);
  const [total, setTotal]     = useState(0);
  const [page, setPage]       = useState(0);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [patients, setPatients]   = useState([]);
  const [doctors, setDoctors]     = useState([]);
  const [selectedPatient, setSelectedPatient] = useState('');
  const [view, setView]     = useState('my'); // 'my' | 'today' | 'patient'
  const [form, setForm]     = useState({
    patientId:'', doctorId:'', appointmentTime:'',
    durationMinutes: 30, reason:'', priority:'LOW'
  });
  const [saving, setSaving]   = useState(false);
  const [error, setError]     = useState('');
  const [success, setSuccess] = useState('');

  const PAGE_SIZE = 20;

  useEffect(() => {
    // Load dropdown data for ADMIN/DOCTOR who can book appointments
    if (!isPatient) loadDropdownData();
  }, []);

  useEffect(() => {
    fetchAppointments();
  }, [view, selectedPatient, page]);

  const loadDropdownData = async () => {
    try {
      const [pRes, dRes] = await Promise.all([
        patientAPI.getAll({ page: 0, size: 200 }),
        doctorAPI.getAll({ page: 0, size: 200 }),
      ]);
      setPatients(pRes.data.data.content || []);
      setDoctors(dRes.data.data.content || []);
    } catch (e) { console.error('Dropdown load failed', e); }
  };

  const fetchAppointments = async () => {
    setLoading(true); setError('');
    try {
      let data = [], totalCount = 0;
      const params = { page, size: PAGE_SIZE, sort: 'appointmentTime,desc' };

      if (view === 'today') {
        // Today's appointments (filtered by role on backend)
        const res = await appointmentAPI.getToday();
        data = res.data.data || [];
        totalCount = data.length;

      } else if (view === 'patient' && selectedPatient && !isPatient) {
        // Admin/Doctor viewing a specific patient's appointments
        const res = await appointmentAPI.getByPatient(selectedPatient, params);
        data = res.data.data.content || [];
        totalCount = res.data.data.totalElements || 0;

      } else {
        // DEFAULT: /api/appointments/my — role-aware
        // PATIENT  → their own appointments
        // DOCTOR   → their own schedule
        // ADMIN    → all appointments
        const res = await appointmentAPI.getMy(params);
        data = res.data.data.content || [];
        totalCount = res.data.data.totalElements || 0;
      }

      setAppointments(data);
      setTotal(totalCount);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load appointments');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = async () => {
    setError('');
    setForm({ patientId:'', doctorId:'', appointmentTime:'', durationMinutes:30, reason:'', priority:'LOW' });
    if (!patients.length || !doctors.length) await loadDropdownData();
    setShowModal(true);
  };

  const handleChange = (e) =>
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      await appointmentAPI.create({
        ...form,
        patientId: Number(form.patientId),
        doctorId:  Number(form.doctorId),
        durationMinutes: Number(form.durationMinutes),
      });
      setSuccess('✅ Appointment booked successfully!');
      setShowModal(false);
      setView('my'); setPage(0);
      setTimeout(() => setSuccess(''), 4000);
    } catch (err) {
      const msg = err.response?.data?.message || 'Booking failed';
      setError(msg);
    } finally { setSaving(false); }
  };

  const handleCancel = async (id) => {
    const reason = prompt('Reason for cancellation (optional):');
    if (reason === null) return; // user pressed Cancel on dialog
    try {
      await appointmentAPI.cancel(id, reason || 'No reason provided');
      setSuccess('Appointment cancelled');
      fetchAppointments();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Cancel failed');
    }
  };

  const handleComplete = async (id) => {
    const notes = prompt('Add doctor notes (optional):');
    if (notes === null) return;
    try {
      await appointmentAPI.updateStatus(id, { status: 'COMPLETED', doctorNotes: notes });
      setSuccess('Marked as completed ✓');
      fetchAppointments();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed');
    }
  };

  const fmt = (dt) => {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      {/* ── HEADER ── */}
      <div className="flex justify-between flex-center" style={{ marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>
            {isPatient ? 'My Appointments' : isDoctor ? 'My Schedule' : 'Appointments'}
          </h1>
          <p className="text-muted" style={{ fontSize: 14 }}>{total} record{total !== 1 ? 's' : ''}</p>
        </div>
        {/* Only ADMIN and DOCTOR can book appointments */}
        {!isPatient && (
          <button className="btn btn-primary" onClick={openCreate}>+ Book Appointment</button>
        )}
      </div>

      {success && <div className="alert alert-success">{success}</div>}
      {error && !showModal && <div className="alert alert-error">{error}</div>}

      {/* ── VIEW FILTER (only for ADMIN/DOCTOR) ── */}
      {!isPatient && (
        <div className="card" style={{ marginBottom: 16, padding: 16 }}>
          <div className="flex gap-2" style={{ flexWrap: 'wrap', alignItems: 'center' }}>
            <button className={`btn btn-sm ${view === 'my' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => { setView('my'); setPage(0); }}>
              {isDoctor ? '📋 My Schedule' : '📋 All'}
            </button>
            <button className={`btn btn-sm ${view === 'today' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => { setView('today'); setPage(0); }}>
              📅 Today
            </button>
            {isAdmin && (
              <button className={`btn btn-sm ${view === 'patient' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setView('patient')}>
                🔍 By Patient
              </button>
            )}

            {/* Patient selector for By Patient view */}
            {view === 'patient' && isAdmin && (
              <select className="form-select" style={{ maxWidth: 280, marginLeft: 8 }}
                value={selectedPatient}
                onChange={e => { setSelectedPatient(e.target.value); setPage(0); }}>
                <option value="">— Select a patient —</option>
                {patients.map(p => (
                  <option key={p.id} value={p.id}>{p.fullName}</option>
                ))}
              </select>
            )}

            <button className="btn btn-sm btn-secondary" style={{ marginLeft: 'auto' }}
              onClick={fetchAppointments}>
              🔄 Refresh
            </button>
          </div>
        </div>
      )}

      {/* PATIENT info banner */}
      {isPatient && (
        <div style={{
          background: '#dbeafe', border: '1px solid #93c5fd',
          borderRadius: 8, padding: '10px 16px', marginBottom: 16,
          fontSize: 14, color: '#1e40af'
        }}>
          ℹ️ Showing all appointments booked for you. Contact your doctor or admin to book a new appointment.
        </div>
      )}

      {/* ── TABLE ── */}
      <div className="card">
        <div className="table-container">
          {loading ? (
            <div style={{ textAlign: 'center', padding: 60 }}>
              <div className="spinner" style={{ width: 36, height: 36, margin: '0 auto 12px' }} />
              <p className="text-muted">Loading appointments...</p>
            </div>
          ) : appointments.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 60, color: 'var(--text-muted)' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>📅</div>
              <p style={{ fontWeight: 600, marginBottom: 6 }}>No appointments found</p>
              {isPatient && (
                <p style={{ fontSize: 13 }}>
                  No appointments have been booked for you yet.<br />
                  Contact your doctor or admin to schedule one.
                </p>
              )}
              {view === 'today' && !isPatient && (
                <p style={{ fontSize: 13 }}>No appointments scheduled for today.</p>
              )}
              {view === 'patient' && !selectedPatient && (
                <p style={{ fontSize: 13 }}>Select a patient from the dropdown above.</p>
              )}
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Patient</th>
                  <th>Doctor</th>
                  <th>Date &amp; Time</th>
                  <th>Reason</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map((a, idx) => (
                  <tr key={a.id}>
                    <td style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                      {idx + 1 + page * PAGE_SIZE}
                    </td>
                    <td style={{ fontWeight: 600 }}>{a.patientName}</td>
                    <td>
                      <div style={{ fontWeight: 500 }}>{a.doctorName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                        {a.doctorSpecialization}
                      </div>
                    </td>
                    <td style={{ whiteSpace: 'nowrap', fontSize: 13 }}>
                      {fmt(a.appointmentTime)}
                    </td>
                    <td style={{ maxWidth: 160, fontSize: 13 }}>{a.reason}</td>
                    <td>
                      <span className={`badge ${
                        a.priority === 'CRITICAL' ? 'badge-danger' :
                        a.priority === 'HIGH'     ? 'badge-warning' :
                        a.priority === 'MEDIUM'   ? 'badge-info' : 'badge-gray'
                      }`}>{a.priority}</span>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_COLORS[a.status] || 'badge-gray'}`}>
                        {a.status}
                      </span>
                      {a.cancellationReason && (
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                          {a.cancellationReason}
                        </div>
                      )}
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {(a.status === 'SCHEDULED' || a.status === 'CONFIRMED') && (
                          <>
                            {/* Doctor/Admin can complete */}
                            {!isPatient && (
                              <button className="btn btn-secondary btn-sm"
                                title="Mark as completed"
                                onClick={() => handleComplete(a.id)}>✓</button>
                            )}
                            {/* Everyone can cancel their own */}
                            <button className="btn btn-danger btn-sm"
                              title="Cancel appointment"
                              onClick={() => handleCancel(a.id)}>✕</button>
                          </>
                        )}
                        {a.status === 'COMPLETED' && (
                          <span style={{ fontSize: 12, color: 'var(--success)' }}>✓ Done</span>
                        )}
                        {a.status === 'CANCELLED' && (
                          <span style={{ fontSize: 12, color: 'var(--danger)' }}>✕ Cancelled</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* ── PAGINATION ── */}
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>← Prev</button>
          {[...Array(Math.min(totalPages, 7))].map((_, i) => (
            <button key={i} className={i === page ? 'active' : ''} onClick={() => setPage(i)}>
              {i + 1}
            </button>
          ))}
          <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
        </div>
      )}

      {/* ── BOOK MODAL (ADMIN/DOCTOR only) ── */}
      {showModal && !isPatient && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 999, padding: 20
        }}>
          <div className="card" style={{ width: '100%', maxWidth: 560 }}>
            <div className="card-header flex justify-between flex-center">
              <span>📅 Book New Appointment</span>
              <button onClick={() => setShowModal(false)}
                style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer' }}>✕</button>
            </div>
            <div className="card-body">
              {error && <div className="alert alert-error">{error}</div>}
              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label className="form-label">Patient *</label>
                  <select className="form-select" name="patientId"
                    value={form.patientId} onChange={handleChange} required>
                    <option value="">Select patient...</option>
                    {patients.map(p => (
                      <option key={p.id} value={p.id}>{p.fullName} ({p.email})</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Doctor *</label>
                  <select className="form-select" name="doctorId"
                    value={form.doctorId} onChange={handleChange} required>
                    <option value="">Select doctor...</option>
                    {doctors.map(d => (
                      <option key={d.id} value={d.id}>{d.fullName} — {d.specialization}</option>
                    ))}
                  </select>
                </div>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label">Date &amp; Time *</label>
                    <input type="datetime-local" className="form-control"
                      name="appointmentTime" value={form.appointmentTime}
                      onChange={handleChange} required
                      min={new Date(Date.now() + 60000).toISOString().slice(0, 16)} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Duration (min)</label>
                    <input type="number" className="form-control"
                      name="durationMinutes" value={form.durationMinutes}
                      onChange={handleChange} min={15} max={120} />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Reason *</label>
                  <textarea className="form-control" name="reason" rows={2}
                    value={form.reason} onChange={handleChange} required
                    placeholder="e.g. Routine checkup, Follow-up, Fever..." />
                </div>
                <div className="form-group">
                  <label className="form-label">Priority</label>
                  <select className="form-select" name="priority"
                    value={form.priority} onChange={handleChange}>
                    <option value="LOW">🟢 LOW — Routine</option>
                    <option value="MEDIUM">🟡 MEDIUM — Semi-urgent</option>
                    <option value="HIGH">🟠 HIGH — Urgent</option>
                    <option value="CRITICAL">🔴 CRITICAL — Emergency</option>
                  </select>
                </div>
                <div className="flex gap-2" style={{ justifyContent: 'flex-end' }}>
                  <button type="button" className="btn btn-secondary"
                    onClick={() => setShowModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Booking...' : '📅 Book Appointment'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}