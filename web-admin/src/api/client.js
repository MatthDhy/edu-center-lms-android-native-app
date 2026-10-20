import axios from 'axios';

// Dev local: VITE_API_BASE_URL=http://localhost:8080 (trong .env)
// Docker:    VITE_API_BASE_URL=""  → relative URL → nginx proxy đến backend:8080
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: tự động đính kèm JWT Bearer Token nếu đã đăng nhập
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('celehub_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: xử lý 401 hết hạn hoặc không có quyền
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('celehub_token');
      localStorage.removeItem('celehub_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    const message = error.response?.data?.message || error.message || 'Đã có lỗi xảy ra';
    return Promise.reject(new Error(message));
  }
);

export const authApi = {
  login: (email, password) => apiClient.post('/api/auth/login', { email, password }),
  getMe: () => apiClient.get('/api/users/me'),
};

export const adminApi = {
  getUsers: (params) => apiClient.get('/api/admin/users', { params }),
  createUser: (data) => apiClient.post('/api/admin/users', data),
  updateStatus: (id, status) => apiClient.patch(`/api/admin/users/${id}/status`, { status }),
  resetPassword: (id) => apiClient.post(`/api/admin/users/${id}/reset-password`),
  getStats: () => apiClient.get('/api/admin/users/stats'),
};
