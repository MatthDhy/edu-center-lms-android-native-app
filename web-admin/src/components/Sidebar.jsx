import React from 'react';
import {
  LayoutDashboard, GraduationCap, LogOut, BookOpen,
  School, CalendarDays, ClipboardList, Megaphone,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const GROUPS = [
  { label: 'Tổng quan', items: [{ id: 'dashboard', label: 'Tổng quan', icon: LayoutDashboard }] },
  {
    label: 'Tài khoản',
    items: [
      { id: 'students', label: 'Quản lý học viên', icon: GraduationCap },
      { id: 'teachers', label: 'Quản lý giảng viên', icon: BookOpen },
    ],
  },
  {
    label: 'Đào tạo',
    items: [
      { id: 'courses', label: 'Quản lý lớp học', icon: School },
      { id: 'timetable', label: 'Thời khóa biểu', icon: CalendarDays },
      { id: 'assignments', label: 'Theo dõi bài tập', icon: ClipboardList },
      { id: 'notifications', label: 'Gửi thông báo', icon: Megaphone },
    ],
  },
];

export default function Sidebar({ activeTab, setActiveTab }) {
  const { logout, user } = useAuth();
  const initials = (user?.fullName || 'Quản trị')
    .split(/\s+/).map((w) => w[0]).slice(-2).join('').toUpperCase();

  return (
    <aside className="w-64 bg-white border-r border-slate-200 flex flex-col shrink-0 sticky top-0 h-screen">
      <div className="px-6 pt-7 pb-4 flex items-center gap-3">
        <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-brand-400 to-brand-700 flex items-center justify-center shadow-lg shadow-brand-500/30">
          <GraduationCap className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-xl font-extrabold leading-none tracking-tight">
            <span className="text-blue-700">CeLe</span><span className="text-brand-500">Hub</span>
          </h1>
          <p className="text-[11px] text-slate-400 mt-1">Learn. Connect. Grow.</p>
        </div>
      </div>

      <nav className="px-4 flex-1 overflow-y-auto space-y-5 pb-4">
        {GROUPS.map((g) => (
          <div key={g.label}>
            <p className="px-3 mb-1.5 text-[11px] font-semibold text-slate-400">{g.label}</p>
            <div className="space-y-1">
              {g.items.map(({ id, label, icon: Icon }) => {
                const on = activeTab === id;
                return (
                  <button
                    key={id}
                    onClick={() => setActiveTab(id)}
                    className={`relative w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm transition-all ${
                      on ? 'bg-brand-50 text-brand-700 font-bold' : 'text-slate-600 font-medium hover:bg-slate-50 hover:text-ink'
                    }`}
                  >
                    {on && <span className="absolute left-0 top-2 bottom-2 w-1 rounded-r bg-brand-500" />}
                    <Icon className={`w-[18px] h-[18px] ${on ? 'text-brand-600' : 'text-slate-400'}`} />
                    <span>{label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-4 space-y-3">
        <div className="rounded-2xl bg-brand-50 border border-brand-100 p-4">
          <p className="text-sm font-bold text-ink">Cùng kết nối tri thức</p>
          <p className="text-xs text-slate-500 mt-1 leading-relaxed">Quản lý lớp học trung tâm trong một nơi duy nhất.</p>
        </div>
        <div className="flex items-center gap-3 px-1">
          <div className="w-10 h-10 rounded-full bg-brand-100 text-brand-700 font-bold text-sm flex items-center justify-center">{initials}</div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold text-ink truncate">{user?.fullName || 'Quản trị viên'}</p>
            <p className="text-[11px] text-slate-400 truncate">{user?.email || 'Quản trị hệ thống'}</p>
          </div>
          <button onClick={logout} title="Đăng xuất" className="p-2 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors">
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
