import React, { useState, useEffect } from 'react';
import { GraduationCap, BookOpen, UserCheck, UserX, PlusCircle, RefreshCw } from 'lucide-react';
import { adminApi } from '../api/client';

const CARDS = [
  { key: 'totalStudents', label: 'Học viên', icon: GraduationCap, cls: 'bg-blue-50 text-blue-600' },
  { key: 'totalTeachers', label: 'Giảng viên', icon: BookOpen, cls: 'bg-purple-50 text-purple-600' },
  { key: 'activeUsers', label: 'Đang hoạt động', icon: UserCheck, cls: 'bg-emerald-50 text-emerald-600' },
  { key: 'lockedUsers', label: 'Đã khóa', icon: UserX, cls: 'bg-rose-50 text-rose-600' },
];

export default function DashboardPage({ onNavigate, onOpenCreateModal }) {
  const [stats, setStats] = useState({});
  const [recent, setRecent] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const [s, u] = await Promise.all([adminApi.getStats(), adminApi.getUsers({})]);
      setStats(s.data || {});
      setRecent((u.data || []).slice(0, 6));
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {CARDS.map(({ key, label, icon: Icon, cls }) => (
          <div key={key} className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">{label}</span>
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${cls}`}><Icon className="w-5 h-5" /></div>
            </div>
            <p className="text-3xl font-black text-slate-800 mt-3">{stats[key] ?? 0}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap gap-3">
        <button onClick={() => onOpenCreateModal('TEACHER')} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold text-sm">
          <PlusCircle className="w-4 h-4" /> Cấp tài khoản giảng viên
        </button>
        <button onClick={() => onOpenCreateModal('STUDENT')} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-brand-200 text-brand-700 hover:bg-brand-50 font-semibold text-sm">
          <PlusCircle className="w-4 h-4" /> Cấp tài khoản học viên
        </button>
        <button onClick={() => onNavigate('courses')} className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100 font-semibold text-sm">Tạo lớp học</button>
      </div>

      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-2">
          <h3 className="font-bold text-slate-800 text-sm">Tài khoản mới tạo</h3>
          <button onClick={load} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100" title="Làm mới">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
        {recent.length === 0 ? (
          <p className="py-10 text-center text-sm text-slate-400">{loading ? 'Đang tải...' : 'Chưa có tài khoản nào.'}</p>
        ) : (
          <div className="divide-y divide-slate-100">
            {recent.map((u) => (
              <div key={u.id} className="py-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-slate-800">{u.fullName}</p>
                  <p className="text-xs text-slate-400">{u.email}</p>
                </div>
                <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                  u.role === 'TEACHER' ? 'bg-purple-50 text-purple-700 border-purple-200' : 'bg-blue-50 text-blue-700 border-blue-200'}`}>
                  {u.role === 'TEACHER' ? 'Giảng viên' : 'Học viên'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}