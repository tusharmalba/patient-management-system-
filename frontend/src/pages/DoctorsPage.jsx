import { useState, useEffect } from 'react';
import { doctorAPI } from '../services/api';
import { isAdmin } from '../utils/auth';

const EMPTY_FORM = {
  firstName:'', lastName:'', email:'', phoneNumber:'', specialization:'',
  licenseNumber:'', experienceYears:'', qualification:'', bio:'',
  consultationFee:'', availableDays:'', availableFrom:'', availableTo:'',
  appointmentDurationMinutes:30, password:''
};

export default function DoctorsPage() {
  const [doctors, setDoctors] = useState([]);
  const [total, setTotal]     = useState(0);
  const [page, setPage]       = useState(0);
  const [search, setSearch]   = useState('');
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editDoc, setEditDoc] = useState(null);
  const [form, setForm]       = useState(EMPTY_FORM);
  const [saving, setSaving]   = useState(false);
  const [error, setError]     = useState('');
  const [successMsg, setSuccess] = useState('');
  const adminUser = isAdmin();
  const PAGE_SIZE = 10;

  useEffect(() => { fetchDoctors(); }, [page, search]);

  const fetchDoctors = async () => {
    setLoading(true);
    try {
      const params = { page, size: PAGE_SIZE };
      const res = search
        ? await doctorAPI.search(search, params)
        : await doctorAPI.getAll(params);
      setDoctors(res.data.data.content);
      setTotal(res.data.data.totalElements);
    } catch { setError('Failed to load doctors'); }
    finally { setLoading(false); }
  };

  const openCreate = () => { setForm(EMPTY_FORM); setEditDoc(null); setError(''); setShowModal(true); };
  const openEdit = (d) => {
    setForm({
      firstName:d.firstName, lastName:d.lastName, email:d.email,
      phoneNumber:d.phoneNumber||'', specialization:d.specialization,
      licenseNumber:d.licenseNumber||'', experienceYears:d.experienceYears||'',
      qualification:d.qualification||'', bio:d.bio||'',
      consultationFee:d.consultationFee||'', availableDays:d.availableDays||'',
      availableFrom:d.availableFrom||'', availableTo:d.availableTo||'',
      appointmentDurationMinutes:d.appointmentDurationMinutes||30, password:''
    });
    setEditDoc(d); setError(''); setShowModal(true);
  };

  const handleChange = (e) => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      const payload = { ...form };
      if (!payload.password) delete payload.password;
      if (editDoc) await doctorAPI.update(editDoc.id, payload);
      else await doctorAPI.create(payload);
      setSuccess(editDoc ? 'Doctor updated!' : 'Doctor created!');
      setShowModal(false); fetchDoctors();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      const errs = err.response?.data?.data;
      setError(errs && typeof errs === 'object'
        ? Object.values(errs).join('. ')
        : err.response?.data?.message || 'Operation failed');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id, name) => {
    if (!confirm(`Soft-delete Dr. "${name}"?`)) return;
    try { await doctorAPI.delete(id); setSuccess('Doctor removed'); fetchDoctors(); }
    catch { setError('Delete failed'); }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      <div className="flex justify-between flex-center" style={{ marginBottom: 20 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>Doctors</h1>
          <p className="text-muted" style={{ fontSize: 14 }}>{total} total</p>
        </div>
        {adminUser && <button className="btn btn-primary" onClick={openCreate}>+ Add Doctor</button>}
      </div>

      {successMsg && <div className="alert alert-success">{successMsg}</div>}

      <div className="card" style={{ marginBottom: 16, padding: 16 }}>
        <input className="form-control" placeholder="🔍 Search doctors by name..."
          value={search} onChange={e => { setSearch(e.target.value); setPage(0); }}
          style={{ maxWidth: 400 }} />
      </div>

      <div className="card">
        <div className="table-container">
          {loading ? (
            <div style={{ textAlign:'center', padding:40 }}><div className="spinner" style={{ width:32,height:32,margin:'0 auto' }} /></div>
          ) : doctors.length === 0 ? (
            <div className="text-center text-muted" style={{ padding:40 }}>No doctors found</div>
          ) : (
            <table>
              <thead>
                <tr><th>Name</th><th>Specialization</th><th>Email</th><th>Experience</th><th>Fee</th><th>Hours</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {doctors.map(d => (
                  <tr key={d.id}>
                    <td>
                      <div style={{ fontWeight:600 }}>{d.fullName}</div>
                      <div style={{ fontSize:12, color:'var(--text-muted)' }}>{d.qualification}</div>
                    </td>
                    <td><span className="badge badge-info">{d.specialization}</span></td>
                    <td>{d.email}</td>
                    <td>{d.experienceYears ? `${d.experienceYears} yrs` : '—'}</td>
                    <td>{d.consultationFee ? `₹${d.consultationFee}` : '—'}</td>
                    <td style={{ fontSize:13 }}>
                      {d.availableFrom && d.availableTo
                        ? `${d.availableFrom} – ${d.availableTo}` : '—'}
                    </td>
                    <td>
                      <div className="flex gap-2">
                        <button className="btn btn-secondary btn-sm" onClick={() => openEdit(d)}>Edit</button>
                        {adminUser && (
                          <button className="btn btn-danger btn-sm" onClick={() => handleDelete(d.id, d.fullName)}>Delete</button>
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

      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p-1)} disabled={page===0}>← Prev</button>
          {[...Array(totalPages)].map((_,i) => (
            <button key={i} className={i===page?'active':''} onClick={() => setPage(i)}>{i+1}</button>
          ))}
          <button onClick={() => setPage(p => p+1)} disabled={page>=totalPages-1}>Next →</button>
        </div>
      )}

      {showModal && (
        <div style={{ position:'fixed',inset:0,background:'rgba(0,0,0,.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:999,padding:20 }}>
          <div className="card" style={{ width:'100%',maxWidth:640,maxHeight:'90vh',overflow:'auto' }}>
            <div className="card-header flex justify-between flex-center">
              <span>{editDoc ? 'Edit Doctor' : 'Add New Doctor'}</span>
              <button onClick={() => setShowModal(false)} style={{ background:'none',border:'none',fontSize:20,cursor:'pointer' }}>✕</button>
            </div>
            <div className="card-body">
              {error && <div className="alert alert-error">{error}</div>}
              <form onSubmit={handleSubmit}>
                <div className="grid-2">
                  {[
                    { name:'firstName', label:'First Name', required:true },
                    { name:'lastName',  label:'Last Name',  required:true },
                    { name:'email',     label:'Email',      required:true, type:'email' },
                    { name:'phoneNumber', label:'Phone' },
                    { name:'specialization', label:'Specialization', required:true },
                    { name:'licenseNumber',  label:'License Number' },
                    { name:'qualification',  label:'Qualification (MBBS, MD...)' },
                    { name:'experienceYears',label:'Experience (years)', type:'number' },
                    { name:'consultationFee',label:'Consultation Fee (₹)', type:'number' },
                    { name:'availableDays',  label:'Available Days (e.g. MON,TUE)' },
                    { name:'availableFrom',  label:'Available From (HH:MM)', type:'time' },
                    { name:'availableTo',    label:'Available To (HH:MM)',   type:'time' },
                  ].map(({ name, label, required, type='text' }) => (
                    <div key={name} className="form-group">
                      <label className="form-label">{label}{required ? ' *' : ''}</label>
                      <input type={type} className="form-control" name={name}
                        value={form[name]} onChange={handleChange} required={required} />
                    </div>
                  ))}
                </div>
                <div className="form-group">
                  <label className="form-label">Bio</label>
                  <textarea className="form-control" name="bio" value={form.bio} onChange={handleChange} rows={2} />
                </div>
                {!editDoc && (
                  <div className="form-group">
                    <label className="form-label">Password (for login account)</label>
                    <input type="password" className="form-control" name="password" value={form.password} onChange={handleChange} />
                  </div>
                )}
                <div className="flex gap-2" style={{ justifyContent:'flex-end' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? 'Saving...' : editDoc ? 'Update' : 'Create'}
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
