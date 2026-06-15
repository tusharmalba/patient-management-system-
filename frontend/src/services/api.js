// ╔══════════════════════════════════════════════════════════════════════╗
// ║                      AXIOS API SERVICE                               ║
// ║                                                                       ║
// ║  Central Axios instance with:                                         ║
// ║  1. Base URL configuration                                            ║
// ║  2. Request interceptor → auto-attach JWT token                       ║
// ║  3. Response interceptor → handle 401 (token expired)                ║
// ║                                                                       ║
// ║  Every API call goes through this instance.                           ║
// ║  No need to manually add Authorization header in every component.     ║
// ╚══════════════════════════════════════════════════════════════════════╝


import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
};

export const patientAPI = {
  create:          (data)          => api.post('/patients', data),
  update:          (id, data)      => api.put(`/patients/${id}`, data),
  delete:          (id)            => api.delete(`/patients/${id}`),
  getById:         (id)            => api.get(`/patients/${id}`),
  getAll:          (params)        => api.get('/patients', { params }),
  search:          (name, params)  => api.get('/patients/search', { params: { name, ...params } }),
  getPriorityQueue:(params)        => api.get('/patients/priority-queue', { params }),
  getHighRisk:     ()              => api.get('/patients/high-risk'),
};

export const doctorAPI = {
  create:             (data)         => api.post('/doctors', data),
  update:             (id, data)     => api.put(`/doctors/${id}`, data),
  delete:             (id)           => api.delete(`/doctors/${id}`),
  getById:            (id)           => api.get(`/doctors/${id}`),
  getAll:             (params)       => api.get('/doctors', { params }),
  search:             (name, params) => api.get('/doctors/search', { params: { name, ...params } }),
  getBySpecialization:(spec, params) => api.get('/doctors/specialization', { params: { specialization: spec, ...params } }),
};

export const appointmentAPI = {
  create:         (data)           => api.post('/appointments', data),
  update:         (id, data)       => api.put(`/appointments/${id}`, data),
  cancel:         (id, reason)     => api.patch(`/appointments/${id}/cancel`, { reason }),
  updateStatus:   (id, data)       => api.patch(`/appointments/${id}/status`, data),
  getById:        (id)             => api.get(`/appointments/${id}`),
  // BUG 1 FIX: /my returns appointments scoped to logged-in user's role
  getMy:          (params)         => api.get('/appointments/my', { params }),
  getByPatient:   (patientId, params) => api.get(`/appointments/patient/${patientId}`, { params }),
  getByDoctor:    (doctorId, params)  => api.get(`/appointments/doctor/${doctorId}`, { params }),
  getToday:       ()               => api.get('/appointments/today'),
};

export const dashboardAPI = {
  getStats: () => api.get('/dashboard/stats'),
};

export const reportAPI = {
  upload:     (patientId, formData) => api.post(`/reports/patient/${patientId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  getByPatient: (patientId) => api.get(`/reports/patient/${patientId}`),
  download:     (reportId)  => api.get(`/reports/${reportId}/download`, { responseType: 'blob' }),
};