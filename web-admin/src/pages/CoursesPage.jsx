import React, { useState, useEffect, useCallback } from 'react';
import { Search, PlusCircle, RefreshCw, Pencil, Settings2, School, Filter, ChevronDown } from 'lucide-react';
import { adminApi, courseApi } from '../api/client';
import CourseFormModal from '../components/CourseFormModal';
import CourseDetailPage from './CourseDetailPage';
import { fmtMoney } from '../utils/format';
import { btnPrimary, inputCls } from '../components/ui';

function StatusBadge({ status }) {
  return status === 'ACTIVE' ? (
    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">ACTIVE</span>
  ) : (
    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200">CLOSED</span>
  );
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
    adminApi
      .getUsers({ role: 'TEACHER', status: 'ACTIVE' })
      .then((res) => setTeachers(res.data || []))
      .catch(() => {});
  }, []);

  const fetchCourses = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await courseApi.list({
        status: status || undefined,
        teacherId: teacherId || undefined,
        search: search.trim() || undefined,
      });
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
    return (
      <CourseDetailPage
        courseId={selectedId}
        teachers={teachers}
        onBack={() => {
          setSelectedId(null);
          fetchCourses();
        }}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h3 className="text-lg font-bold text-slate-800">Danh sách lớp học</h3>
          <p className="text-xs text-slate-500 mt-0.5">Mỗi lớp có 1 giảng viên phụ trách, lịch học và danh sách học viên riêng.</p>
        </div>
        <button
          onClick={() => { setEditing(null); setFormOpen(true); }}
          className={btnPrimary}
        >
          <PlusCircle className="w-4 h-4" /><span>Tạo lớp học</span>
        </button>
      </div>

      <div className="bg-white p-4 rounded-xl border border-slate-200 flex flex-col lg:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input className={`${inputCls} pl-9`} placeholder="Tìm theo tên lớp..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          <select className={`${inputCls} appearance-none pl-9 pr-8`} value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="CLOSED">CLOSED</option>
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
        </div>
        <div className="relative">
          <select className={`${inputCls} appearance-none pr-8`} value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
            <option value="">Tất cả giảng viên</option>
            {teachers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
        </div>
        <button onClick={fetchCourses} disabled={loading} className="p-2.5 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-xl border border-slate-200 transition-colors" title="Làm mới">
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      {courses.length === 0 && !loading ? (
        <div className="py-20 text-center text-sm text-slate-400 bg-white rounded-xl border border-slate-200">
          <School className="w-10 h-10 mx-auto mb-3 text-slate-300" />
          <p>Chưa có lớp học nào. Bấm “Tạo lớp học” để bắt đầu.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                <th className="px-5 py-3.5">Lớp học</th>
                <th className="px-5 py-3.5">Giảng viên</th>
                <th className="px-5 py-3.5">Học phí</th>
                <th className="px-5 py-3.5 text-center">Sĩ số</th>
                <th className="px-5 py-3.5">Tiến độ</th>
                <th className="px-5 py-3.5">Trạng thái</th>
                <th className="px-5 py-3.5 text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {courses.map((c) => {
                const total = c.totalSessions || c.lessonCount || 0;
                const pct = total > 0 ? Math.min(100, Math.round((c.heldCount * 100) / total)) : 0;
                return (
                  <tr key={c.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-5 py-3.5">
                      <button onClick={() => setSelectedId(c.id)} className="font-semibold text-brand-700 hover:underline text-left">{c.courseName}</button>
                      <p className="text-[11px] text-slate-400">ID: #{c.id}</p>
                    </td>
                    <td className="px-5 py-3.5 text-slate-700">{c.teacherName}</td>
                    <td className="px-5 py-3.5 text-slate-700">{fmtMoney(c.tuitionFee)}</td>
                    <td className="px-5 py-3.5 text-center font-semibold text-slate-700">{c.studentCount}</td>
                    <td className="px-5 py-3.5 min-w-[150px]">
                      <div className="h-1.5 rounded-full bg-slate-100 overflow-hidden">
                        <div className="h-full bg-brand-500" style={{ width: `${pct}%` }} />
                      </div>
                      <p className="text-[11px] text-slate-500 mt-1">{c.heldCount}/{total || '?'} buổi đã học · {c.lessonCount} đã xếp</p>
                    </td>
                    <td className="px-5 py-3.5"><StatusBadge status={c.status} /></td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-2">
                        <button onClick={() => setSelectedId(c.id)} title="Quản lý lịch & học viên" className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100 transition-colors">
                          <Settings2 className="w-4 h-4" />
                        </button>
                        <button onClick={() => { setEditing(c); setFormOpen(true); }} title="Sửa lớp" className="p-2 rounded-lg text-amber-600 hover:bg-amber-50 border border-amber-200 transition-colors">
                          <Pencil className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <CourseFormModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        course={editing}
        teachers={teachers}
        onSaved={fetchCourses}
      />
    </div>
  );
}
