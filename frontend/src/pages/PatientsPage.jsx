import { useState, useEffect } from 'react';
import { patientAPI } from '../services/api';
import { isAdmin, isDoctor } from '../utils/auth';

const EMPTY_FORM = {
  firstName: '', lastName: '', email: '', phoneNumber: '',
  dateOfBirth: '', gender: '', address: '', bloodGroup: '',
  medicalHistory: '', currentMedications: '', allergies: '',
  primaryDiagnosis: '', hasDiabetes: false, isSmoker: false,
  hasHeartDisease: false, hasHighBloodPressure: false,
  priority: 'LOW', emergencyNotes: '', password: ''
};

export default function PatientsPage() {
  const [patients, setPatients]   = useState([]);
  const [total, setTotal]         = useState(0);
  const [page, setPage]           = useState(0);
  const [search, setSearch]       = useState('');
  const [loading, setLoading]     = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editPatient, setEditPatient] = useState(null);
  const [form, setForm]           = useState(EMPTY_FORM);
  const [saving, setSaving]       = useState(false);
  const [error, setError]         = useState('');
  const [successMsg, setSuccess]  = useState('');

  const PAGE_SIZE = 10;
  const canEdit = isDoctor();
  const canDelete = isAdmin();

  // ── Fetch patients whenever page or search changes ──
  useEffect(() => {
    fetchPatients();
  }, [page, search]);

  const fetchPatients = async () => {
    setLoading(true);
    try {
      const params = { page, size: PAGE_SIZE, sort: 'createdAt,desc' };
      const res = search
        ? await patientAPI.search(search, params)
        : await patientAPI.getAll(params);
      setPatients(res.data.data.content);
      setTotal(res.data.data.totalElements);
    } catch {
      setError('Failed to load patients');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => { setForm(EMPTY_FORM); setEditPatient(null); setError(''); setShowModal(true); };
  const openEdit = (p) => {
    setForm({
      firstName: p.firstName, lastName: p.lastName, email: p.email,
      phoneNumber: p.phoneNumber || '', dateOfBirth: p.dateOfBirth || '',
      gender: p.gender || '', address: p.address || '', bloodGroup: p.bloodGroup || '',
      medicalHistory: p.medicalHistory || '', currentMedications: p.currentMedications || '',
      allergies: p.allergies || '', primaryDiagnosis: p.primaryDiagnosis || '',
      hasDiabetes: !!p.hasDiabetes, isSmoker: !!p.isSmoker,
      hasHeartDisease: !!p.hasHeartDisease, hasHighBloodPressure: !!p.hasHighBloodPressure,
      priority: p.priority || 'LOW', emergencyNotes: p.emergencyNotes || '', password: ''
    });
    setEditPatient(p);
    setError('');
    setShowModal(true);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      const payload = { ...form };
      if (!payload.password) delete payload.password;
      if (editPatient) {
        await patientAPI.update(editPatient.id, payload);
        setSuccess('Patient updated successfully!');
      } else {
        await patientAPI.create(payload);
        setSuccess('Patient created successfully!');
      }
      setShowModal(false);
      fetchPatients();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      const errs = err.response?.data?.data;
      if (errs && typeof errs === 'object') setError(Object.values(errs).join('. '));
      else setError(err.response?.data?.message || 'Operation failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id, name) => {
    if (!confirm(`Soft-delete patient "${name}"? Record will be hidden but preserved.`)) return;
    try {
      await patientAPI.delete(id);
      setSuccess('Patient deleted (soft-delete)');
      fetchPatients();
      setTimeout(() => setSuccess(''), 3000);
    } catch { setError('Delete failed'); }
  };

  const riskBadge = (risk) => {
    const map = { HIGH: 'badge-danger', MEDIUM: 'badge-warning', LOW: 'badge-success' };
    return <span className={`badge ${map[risk] || 'badge-gray'}`}>{risk}</span>;
  };

  const priorityBadge = (p) => {
    const map = { CRITICAL: 'badge-danger', HIGH: 'badge-warning', MEDIUM: 'badge-info', LOW: 'badge-gray' };
    return <span className={`badge ${map[p] || 'badge-gray'}`}>{p}</span>;
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between flex-center" style={{ marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>Patients</h1>
          <p className="text-muted" style={{ fontSize: 14, marginTop: 2 }}>{total} total records</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={openCreate}>+ Add Patient</button>
        )}
      </div>

      {successMsg && <div className="alert alert-success">{successMsg}</div>}
      {error && !showModal && <div className="alert alert-error">{error}</div>}

      {/* Search */}
      <div className="card" style={{ marginBottom: 16, padding: 16 }}>
        <input
          className="form-control" placeholder="🔍 Search patients by name..."
          value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          style={{ maxWidth: 400 }}
        />
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          {loading ? (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <div className="spinner" style={{ width: 32, height: 32, margin: '0 auto' }} />
            </div>
          ) : patients.length === 0 ? (
            <div className="text-center text-muted" style={{ padding: 40 }}>
              No patients found
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Name</th><th>Email</th><th>Age</th><th>Blood</th>
                  <th>Risk</th><th>Priority</th><th>Diagnosis</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {patients.map(p => (
                  <tr key={p.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{p.fullName}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.gender}</div>
                    </td>
                    <td>{p.email}</td>
                    <td>{p.age ?? '—'}</td>
                    <td>{p.bloodGroup?.replace('_', ' ').replace('POSITIVE', '+').replace('NEGATIVE', '-') || '—'}</td>
                    <td>{riskBadge(p.riskLevel)}</td>
                    <td>{priorityBadge(p.priority)}</td>
                    <td style={{ maxWidth: 180 }}>
                      <span style={{ fontSize: 13 }}>{p.primaryDiagnosis || '—'}</span>
                    </td>
                    <td>
                      <div className="flex gap-2">
                        {canEdit && (
                          <button className="btn btn-secondary btn-sm" onClick={() => openEdit(p)}>
                            Edit
                          </button>
                        )}
                        {canDelete && (
                          <button className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(p.id, p.fullName)}>
                            Delete
                          </button>
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

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>← Prev</button>
          {[...Array(totalPages)].map((_, i) => (
            <button key={i} className={i === page ? 'active' : ''} onClick={() => setPage(i)}>
              {i + 1}
            </button>
          ))}
          <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next →</button>
        </div>
      )}

      {/* MODAL */}
      {showModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 999,
          padding: 20
        }}>
          <div className="card" style={{ width: '100%', maxWidth: 680, maxHeight: '90vh', overflow: 'auto' }}>
            <div className="card-header flex justify-between flex-center">
              <span>{editPatient ? 'Edit Patient' : 'Add New Patient'}</span>
              <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer' }}>✕</button>
            </div>
            <div className="card-body">
              {error && <div className="alert alert-error">{error}</div>}
              <form onSubmit={handleSubmit}>
                {/* Personal Info */}
                <div className="grid-2">
                  <div className="form-group">
                    <label className="form-label">First Name *</label>
                    <input className="form-control" name="firstName" value={form.firstName} onChange={handleChange} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Last Name *</label>
                    <input className="form-control" name="lastName" value={form.lastName} onChange={handleChange} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Email *</label>
                    <input type="email" className="form-control" name="email" value={form.email} onChange={handleChange} required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Phone</label>
                    <input className="form-control" name="phoneNumber" value={form.phoneNumber} onChange={handleChange} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Date of Birth</label>
                    <input type="date" className="form-control" name="dateOfBirth" value={form.dateOfBirth} onChange={handleChange} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Gender</label>
                    <select className="form-select" name="gender" value={form.gender} onChange={handleChange}>
                      <option value="">Select</option>
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Blood Group</label>
                    <select className="form-select" name="bloodGroup" value={form.bloodGroup} onChange={handleChange}>
                      <option value="">Select</option>
                      {['A_POSITIVE','A_NEGATIVE','B_POSITIVE','B_NEGATIVE','AB_POSITIVE','AB_NEGATIVE','O_POSITIVE','O_NEGATIVE'].map(b => (
                        <option key={b} value={b}>{b.replace('_POSITIVE','+').replace('_NEGATIVE','-').replace('_',' ')}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Priority</label>
                    <select className="form-select" name="priority" value={form.priority} onChange={handleChange}>
                      {['CRITICAL','HIGH','MEDIUM','LOW'].map(p => <option key={p} value={p}>{p}</option>)}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Primary Diagnosis</label>
                  <input className="form-control" name="primaryDiagnosis" value={form.primaryDiagnosis} onChange={handleChange} />
                </div>
                <div className="form-group">
                  <label className="form-label">Medical History</label>
                  <textarea className="form-control" name="medicalHistory" value={form.medicalHistory} onChange={handleChange} rows={2} />
                </div>
                <div className="form-group">
                  <label className="form-label">Current Medications</label>
                  <textarea className="form-control" name="currentMedications" value={form.currentMedications} onChange={handleChange} rows={2} />
                </div>
                <div className="form-group">
                  <label className="form-label">Allergies</label>
                  <input className="form-control" name="allergies" value={form.allergies} onChange={handleChange} />
                </div>

                {/* Risk Factors */}
                <div style={{ background: '#fef3c7', borderRadius: 8, padding: 16, marginBottom: 16 }}>
                  <div style={{ fontWeight: 600, marginBottom: 10 }}>⚠️ Risk Factors (auto-calculates risk score)</div>
                  <div className="grid-2">
                    {[
                      { name: 'hasDiabetes', label: 'Diabetes' },
                      { name: 'isSmoker', label: 'Smoker' },
                      { name: 'hasHeartDisease', label: 'Heart Disease' },
                      { name: 'hasHighBloodPressure', label: 'High Blood Pressure' },
                    ].map(({ name, label }) => (
                      <label key={name} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                        <input type="checkbox" name={name} checked={form[name]} onChange={handleChange} />
                        {label}
                      </label>
                    ))}
                  </div>
                </div>

                {!editPatient && (
                  <div className="form-group">
                    <label className="form-label">Password (optional — creates login account)</label>
                    <input type="password" className="form-control" name="password" value={form.password} onChange={handleChange} />
                  </div>
                )}

                <div className="flex gap-2" style={{ justifyContent: 'flex-end', marginTop: 8 }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : editPatient ? 'Update Patient' : 'Create Patient'}
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
