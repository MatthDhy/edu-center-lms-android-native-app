import React, { useState, useEffect } from 'react';
import {
  GraduationCap, BookOpen, School, Lock, PlusCircle, RefreshCw,
  KeyRound, CalendarDays, ChevronRight, Wallet, AlertTriangle, Inbox,
} from 'lucide-react';
import { adminApi, courseApi, financeApi, leaveApi } from '../api/client';
import StatCard from '../components/StatCard';
import BarChart from '../components/BarChart';
import { cardCls, btnPrimary } from '../components/ui';
import { fmtMoney } from '../utils/format';

const pct = (n, t) => (t > 0 ? Math.round((n * 100) / t) : 0);

function Bar({ label, value, total, color }) {
  const p = pct(value, total);
  return (
    <div>
      <div className="flex items-baseline justify-between mb-1.5">
        <span className="text-sm font-semibold text-slate-600">{label}</span>
        <span className="text-sm"><b className="text-ink text-base">{value}</b><span className="text-slate-400"> · {p}%</span></span>
      </div>
      <div className="h-2.5 rounded-full bg-slate-100 overflow-hidden"><div className={`h-full rounded-full ${color}`} style={{ width: `${p}%` }} /></div>
    </div>
  );
}

function TodoRow({ icon: Icon, label, count, onClick }) {
  const has = count > 0;
  return (
    <button onClick={onClick} className="w-full flex items-center gap-3 py-3 px-2 -mx-2 rounded-xl hover:bg-brand-50/60 transition-colors text-left">
      <span className={`w-9 h-9 rounded-xl flex items-center justify-center ${has ? 'bg-brand-50 text-brand-600' : 'bg-slate-50 text-slate-400'}`}><Icon className="w-[18px] h-[18px]" /></span>
      <span className="flex-1 text-sm font-medium text-slate-700">{label}</span>
      <span className={`text-xl font-extrabold tabular-nums ${has ? 'text-brand-600' : 'text-slate-300'}`}>{String(count).padStart(2, '0')}</span>
      <ChevronRight className="w-4 h-4 text-slate-300" />
    </button>
  );
}

const initials = (name = '') => name.split(/\s+/).map((w) => w[0]).slice(-2).join('').toUpperCase();

