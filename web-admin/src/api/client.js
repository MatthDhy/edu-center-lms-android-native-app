import axios from 'axios';

// Dev local: VITE_API_BASE_URL=http://localhost:8080 (trong .env)
// Docker:    VITE_API_BASE_URL=""  → relative URL → nginx proxy đến backend:8080
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('celehub_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// Trả thẳng body { status, message, data }; xử lý 401 hết hạn
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/api/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
      localStorage.removeItem('celehub_token');
      localStorage.removeItem('celehub_user');
      if (window.location.pathname !== '/login') window.location.href = '/login';
    }
    const message = error.response?.data?.message || error.message || 'Đã có lỗi xảy ra';
    return Promise.reject(new Error(message));
  }
);

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } };

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
  importUsers: (formData) => apiClient.post('/api/admin/users/import', formData, multipart),
  importTemplate: () => apiClient.get('/api/admin/users/import/template', { responseType: 'blob' }),
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
  addMaterial: (courseId, formData) => apiClient.post(`/api/courses/${courseId}/materials`, formData, multipart),
  deleteMaterial: (id) => apiClient.delete(`/api/materials/${id}`),

  roster: (courseId) => apiClient.get(`/api/admin/courses/${courseId}/enrollments`),
  enroll: (courseId, studentIds) => apiClient.post(`/api/admin/courses/${courseId}/enrollments`, { studentIds }),
  removeStudent: (courseId, studentId) => apiClient.delete(`/api/admin/courses/${courseId}/enrollments/${studentId}`),

  // Admin chỉ được ĐỌC bài tập / quiz (giao, chấm do giảng viên làm trên app)
  assignments: (courseId) => apiClient.get(`/api/courses/${courseId}/assignments`),
};

export const assignmentApi = {
  submissions: (assignmentId) => apiClient.get(`/api/assignments/${assignmentId}/submissions`),
};

export const quizApi = {
  byCourse: (courseId) => apiClient.get(`/api/courses/${courseId}/quizzes`),
  results: (quizId) => apiClient.get(`/api/quizzes/${quizId}/results`),
};

// ---------- Thông báo ----------
export const notificationApi = {
  /** data có thể là object (JSON) hoặc FormData (khi kèm ảnh). */
  broadcast: (data) =>
    data instanceof FormData
      ? apiClient.post('/api/admin/notifications/broadcast', data, multipart)
      : apiClient.post('/api/admin/notifications/broadcast', data),
};

// ---------- Điểm danh / nghỉ-bù ----------
export const attendanceApi = {
  matrix: (courseId, from, to) => apiClient.get('/api/admin/attendance', { params: { courseId, from: from || undefined, to: to || undefined } }),
};

export const leaveApi = {
  list: (status) => apiClient.get('/api/admin/leave-requests', { params: { status: status || undefined } }),
};

// ---------- Học phí & doanh thu ----------
export const financeApi = {
  overview: (month) => apiClient.get('/api/admin/finance/overview', { params: { month } }),
  course: (courseId, month) => apiClient.get(`/api/admin/finance/courses/${courseId}`, { params: { month } }),
  bills: (params) => apiClient.get('/api/admin/tuition/bills', { params }),
  createBills: (data) => apiClient.post('/api/admin/tuition/bills', data),
  confirmCash: (id) => apiClient.post(`/api/admin/tuition/bills/${id}/confirm-cash`),
  deleteBill: (id) => apiClient.delete(`/api/admin/tuition/bills/${id}`),
  simulate: (data) => apiClient.post('/api/admin/tuition/simulate-payment', data),
  sendMonthlyReminder: () => apiClient.post('/api/admin/tuition/send-monthly-reminder'),
};

// ---------- Lương giảng viên ----------
export const payrollApi = {
  rates: () => apiClient.get('/api/admin/payroll/rates'),
  setRate: (teacherId, hourlyRate) => apiClient.put(`/api/admin/teachers/${teacherId}/rate`, { hourlyRate }),
  generate: (month) => apiClient.post('/api/admin/payroll/generate', null, { params: { month } }),
  view: (month) => apiClient.get('/api/admin/payroll', { params: { month } }),
  items: (month, teacherId) => apiClient.get('/api/admin/payroll/items', { params: { month, teacherId } }),
  approve: (periodId) => apiClient.patch(`/api/admin/payroll/${periodId}/approve`),
};
