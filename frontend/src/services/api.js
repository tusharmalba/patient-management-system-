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

// Create Axios instance with base configuration
// In production (Render), set VITE_API_URL to the backend's public URL,
// e.g. https://pms-backend.onrender.com  (no trailing slash, no /api).
// In local dev it's empty, so '/api' is used and the Vite proxy forwards it.
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

const api = axios.create({
  baseURL: `${API_BASE}/api`,
  timeout: 30000,            // 30 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// ─────────────────────────────────────────────
// REQUEST INTERCEPTOR
// Runs BEFORE every request is sent
// ─────────────────────────────────────────────

/**
 * Automatically attach JWT token to every request.
 *
 * Flow:
 * Component calls api.get('/patients')
 *   → Interceptor reads token from localStorage
 *   → Adds: Authorization: Bearer eyJ...
 *   → Request sent to server
 *
 * This means you NEVER need to manually add Authorization header!
 */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─────────────────────────────────────────────
// RESPONSE INTERCEPTOR
// Runs AFTER every response is received
// ─────────────────────────────────────────────

/**
 * Handle authentication errors globally.
 *
 * 401 Unauthorized → Token expired or invalid
 *   → Clear stored token
 *   → Redirect to login page
 *
 * Without this, users would see cryptic errors
 * instead of being redirected to login.
 */
api.interceptors.response.use(
  (response) => response,  // Pass successful responses through
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid → force re-login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// ─────────────────────────────────────────────
// AUTH API CALLS
// ─────────────────────────────────────────────

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
};

// ─────────────────────────────────────────────
// PATIENT API CALLS
// ─────────────────────────────────────────────

export const patientAPI = {
  create: (data) => api.post('/patients', data),
  update: (id, data) => api.put(`/patients/${id}`, data),
  delete: (id) => api.delete(`/patients/${id}`),
  getById: (id) => api.get(`/patients/${id}`),

  // Pagination: { page, size, sort }
  getAll: (params) => api.get('/patients', { params }),
  search: (name, params) => api.get('/patients/search', { params: { name, ...params } }),
  getPriorityQueue: (params) => api.get('/patients/priority-queue', { params }),
  getHighRisk: () => api.get('/patients/high-risk'),
};

// ─────────────────────────────────────────────
// DOCTOR API CALLS
// ─────────────────────────────────────────────

export const doctorAPI = {
  create: (data) => api.post('/doctors', data),
  update: (id, data) => api.put(`/doctors/${id}`, data),
  delete: (id) => api.delete(`/doctors/${id}`),
  getById: (id) => api.get(`/doctors/${id}`),
  getAll: (params) => api.get('/doctors', { params }),
  search: (name, params) => api.get('/doctors/search', { params: { name, ...params } }),
  getBySpecialization: (spec, params) =>
    api.get('/doctors/specialization', { params: { specialization: spec, ...params } }),
};

// ─────────────────────────────────────────────
// APPOINTMENT API CALLS
// ─────────────────────────────────────────────

export const appointmentAPI = {
  create: (data) => api.post('/appointments', data),
  update: (id, data) => api.put(`/appointments/${id}`, data),
  cancel: (id, reason) => api.patch(`/appointments/${id}/cancel`, { reason }),
  updateStatus: (id, data) => api.patch(`/appointments/${id}/status`, data),
  getById: (id) => api.get(`/appointments/${id}`),
  getByPatient: (patientId, params) => api.get(`/appointments/patient/${patientId}`, { params }),
  getByDoctor: (doctorId, params) => api.get(`/appointments/doctor/${doctorId}`, { params }),
  getToday: () => api.get('/appointments/today'),
};

// ─────────────────────────────────────────────
// DASHBOARD API CALLS
// ─────────────────────────────────────────────

export const dashboardAPI = {
  getStats: () => api.get('/dashboard/stats'),
};

// ─────────────────────────────────────────────
// REPORTS API CALLS
// ─────────────────────────────────────────────

export const reportAPI = {
  upload: (patientId, formData) =>
    api.post(`/reports/patient/${patientId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  getByPatient: (patientId) => api.get(`/reports/patient/${patientId}`),
  download: (reportId) => api.get(`/reports/${reportId}/download`, { responseType: 'blob' }),
};
