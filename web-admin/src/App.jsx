import React, { useState } from 'react';
import { useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UserListPage from './pages/UserListPage';
import CoursesPage from './pages/CoursesPage';
import TimetablePage from './pages/TimetablePage';
import AssignmentsPage from './pages/AssignmentsPage';
import BroadcastPage from './pages/BroadcastPage';
import CreateUserModal from './components/CreateUserModal';

const PAGE_META = {
  dashboard: { title: 'Tổng quan', subtitle: 'Số liệu nhanh của trung tâm' },
  teachers: { title: 'Giảng viên', subtitle: 'Cấp tài khoản, khóa/mở khóa, cấp lại mật khẩu' },
  students: { title: 'Học viên', subtitle: 'Cấp tài khoản, khóa/mở khóa, cấp lại mật khẩu' },
  courses: { title: 'Lớp học', subtitle: 'Tạo lớp, gán giảng viên, xếp lịch, xếp học viên, tài liệu' },
  timetable: { title: 'Thời khóa biểu', subtitle: 'Lưới tuần theo phòng, cảnh báo trùng giờ' },
  assignments: { title: 'Theo dõi bài tập', subtitle: 'Tiến độ nộp bài của từng lớp' },
  notifications: { title: 'Gửi thông báo', subtitle: 'Tới tất cả, học viên, giảng viên hoặc một lớp' },
};

export default function App() {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [createOpen, setCreateOpen] = useState(false);
  const [createRole, setCreateRole] = useState('STUDENT');

  if (!isAuthenticated) return <LoginPage />;

  const openCreate = (role) => { setCreateRole(role); setCreateOpen(true); };
  const meta = PAGE_META[activeTab] || PAGE_META.dashboard;

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
      <div className="flex-1 flex flex-col min-h-screen overflow-hidden">
        <Header title={meta.title} subtitle={meta.subtitle} />
        <main className="flex-1 p-6 lg:p-8 overflow-y-auto">
          {activeTab === 'dashboard' && <DashboardPage onNavigate={setActiveTab} onOpenCreateModal={openCreate} />}
          {activeTab === 'teachers' && <UserListPage role="TEACHER" title="Danh sách giảng viên" subtitle="Tài khoản dùng app Android bản giảng viên" />}
          {activeTab === 'students' && <UserListPage role="STUDENT" title="Danh sách học viên" subtitle="Tài khoản dùng app Android bản học viên" />}
          {activeTab === 'courses' && <CoursesPage />}
          {activeTab === 'timetable' && <TimetablePage />}
          {activeTab === 'assignments' && <AssignmentsPage />}
          {activeTab === 'notifications' && <BroadcastPage />}
        </main>
      </div>
      <CreateUserModal isOpen={createOpen} onClose={() => setCreateOpen(false)}
        defaultRole={createRole} onSuccess={() => setCreateOpen(false)} />
    </div>
  );
}