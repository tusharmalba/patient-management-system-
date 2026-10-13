// ╔══════════════════════════════════════════════════════════════════════╗
// ║                     AUTH UTILITIES                                   ║
// ║                                                                       ║
// ║  Simple token management using localStorage.                          ║
// ║  No Redux, no Context — pure utility functions.                       ║
// ╚══════════════════════════════════════════════════════════════════════╝

export const saveAuth = (authResponse) => {
  localStorage.setItem('token', authResponse.token);
  localStorage.setItem('user', JSON.stringify({
    userId: authResponse.userId,
    email: authResponse.email,
    fullName: authResponse.fullName,
    role: authResponse.role,
  }));
};

export const getUser = () => {
  try {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  } catch {
    return null;
  }
};

export const getToken = () => localStorage.getItem('token');

export const isAuthenticated = () => !!getToken();

export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  window.location.href = '/login';
};

export const hasRole = (role) => {
  const user = getUser();
  return user?.role === role;
};

export const isAdmin = () => hasRole('ADMIN');
export const isDoctor = () => hasRole('DOCTOR') || hasRole('ADMIN');