export default function DashboardPage({ onNavigate, onOpenCreateModal }) {
  const [stats, setStats] = useState({});
  const [users, setUsers] = useState([]);
  const [courses, setCourses] = useState([]);
  const [fin, setFin] = useState(null);
  const [pendingLeave, setPendingLeave] = useState(0);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const [s, u, c, f, l] = await Promise.all([
        adminApi.getStats(),
        adminApi.getUsers({}),
        courseApi.list({}).catch(() => ({ data: [] })),
        financeApi.overview().catch(() => ({ data: null })),
        leaveApi.list('PENDING').catch(() => ({ data: [] })),
      ]);
      setStats(s.data || {}); setUsers(u.data || []); setCourses(c.data || []);
      setFin(f.data); setPendingLeave((l.data || []).length);
    } catch (e) { console.error(e); } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const total = stats.totalUsers ?? 0;
  const openCourses = courses.filter((c) => c.status === 'ACTIVE');
  const pendingPw = users.filter((u) => u.mustChangePassword).length;
  const noLessons = openCourses.filter((c) => c.lessonCount === 0).length;
  const noStudents = openCourses.filter((c) => c.studentCount === 0).length;
  const overdueCount = fin?.overdueCount ?? 0;
  const todoTotal = pendingPw + noLessons + noStudents + (stats.lockedUsers ?? 0) + pendingLeave + overdueCount;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-3">
        <button onClick={() => onOpenCreateModal('STUDENT')} className={btnPrimary}><PlusCircle className="w-4 h-4" /> Thêm học viên</button>
        <button onClick={() => onOpenCreateModal('TEACHER')} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-brand-200 bg-white text-brand-700 hover:bg-brand-50 font-semibold text-sm"><PlusCircle className="w-4 h-4" /> Thêm giảng viên</button>
        <button onClick={() => onNavigate('courses')} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 font-semibold text-sm"><School className="w-4 h-4" /> Tạo lớp học</button>
        <button onClick={load} className="ml-auto p-2.5 rounded-xl border border-slate-200 bg-white text-slate-500 hover:bg-slate-50" title="Làm mới"><RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /></button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-5">
        <StatCard label="Tổng học viên" value={stats.totalStudents} hint={`${total} tài khoản`} icon={GraduationCap} tone="brand" />
        <StatCard label="Tổng giảng viên" value={stats.totalTeachers} icon={BookOpen} tone="blue" />
        <StatCard label="Lớp đang mở" value={openCourses.length} hint={`/${courses.length} lớp`} icon={School} tone="emerald" />
        <StatCard label="Doanh thu tháng" value={fin ? fmtMoney(fin.revenueMonth).replace(' đ', '') : '—'} hint="đ" icon={Wallet} tone="amber" />
        <StatCard label="Công nợ" value={fin ? fmtMoney(fin.outstandingTotal).replace(' đ', '') : '—'} hint={fin ? `${fin.outstandingCount} khoản` : ''} icon={AlertTriangle} tone="rose" />
      </div>

      {fin && (
        <div className={`${cardCls} p-6`}>
          <div className="flex items-baseline justify-between mb-4">
            <div><h3 className="text-lg font-bold text-ink">Doanh thu học phí 12 tháng</h3>
              <p className="text-xs text-slate-500 mt-0.5">VietQR {fmtMoney(fin.revenueMonthVietQr)} · Tiền mặt {fmtMoney(fin.revenueMonthCash)} (tháng {fin.month})</p></div>
            <button onClick={() => onNavigate('finance')} className="text-xs font-semibold text-brand-600 hover:underline">Xem học phí</button>
          </div>
          <BarChart height={170} highlight={fin.month} data={fin.monthly.map((m) => ({ label: m.month.slice(5) + '/' + m.month.slice(2, 4), value: m.amount, _m: m.month }))} />
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-5">
        <div className={`${cardCls} p-6 xl:col-span-3`}>
          <h3 className="text-lg font-bold text-ink">Cơ cấu tài khoản</h3>
          <div className="flex items-baseline gap-2 mt-3 mb-5"><span className="text-5xl font-extrabold tracking-tight text-ink tabular-nums">{total}</span><span className="text-sm text-slate-400">tài khoản trong trung tâm</span></div>
          <div className="space-y-5">
            <Bar label="Học viên" value={stats.totalStudents ?? 0} total={total} color="bg-brand-500" />
            <Bar label="Giảng viên" value={stats.totalTeachers ?? 0} total={total} color="bg-blue-500" />
            <Bar label="Đang hoạt động" value={stats.activeUsers ?? 0} total={total} color="bg-emerald-500" />
            <Bar label="Đã khóa" value={stats.lockedUsers ?? 0} total={total} color="bg-rose-500" />
          </div>
        </div>
        <div className={`${cardCls} p-6 xl:col-span-2`}>
          <div className="flex items-baseline justify-between"><h3 className="text-lg font-bold text-ink">Cần xử lý</h3><span className="text-xs text-slate-400">{todoTotal} yêu cầu</span></div>
          <div className="mt-2 divide-y divide-slate-100">
            <TodoRow icon={Wallet} label="Khoản học phí quá hạn" count={overdueCount} onClick={() => onNavigate('finance')} />
            <TodoRow icon={Inbox} label="Đơn xin nghỉ chờ duyệt" count={pendingLeave} onClick={() => onNavigate('leave')} />
            <TodoRow icon={KeyRound} label="Chưa đổi mật khẩu lần đầu" count={pendingPw} onClick={() => onNavigate('students')} />
            <TodoRow icon={Lock} label="Tài khoản đang bị khóa" count={stats.lockedUsers ?? 0} onClick={() => onNavigate('students')} />
            <TodoRow icon={CalendarDays} label="Lớp chưa có lịch học" count={noLessons} onClick={() => onNavigate('courses')} />
            <TodoRow icon={GraduationCap} label="Lớp chưa có học viên" count={noStudents} onClick={() => onNavigate('courses')} />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <div className={`${cardCls} p-6`}>
          <h3 className="text-lg font-bold text-ink mb-2">Tài khoản mới tạo</h3>
          {users.length === 0 ? <p className="py-10 text-center text-sm text-slate-400">{loading ? 'Đang tải...' : 'Chưa có tài khoản nào.'}</p> : (
            <div className="divide-y divide-slate-100">{users.slice(0, 6).map((u) => (
              <div key={u.id} className="py-3 flex items-center gap-3">
                <span className="w-10 h-10 rounded-full bg-brand-100 text-brand-700 font-bold text-sm flex items-center justify-center">{initials(u.fullName)}</span>
                <div className="min-w-0 flex-1"><p className="text-sm font-bold text-ink truncate">{u.fullName}</p><p className="text-xs text-slate-400 truncate">{u.email}</p></div>
                <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${u.role === 'TEACHER' ? 'bg-blue-50 text-blue-700' : u.role === 'ADMIN' ? 'bg-slate-100 text-slate-600' : 'bg-brand-50 text-brand-700'}`}>{u.role === 'TEACHER' ? 'Giảng viên' : u.role === 'ADMIN' ? 'Quản trị' : 'Học viên'}</span>
              </div>))}</div>
          )}
        </div>
        <div className={`${cardCls} p-6`}>
          <div className="flex items-baseline justify-between mb-2"><h3 className="text-lg font-bold text-ink">Lớp học gần đây</h3><button onClick={() => onNavigate('courses')} className="text-xs font-semibold text-brand-600 hover:underline">Xem tất cả</button></div>
          {courses.length === 0 ? <p className="py-10 text-center text-sm text-slate-400">{loading ? 'Đang tải...' : 'Chưa có lớp học nào.'}</p> : (
            <div className="divide-y divide-slate-100">{courses.slice(0, 6).map((c) => (
              <div key={c.id} className="py-3 flex items-center gap-3">
                <div className="min-w-0 flex-1"><p className="text-sm font-bold text-ink truncate">{c.courseName}</p><p className="text-xs text-slate-400 truncate">{c.teacherName} · {c.heldCount}/{c.totalSessions || c.lessonCount || '?'} buổi</p></div>
                <div className="text-right"><p className="text-2xl font-extrabold text-brand-600 leading-none tabular-nums">{c.studentCount}</p><p className="text-[11px] text-slate-400 mt-1">học viên</p></div>
              </div>))}</div>
          )}
        </div>
      </div>
    </div>
  );
}
