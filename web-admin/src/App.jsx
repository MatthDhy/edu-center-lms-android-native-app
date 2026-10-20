import React, { useState } from 'react';
import { useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UserListPage from './pages/UserListPage';
import ArchitecturePage from './pages/ArchitecturePage';
import CreateUserModal from './components/CreateUserModal';

const PAGE_META = {
  dashboard: {
    title: 'Tổng quan & Thống kê',
    subtitle: 'Dashboard quản trị toàn hệ thống CeLeHub'
  },
  teachers: {
    title: 'Quản lý Giảng viên',
    subtitle: 'Cấp tài khoản, khóa/mở khóa, reset mật khẩu Giảng viên (ROLE_TEACHER)'
  },
  students: {
    title: 'Quản lý Học viên',
    subtitle: 'Cấp tài khoản, khóa/mở khóa, reset mật khẩu Học viên (ROLE_STUDENT)'
  },
  architecture: {
    title: 'Cơ chế Nối DB & Phân quyền',
    subtitle: 'Tài liệu kỹ thuật: JWT, CORS, Role-based Access Control'
  },
};

export default function App() {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createModalRole, setCreateModalRole] = useState('STUDENT');

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const handleOpenCreateModal = (role) => {
    setCreateModalRole(role);
    setCreateModalOpen(true);
  };

  const meta = PAGE_META[activeTab] || PAGE_META.dashboard;

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      <div className="flex-1 flex flex-col min-h-screen overflow-hidden">
        <Header title={meta.title} subtitle={meta.subtitle} />

        <main className="flex-1 p-6 lg:p-8 overflow-y-auto">
          {activeTab === 'dashboard' && (
            <DashboardPage
              onNavigate={setActiveTab}
              onOpenCreateModal={handleOpenCreateModal}
            />
          )}
          {activeTab === 'teachers' && (
            <UserListPage
              role="TEACHER"
              title="Danh sách Giảng viên"
              subtitle="Quản lý tất cả tài khoản có vai trò TEACHER — Sử dụng app Android phiên bản Giảng viên"
            />
          )}
          {activeTab === 'students' && (
            <UserListPage
              role="STUDENT"
              title="Danh sách Học viên"
              subtitle="Quản lý tất cả tài khoản có vai trò STUDENT — Sử dụng app Android phiên bản Học viên"
            />
          )}
          {activeTab === 'architecture' && <ArchitecturePage />}
        </main>
      </div>

      {/* Global Create Modal (opened from Dashboard) */}
      <CreateUserModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        defaultRole={createModalRole}
        onSuccess={() => setCreateModalOpen(false)}
      />
    </div>
  );
}
