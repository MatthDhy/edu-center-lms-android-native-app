import React, { useState, useEffect } from 'react';
import { 
  Users, 
  GraduationCap, 
  BookOpen, 
  UserCheck, 
  UserX, 
  PlusCircle, 
  ArrowRight,
  Database,
  Layers,
  Smartphone,
  Globe,
  RefreshCw
} from 'lucide-react';
import { adminApi } from '../api/client';

export default function DashboardPage({ onNavigate, onOpenCreateModal }) {
  const [stats, setStats] = useState({
    totalStudents: 0,
    totalTeachers: 0,
    activeUsers: 0,
    lockedUsers: 0,
    totalUsers: 0,
  });
  const [recentUsers, setRecentUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes] = await Promise.all([
        adminApi.getStats(),
        adminApi.getUsers({}),
      ]);
      if (statsRes.data) setStats(statsRes.data);
      if (usersRes.data) setRecentUsers(usersRes.data.slice(0, 5));
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  return (
    <div className="space-y-8 animate-in fade-in duration-150">
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-700 via-brand-600 to-indigo-700 p-8 text-white shadow-xl shadow-brand-700/10">
        <div className="relative z-10 max-w-2xl">
          <span className="inline-block px-3 py-1 rounded-full bg-white/20 text-white text-xs font-semibold backdrop-blur-md mb-3">
            Hệ sinh thái Quản lý Đào tạo CeLeHub
          </span>
          <h2 className="text-2xl lg:text-3xl font-extrabold tracking-tight">
            Trung tâm Điều hành & Phân quyền
          </h2>
          <p className="mt-2 text-sm text-brand-100 leading-relaxed">
            Hệ thống dùng chung <strong>1 Database PostgreSQL duy nhất</strong> cho cả <strong>Web Admin</strong> và <strong>Ứng dụng di động Android</strong> (dành cho Giáo viên & Học sinh).
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              onClick={() => onOpenCreateModal('TEACHER')}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-brand-700 font-bold text-xs shadow-md hover:bg-brand-50 transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Cấp tài khoản Giảng viên</span>
            </button>
            <button
              onClick={() => onOpenCreateModal('STUDENT')}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-800/60 hover:bg-brand-800 text-white font-bold text-xs backdrop-blur-md border border-white/20 transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Cấp tài khoản Học viên</span>
            </button>
            <button
              onClick={() => onNavigate('architecture')}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-transparent hover:bg-white/10 text-white font-semibold text-xs border border-white/30 transition-all"
            >
              <span>Xem sơ đồ kết nối DB</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Top Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Học viên</span>
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
              <GraduationCap className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-800 mt-3">{stats.totalStudents}</p>
          <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
            <Smartphone className="w-3.5 h-3.5 text-blue-500" />
            <span>Sử dụng App Android (STUDENT)</span>
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Giảng viên</span>
            <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <BookOpen className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-800 mt-3">{stats.totalTeachers}</p>
          <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
            <Smartphone className="w-3.5 h-3.5 text-purple-500" />
            <span>Sử dụng App Android (TEACHER)</span>
          </p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Tài khoản Hoạt động</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <UserCheck className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-800 mt-3">{stats.activeUsers}</p>
          <p className="text-xs text-emerald-600 font-medium mt-1">Trạng thái ACTIVE hợp lệ</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Tài khoản Bị khóa</span>
            <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
              <UserX className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-800 mt-3">{stats.lockedUsers}</p>
          <p className="text-xs text-rose-600 font-medium mt-1">Trạng thái LOCKED (chặn login)</p>
        </div>
      </div>

      {/* Architecture Highlights & Recent Users */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Architecture Summary Box */}
        <div className="lg:col-span-1 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <Database className="w-5 h-5 text-brand-600" />
              <h3 className="font-bold text-slate-800 text-sm">Mô hình Dữ liệu chung</h3>
            </div>
            <span className="text-[11px] font-semibold text-brand-600 bg-brand-50 px-2 py-0.5 rounded-full">
              PostgreSQL 16
            </span>
          </div>

          <div className="space-y-3 text-xs text-slate-600">
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
              <p className="font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                <Globe className="w-4 h-4 text-brand-500" />
                1. Web Admin (Trình duyệt)
              </p>
              <p className="text-slate-500">
                Giao tiếp qua HTTP REST API với quyền <span className="font-mono text-brand-600">ROLE_ADMIN</span> để quản trị nhân sự, khóa tài khoản, cấp pass.
              </p>
            </div>

            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
              <p className="font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                <Smartphone className="w-4 h-4 text-purple-500" />
                2. Android App (Giáo viên & Học sinh)
              </p>
              <p className="text-slate-500">
                Giao tiếp qua Retrofit tới cùng backend. Backend kiểm tra JWT để trả dữ liệu theo đúng vai trò (<span className="font-mono text-purple-600">TEACHER</span> hoặc <span className="font-mono text-blue-600">STUDENT</span>).
              </p>
            </div>

            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
              <p className="font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                <Layers className="w-4 h-4 text-emerald-500" />
                3. Spring Boot Backend
              </p>
              <p className="text-slate-500">
                Nắm giữ kết nối JDBC tới PostgreSQL, kiểm tra tính toàn vẹn (unique email, mã hóa BCrypt, kiểm tra cờ bắt buộc đổi mật khẩu).
              </p>
            </div>
          </div>

          <button
            onClick={() => onNavigate('architecture')}
            className="w-full mt-2 py-2 px-3 text-center text-xs font-bold text-brand-600 hover:text-brand-700 bg-brand-50 hover:bg-brand-100 rounded-xl transition-colors"
          >
            Đọc tài liệu kiến trúc chi tiết →
          </button>
        </div>

        {/* Recent Registered Users */}
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-4">
              <div>
                <h3 className="font-bold text-slate-800 text-sm">Danh sách Tài khoản gần đây</h3>
                <p className="text-xs text-slate-500">Các thành viên vừa được cấp tài khoản trên hệ thống</p>
              </div>
              <button
                onClick={fetchDashboardData}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                title="Làm mới"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
            </div>

            {loading ? (
              <div className="py-12 text-center text-sm text-slate-400">Đang tải danh sách...</div>
            ) : recentUsers.length === 0 ? (
              <div className="py-12 text-center text-sm text-slate-400">
                Chưa có học viên hoặc giảng viên nào. Hãy bấm "Cấp tài khoản" để bắt đầu!
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {recentUsers.map((u) => (
                  <div key={u.id} className="py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs ${
                        u.role === 'TEACHER' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                      }`}>
                        {u.fullName?.charAt(0) || 'U'}
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-slate-800">{u.fullName}</p>
                        <p className="text-xs text-slate-400">{u.email} {u.phone ? `• ${u.phone}` : ''}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                        u.role === 'TEACHER'
                          ? 'bg-purple-50 text-purple-700 border border-purple-200'
                          : 'bg-blue-50 text-blue-700 border border-blue-200'
                      }`}>
                        {u.role === 'TEACHER' ? 'Giảng viên' : 'Học viên'}
                      </span>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                      }`}>
                        {u.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
            <button
              onClick={() => onNavigate('teachers')}
              className="text-xs font-bold text-purple-600 hover:text-purple-700 hover:underline"
            >
              Xem tất cả Giảng viên →
            </button>
            <button
              onClick={() => onNavigate('students')}
              className="text-xs font-bold text-brand-600 hover:text-brand-700 hover:underline"
            >
              Xem tất cả Học viên →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
