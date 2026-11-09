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
import AttendancePage from './pages/AttendancePage';
import LeavePage from './pages/LeavePage';
import FinancePage from './pages/FinancePage';
import PayrollPage from './pages/PayrollPage';
import BroadcastPage from './pages/BroadcastPage';
import CreateUserModal from './components/CreateUserModal';

const PAGE_META = {
  dashboard: { title: 'Tổng quan hệ thống', crumb: 'Tổng quan', subtitle: 'Theo dõi hoạt động dạy và học trên CeLeHub.' },
  teachers: { title: 'Quản lý giảng viên', crumb: 'Quản lý giảng viên', subtitle: 'Cấp tài khoản, nhập Excel, khóa/mở khóa, cấp lại mật khẩu.' },
  students: { title: 'Quản lý học viên', crumb: 'Quản lý học viên', subtitle: 'Cấp tài khoản, nhập Excel, khóa/mở khóa, cấp lại mật khẩu.' },
  courses: { title: 'Quản lý lớp học', crumb: 'Quản lý lớp học', subtitle: 'Tạo lớp, gán giảng viên, xếp lịch, xếp học viên, tài liệu.' },
  timetable: { title: 'Thời khóa biểu', crumb: 'Thời khóa biểu', subtitle: 'Lưới tuần theo phòng, cảnh báo trùng giờ.' },
  attendance: { title: 'Chuyên cần', crumb: 'Chuyên cần', subtitle: 'Ma trận điểm danh học viên theo buổi, cảnh báo vắng > 20%.' },
  leave: { title: 'Đơn xin nghỉ & học bù', crumb: 'Đơn xin nghỉ', subtitle: 'Giảng viên duyệt trên app, Admin theo dõi toàn trung tâm.' },
  assignments: { title: 'Bài tập & Quiz', crumb: 'Bài tập & Quiz', subtitle: 'Tiến độ nộp bài và kết quả quiz của từng lớp.' },
  finance: { title: 'Học phí', crumb: 'Học phí', subtitle: 'Doanh thu, công nợ, đối soát VietQR và thu tiền mặt.' },
  payroll: { title: 'Lương giảng viên', crumb: 'Lương giảng viên', subtitle: 'Đơn giá, tính lương theo buổi đã điểm danh, duyệt bảng lương.' },
  notifications: { title: 'Gửi thông báo', crumb: 'Gửi thông báo', subtitle: 'Tới tất cả, học viên, giảng viên hoặc một lớp (kèm ảnh).' },
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
    <div className="flex min-h-screen bg-[#F5F5FB]">
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
      <div className="flex-1 flex flex-col min-h-screen min-w-0">
        <Header title={meta.crumb} />
        <main className="flex-1 p-6 lg:p-8 overflow-y-auto">
          <div className="mb-6">
            <h1 className="text-3xl font-extrabold tracking-tight text-ink">{meta.title}</h1>
            <p className="text-sm text-slate-500 mt-1">{meta.subtitle}</p>
          </div>
          {activeTab === 'dashboard' && <DashboardPage onNavigate={setActiveTab} onOpenCreateModal={openCreate} />}
          {activeTab === 'teachers' && <UserListPage role="TEACHER" title="Danh sách giảng viên" subtitle="Tài khoản dùng app Android bản giảng viên" />}
          {activeTab === 'students' && <UserListPage role="STUDENT" title="Danh sách học viên" subtitle="Tài khoản dùng app Android bản học viên" />}
          {activeTab === 'courses' && <CoursesPage />}
          {activeTab === 'timetable' && <TimetablePage />}
          {activeTab === 'attendance' && <AttendancePage />}
          {activeTab === 'leave' && <LeavePage />}
          {activeTab === 'assignments' && <AssignmentsPage />}
          {activeTab === 'finance' && <FinancePage />}
          {activeTab === 'payroll' && <PayrollPage />}
          {activeTab === 'notifications' && <BroadcastPage />}
        </main>
      </div>
      <CreateUserModal isOpen={createOpen} onClose={() => setCreateOpen(false)} defaultRole={createRole} onSuccess={() => setCreateOpen(false)} />
    </div>
  );
}
