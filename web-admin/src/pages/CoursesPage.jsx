import React, { useState, useEffect, useCallback } from 'react';
import { Search, PlusCircle, RefreshCw, Pencil, Settings2, School, Filter, ChevronDown, Users, CalendarDays, Lock } from 'lucide-react';
import { adminApi, courseApi } from '../api/client';
import CourseFormModal from '../components/CourseFormModal';
import CourseDetailPage from './CourseDetailPage';
import StatCard from '../components/StatCard';
import { fmtMoney } from '../utils/format';
import { btnPrimary, inputCls, cardCls } from '../components/ui';

function StatusBadge({ status }) {
  return status === 'ACTIVE'
    ? <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700">Đang mở</span>
    : <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-600">Đã đóng</span>;
}

export default function CoursesPage() {
  const [teachers, setTeachers] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    adminApi.getUsers({ role: 'TEACHER', status: 'ACTIVE' }).then((r) => setTeachers(r.data || [])).catch(() => {});
  }, []);

  const fetchCourses = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await courseApi.list({ status: status || undefined, teacherId: teacherId || undefined, search: search.trim() || undefined });
      setCourses(res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [status, teacherId, search]);

  useEffect(() => {
    const t = setTimeout(fetchCourses, 300);
    return () => clearTimeout(t);
  }, [fetchCourses]);

  if (selectedId) {
    return <CourseDetailPage courseId={selectedId} teachers={teachers} onBack={() => { setSelectedId(null); fetchCourses(); }} />;
  }

  const open = courses.filter((c) => c.status === 'ACTIVE');
  const students = open.reduce((s, c) => s + (c.studentCount || 0), 0);
  const lessons = open.reduce((s, c) => s + (c.lessonCount || 0), 0);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <StatCard label="Tổng lớp học" value={courses.length} hint="theo bộ lọc" icon={School} tone="brand" />
        <StatCard label="Đang mở" value={open.length} icon={CalendarDays} tone="emerald" />
        <StatCard label="Học viên đang học" value={students} hint="trong các lớp mở" icon={Users} tone="blue" />
        <StatCard label="Buổi đã xếp lịch" value={lessons} icon={CalendarDays} tone="amber" />
      </div>

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 flex flex-col xl:flex-row xl:items-center gap-3 border-b border-slate-100">
          <div className="xl:mr-auto">
            <h3 className="text-lg font-bold text-ink">Danh sách lớp học</h3>
            <p className="text-xs text-slate-500 mt-0.5">Mỗi lớp có 1 giảng viên phụ trách, lịch học và danh sách học viên riêng.</p>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input className={`${inputCls} pl-9 xl:w-64`} placeholder="Tìm theo tên lớp..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
            <select className={`${inputCls} appearance-none pl-9 pr-8`} value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">Mọi trạng thái</option>
              <option value="ACTIVE">Đang mở</option>
              <option value="CLOSED">Đã đóng</option>
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          </div>
          <div className="relative">
            <select className={`${inputCls} appearance-none pr-8`} value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
              <option value="">Mọi giảng viên</option>
              {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          </div>
          <button onClick={fetchCourses} disabled={loading} className="p-2.5 text-slate-500 hover:bg-slate-100 rounded-xl border border-slate-200" title="Làm mới">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button onClick={() => { setEditing(null); setFormOpen(true); }} className={btnPrimary}><PlusCircle className="w-4 h-4" /> Tạo lớp học</button>
        </div>

        {error && <div className="m-5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

        {courses.length === 0 && !loading ? (
          <div className="py-20 text-center text-sm text-slate-400">
            <School className="w-10 h-10 mx-auto mb-3 text-slate-300" />
            <p>Chưa có lớp học nào. Bấm “Tạo lớp học” để bắt đầu.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b border-slate-100">
                  <th className="px-5 py-3.5">Lớp học</th>
                  <th className="px-5 py-3.5">Giảng viên</th>
                  <th className="px-5 py-3.5">Học phí</th>
                  <th className="px-5 py-3.5 text-center">Sĩ số</th>
                  <th className="px-5 py-3.5">Tiến độ</th>
                  <th className="px-5 py-3.5">Trạng thái</th>
                  <th className="px-5 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {courses.map((c) => {
                  const total = c.totalSessions || c.lessonCount || 0;
                  const pct = total > 0 ? Math.min(100, Math.round((c.heldCount * 100) / total)) : 0;
                  return (
                    <tr key={c.id}>
                      <td className="px-5 py-4">
                        <button onClick={() => setSelectedId(c.id)} className="font-bold text-brand-700 hover:underline text-left">{c.courseName}</button>
                        <p className="text-xs text-slate-400">Mã lớp #{c.id}</p>
                      </td>
                      <td className="px-5 py-4 font-medium text-slate-700">{c.teacherName}</td>
                      <td className="px-5 py-4 font-bold text-ink">{fmtMoney(c.tuitionFee)}</td>
                      <td className="px-5 py-4 text-center text-xl font-extrabold text-brand-600 tabular-nums">{c.studentCount}</td>
                      <td className="px-5 py-4 min-w-[170px]">
                        <div className="flex items-baseline justify-between mb-1">
                          <span className="text-sm font-bold text-ink">{c.heldCount}<span className="text-slate-400 font-medium">/{total || '?'} buổi</span></span>
                          <span className="text-xs font-semibold text-brand-600">{pct}%</span>
                        </div>
                        <div className="h-2 rounded-full bg-slate-100 overflow-hidden"><div className="h-full rounded-full bg-brand-500" style={{ width: `${pct}%` }} /></div>
                      </td>
                      <td className="px-5 py-4"><StatusBadge status={c.status} /></td>
                      <td className="px-5 py-4">
                        <div className="flex items-center justify-end gap-2">
                          <button onClick={() => setSelectedId(c.id)} title="Quản lý lịch & học viên" className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100"><Settings2 className="w-4 h-4" /></button>
                          <button onClick={() => { setEditing(c); setFormOpen(true); }} title="Sửa lớp" className="p-2 rounded-lg text-amber-600 hover:bg-amber-50 border border-amber-200"><Pencil className="w-4 h-4" /></button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <CourseFormModal isOpen={formOpen} onClose={() => setFormOpen(false)} course={editing} teachers={teachers} onSaved={fetchCourses} />
    </div>
  );
}
