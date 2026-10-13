import { useState, useEffect } from 'react';
import { appointmentAPI, patientAPI, doctorAPI } from '../services/api';

const STATUS_COLORS = {
  SCHEDULED: 'badge-info', CONFIRMED: 'badge-success',
  COMPLETED: 'badge-gray', CANCELLED: 'badge-danger', NO_SHOW: 'badge-warning'
};

export default function AppointmentsPage() {
  const [appointments, setAppointments] = useState([]);
  const [total, setTotal]   = useState(0);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [patients, setPatients]   = useState([]);
  const [doctors, setDoctors]     = useState([]);
  const [form, setForm] = useState({
    patientId:'', doctorId:'', appointmentTime:'',
    durationMinutes:30, reason:'', priority:'LOW'
  });
  const [saving, setSaving] = useState(false);
  const [error, setError]   = useState('');
  const [success, setSuccess] = useState('');
  const [view, setView]     = useState('all'); // 'all' | 'today'

  useEffect(() => { fetchData(); }, [view]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (view === 'today') {
        const res = await appointmentAPI.getToday();
        setAppointments(res.data.data);
        setTotal(res.data.data.length);
      } else {
        // Get recent appointments via patient-based query
        // For simplicity: get first 50 appointments across all patients
        const pRes = await patientAPI.getAll({ page: 0, size: 100 });
        const pts = pRes.data.data.content;
        setPatients(pts);
        const dRes = await doctorAPI.getAll({ page: 0, size: 100 });
        setDoctors(dRes.data.data.content);

        // Fetch today appointments for "all" view too
        const aRes = await appointmentAPI.getToday();
        setAppointments(aRes.data.data);
        setTotal(aRes.data.data.length);
      }
    } catch { setError('Failed to load data'); }
    finally { setLoading(false); }
  };

  const openCreate = async () => {
    setError(''); setForm({ patientId:'', doctorId:'', appointmentTime:'', durationMinutes:30, reason:'', priority:'LOW' });
    // Load patients & doctors if not already loaded
    if (!patients.length) {
      const [pRes, dRes] = await Promise.all([
        patientAPI.getAll({ page:0, size:100 }),
        doctorAPI.getAll({ page:0, size:100 })
      ]);
      setPatients(pRes.data.data.content);
      setDoctors(dRes.data.data.content);
    }
    setShowModal(true);
  };

  const handleChange = (e) => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      const payload = {
        ...form,
        patientId: Number(form.patientId),
        doctorId: Number(form.doctorId),
        durationMinutes: Number(form.durationMinutes),
      };
      await appointmentAPI.create(payload);
      setSuccess('Appointment booked successfully!');
      setShowModal(false);
      fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Booking failed — check for scheduling conflicts');
    } finally { setSaving(false); }
  };

  const handleCancel = async (id) => {
    const reason = prompt('Reason for cancellation:');
    if (reason === null) return;
    try {
      await appointmentAPI.cancel(id, reason || 'No reason provided');
      setSuccess('Appointment cancelled');
      fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Cancel failed');
    }
  };

  const handleComplete = async (id) => {
    const notes = prompt('Doctor notes (optional):');
    if (notes === null) return;
    try {
      await appointmentAPI.updateStatus(id, { status: 'COMPLETED', doctorNotes: notes });
      setSuccess('Appointment marked as completed');
      fetchData();
      setTimeout(() => setSuccess(''), 3000);
    } catch { setError('Update failed'); }
  };

  const fmt = (dt) => {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('en-IN', {
      day:'2-digit', month:'short', year:'numeric',
      hour:'2-digit', minute:'2-digit'
    });
  };

  return (
    <div>
      <div className="flex justify-between flex-center" style={{ marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>Appointments</h1>
          <p className="text-muted" style={{ fontSize: 14 }}>{total} records shown</p>
        </div>
        <div className="flex gap-2">
          <button className={`btn ${view==='today' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setView('today')}>📅 Today</button>
          <button className={`btn ${view==='all' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setView('all')}>All</button>
          <button className="btn btn-primary" onClick={openCreate}>+ Book Appointment</button>
        </div>
      </div>

      {success && <div className="alert alert-success">{success}</div>}
      {error && !showModal && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <div className="table-container">
          {loading ? (
            <div style={{ textAlign:'center', padding:40 }}>
              <div className="spinner" style={{ width:32,height:32,margin:'0 auto' }} />
            </div>
          ) : appointments.length === 0 ? (
            <div className="text-center text-muted" style={{ padding:40 }}>
              No appointments found
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Patient</th><th>Doctor</th><th>Date &amp; Time</th>
                  <th>Duration</th><th>Reason</th><th>Priority</th>
                  <th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map(a => (
                  <tr key={a.id}>
                    <td style={{ fontWeight:600 }}>{a.patientName}</td>
                    <td>
                      <div style={{ fontWeight:500 }}>{a.doctorName}</div>
                      <div style={{ fontSize:12, color:'var(--text-muted)' }}>{a.doctorSpecialization}</div>
                    </td>
                    <td style={{ whiteSpace:'nowrap', fontSize:13 }}>{fmt(a.appointmentTime)}</td>
                    <td>{a.durationMinutes} min</td>
                    <td style={{ maxWidth:160, fontSize:13 }}>{a.reason}</td>
                    <td>
                      <span className={`badge ${
                        a.priority==='CRITICAL' ? 'badge-danger' :
                        a.priority==='HIGH' ? 'badge-warning' :
                        a.priority==='MEDIUM' ? 'badge-info' : 'badge-gray'
                      }`}>{a.priority}</span>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_COLORS[a.status] || 'badge-gray'}`}>
                        {a.status}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {(a.status === 'SCHEDULED' || a.status === 'CONFIRMED') && (
                          <>
                            <button className="btn btn-secondary btn-sm" onClick={() => handleComplete(a.id)}>
                              ✓ Complete
                            </button>
                            <button className="btn btn-danger btn-sm" onClick={() => handleCancel(a.id)}>
                              Cancel
                            </button>
                          </>
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

      {/* BOOK APPOINTMENT MODAL */}
      {showModal && (
        <div style={{ position:'fixed',inset:0,background:'rgba(0,0,0,.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:999,padding:20 }}>
          <div className="card" style={{ width:'100%',maxWidth:560 }}>
            <div className="card-header flex justify-between flex-center">
              <span>📅 Book New Appointment</span>
              <button onClick={() => setShowModal(false)} style={{ background:'none',border:'none',fontSize:20,cursor:'pointer' }}>✕</button>
            </div>
            <div className="card-body">
              {error && (
                <div className="alert alert-error">
                  {error.includes('conflict') || error.includes('overlap')
                    ? `⚠️ SCHEDULING CONFLICT: ${error}`
                    : error}
                </div>
              )}
              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label className="form-label">Patient *</label>
                  <select className="form-select" name="patientId" value={form.patientId} onChange={handleChange} required>
                    <option value="">Select patient...</option>
                    {patients.map(p => <option key={p.id} value={p.id}>{p.fullName} ({p.email})</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Doctor *</label>
                  <select className="form-select" name="doctorId" value={form.doctorId} onChange={handleChange} required>
                    <option value="">Select doctor...</option>
                    {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName} — {d.specialization}</option>)}
                  </select>
                </div>
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label">Appointment Date &amp; Time *</label>
                    <input type="datetime-local" className="form-control" name="appointmentTime"
                      value={form.appointmentTime} onChange={handleChange} required
                      min={new Date().toISOString().slice(0,16)} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Duration (minutes)</label>
                    <input type="number" className="form-control" name="durationMinutes"
                      value={form.durationMinutes} onChange={handleChange} min={15} max={120} />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Reason for visit *</label>
                  <textarea className="form-control" name="reason" rows={2}
                    value={form.reason} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Priority</label>
                  <select className="form-select" name="priority" value={form.priority} onChange={handleChange}>
                    {['LOW','MEDIUM','HIGH','CRITICAL'].map(p => <option key={p} value={p}>{p}</option>)}
                  </select>
                </div>
                <div className="flex gap-2" style={{ justifyContent:'flex-end' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
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
