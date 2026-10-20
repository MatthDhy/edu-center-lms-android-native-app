import React from 'react';
import { LayoutDashboard, GraduationCap, Users, Database, LogOut, BookOpen } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar({ activeTab, setActiveTab }) {
  const { logout, user } = useAuth();

  const navItems = [
    { id: 'dashboard', label: 'Tổng quan & Thống kê', icon: LayoutDashboard },
    { id: 'teachers', label: 'Quản lý Giảng viên', icon: BookOpen },
    { id: 'students', label: 'Quản lý Học viên', icon: GraduationCap },
    { id: 'architecture', label: 'Cơ chế Nối DB & Role', icon: Database },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-200 flex flex-col shrink-0 min-h-screen">
      {/* Brand Header */}
      <div className="p-6 border-b border-slate-800 flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-brand-600 flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-brand-500/30">
          C
        </div>
        <div>
          <h1 className="font-bold text-lg text-white leading-tight">CeLeHub</h1>
          <p className="text-xs text-brand-400 font-medium tracking-wide uppercase">Admin Portal</p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="p-4 space-y-1.5 flex-1">
        <div className="text-xs font-semibold text-slate-500 uppercase px-3 mb-2 tracking-wider">
          Phân hệ Quản trị
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 px-3.5 py-3 rounded-xl text-sm font-medium transition-all ${
                isActive
                  ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30 font-semibold'
                  : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60'
              }`}
            >
              <Icon className={`w-5 h-5 ${isActive ? 'text-white' : 'text-slate-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* User profile & Logout */}
      <div className="p-4 border-t border-slate-800 bg-slate-950/40">
        <div className="flex items-center gap-3 mb-3 px-2">
          <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-sm text-brand-400">
            AD
          </div>
          <div className="truncate flex-1">
            <p className="text-xs font-semibold text-white truncate">{user?.fullName || 'Administrator'}</p>
            <p className="text-[11px] text-slate-400 truncate">{user?.email || 'admin@celehub.edu.vn'}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-lg transition-colors border border-rose-500/20"
        >
          <LogOut className="w-4 h-4" />
          <span>Đăng xuất</span>
        </button>
      </div>
    </aside>
  );
}
