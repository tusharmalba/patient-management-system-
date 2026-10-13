import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';
import { saveAuth } from '../utils/auth';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '',
    password: '', role: 'PATIENT', phoneNumber: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await authAPI.register(form);
      saveAuth(res.data.data);
      navigate('/dashboard');
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed';
      const errors = err.response?.data?.data;
      if (errors && typeof errors === 'object') {
        setError(Object.values(errors).join('. '));
      } else {
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'var(--bg)', padding: 20
    }}>
      <div className="card" style={{ width: '100%', maxWidth: 480 }}>
        <div className="card-header" style={{ textAlign: 'center', fontSize: 18 }}>
          🏥 Create Account
        </div>
        <div className="card-body">
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">First Name</label>
                <input className="form-control" name="firstName"
                  value={form.firstName} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label className="form-label">Last Name</label>
                <input className="form-control" name="lastName"
                  value={form.lastName} onChange={handleChange} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input type="email" className="form-control" name="email"
                value={form.email} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input type="password" className="form-control" name="password"
                value={form.password} onChange={handleChange} required
                placeholder="Min 8 chars, 1 uppercase, 1 digit, 1 special char" />
            </div>
            <div className="form-group">
              <label className="form-label">Phone Number</label>
              <input className="form-control" name="phoneNumber"
                value={form.phoneNumber} onChange={handleChange}
                placeholder="+919876543210" />
            </div>
            <div className="form-group">
              <label className="form-label">Role</label>
              <select className="form-select" name="role"
                value={form.role} onChange={handleChange}>
                <option value="PATIENT">Patient</option>
                <option value="DOCTOR">Doctor</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
          <p style={{ marginTop: 16, textAlign: 'center', fontSize: 14, color: 'var(--text-muted)' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: 'var(--primary)' }}>Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
