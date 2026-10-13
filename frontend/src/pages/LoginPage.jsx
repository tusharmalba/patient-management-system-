import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';
import { saveAuth } from '../utils/auth';

/**
 * LOGIN PAGE
 *
 * Uses useState for form state (no Redux, no Context).
 * Uses useNavigate for programmatic navigation.
 * Calls authAPI.login() → receives JWT → saves to localStorage.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
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
      const res = await authAPI.login(form);
      saveAuth(res.data.data);         // Store token + user info
      navigate('/dashboard');          // Redirect to dashboard
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center',
      justifyContent: 'center', background: 'var(--bg)'
    }}>
      <div className="card" style={{ width: '100%', maxWidth: 400 }}>
        <div className="card-header" style={{ textAlign: 'center', fontSize: 18 }}>
          🏥 Patient Management System
        </div>
        <div className="card-body">
          <h2 style={{ marginBottom: 20, fontSize: 22, fontWeight: 700 }}>Welcome back</h2>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                type="email" name="email" className="form-control"
                placeholder="admin@pms.com"
                value={form.email} onChange={handleChange} required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password" name="password" className="form-control"
                placeholder="••••••••"
                value={form.password} onChange={handleChange} required
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? <><span className="spinner" style={{width:16,height:16}} /> Signing in...</> : 'Sign In'}
            </button>
          </form>

          <p style={{ marginTop: 16, textAlign: 'center', fontSize: 14, color: 'var(--text-muted)' }}>
            Don't have an account?{' '}
            <Link to="/register" style={{ color: 'var(--primary)' }}>Register</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
