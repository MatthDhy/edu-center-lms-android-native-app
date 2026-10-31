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

// Response interceptor: trả thẳng body { status, message, data }; xử lý 401 hết hạn
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/api/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
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

// ---------- Tài khoản ----------
export const adminApi = {
  getUsers: (params) => apiClient.get('/api/admin/users', { params }),
  createUser: (data) => apiClient.post('/api/admin/users', data),
  updateStatus: (id, status) => apiClient.patch(`/api/admin/users/${id}/status`, { status }),
  resetPassword: (id) => apiClient.post(`/api/admin/users/${id}/reset-password`),
  deleteUser: (id) => apiClient.delete(`/api/admin/users/${id}`),
  getStats: () => apiClient.get('/api/admin/users/stats'),
};

// ---------- Lớp học, buổi học, học viên ----------
export const courseApi = {
  list: (params) => apiClient.get('/api/admin/courses', { params }),
  get: (id) => apiClient.get(`/api/admin/courses/${id}`),
  create: (data) => apiClient.post('/api/admin/courses', data),
  update: (id, data) => apiClient.put(`/api/admin/courses/${id}`, data),

  lessons: (courseId) => apiClient.get(`/api/admin/courses/${courseId}/lessons`),
  addLesson: (courseId, data) => apiClient.post(`/api/admin/courses/${courseId}/lessons`, data),
  generateLessons: (courseId, data) => apiClient.post(`/api/admin/courses/${courseId}/lessons/generate`, data),
  updateLesson: (lessonId, data) => apiClient.put(`/api/admin/lessons/${lessonId}`, data),
  cancelLesson: (lessonId) => apiClient.delete(`/api/admin/lessons/${lessonId}`),
  schedule: (from, to) => apiClient.get('/api/admin/lessons', { params: { from, to } }),

  materials: (courseId) => apiClient.get(`/api/courses/${courseId}/materials`),
  addMaterial: (courseId, formData) =>
    apiClient.post(`/api/courses/${courseId}/materials`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteMaterial: (id) => apiClient.delete(`/api/materials/${id}`),

  roster: (courseId) => apiClient.get(`/api/admin/courses/${courseId}/enrollments`),
  enroll: (courseId, studentIds) => apiClient.post(`/api/admin/courses/${courseId}/enrollments`, { studentIds }),
  removeStudent: (courseId, studentId) => apiClient.delete(`/api/admin/courses/${courseId}/enrollments/${studentId}`),

  // Admin chỉ được ĐỌC bài tập (giao/chấm bài do giảng viên làm trên app)
  assignments: (courseId) => apiClient.get(`/api/courses/${courseId}/assignments`),
};

export const assignmentApi = {
  submissions: (assignmentId) => apiClient.get(`/api/assignments/${assignmentId}/submissions`),
};

// ---------- Thông báo ----------
export const notificationApi = {
  broadcast: (data) => apiClient.post('/api/admin/notifications/broadcast', data),
};
