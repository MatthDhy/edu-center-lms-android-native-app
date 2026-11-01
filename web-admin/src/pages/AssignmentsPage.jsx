import React, { useState, useEffect } from 'react';
import { ClipboardList, Eye, Paperclip, ChevronDown, FileCheck2, Clock, CheckCircle2 } from 'lucide-react';
import { courseApi } from '../api/client';
import SubmissionsModal from '../components/SubmissionsModal';
import StatCard from '../components/StatCard';
import { fmtDateTime } from '../utils/format';
import { inputCls, cardCls } from '../components/ui';

export default function AssignmentsPage() {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(null);

  useEffect(() => {
    courseApi.list({}).then((res) => {
      const list = res.data || [];
      setCourses(list);
      if (list.length > 0) setCourseId(String(list[0].id));
    }).catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (!courseId) { setItems([]); return; }
    setLoading(true);
    setError('');
    courseApi.assignments(courseId)
      .then((res) => setItems(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [courseId]);

  const submitted = items.reduce((s, a) => s + (a.submittedCount || 0), 0);
  const graded = items.reduce((s, a) => s + (a.gradedCount || 0), 0);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <StatCard label="Bài tập đã giao" value={items.length} icon={ClipboardList} tone="brand" />
        <StatCard label="Lượt nộp bài" value={submitted} icon={FileCheck2} tone="blue" />
        <StatCard label="Đã chấm điểm" value={graded} hint={`còn ${submitted - graded} chờ chấm`} icon={CheckCircle2} tone="emerald" />
      </div>

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 flex flex-col sm:flex-row sm:items-center gap-3 border-b border-slate-100">
          <div className="sm:mr-auto">
            <h3 className="text-lg font-bold text-ink">Tiến độ nộp bài</h3>
            <p className="text-xs text-slate-500 mt-0.5">Giảng viên giao và chấm bài trên app Android; Admin chỉ theo dõi.</p>
          </div>
          <div className="relative sm:w-80">
            <select className={`${inputCls} appearance-none pr-8`} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              {courses.length === 0 && <option value="">Chưa có lớp học</option>}
              {courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} — {c.teacherName}</option>)}
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          </div>
        </div>

        {error && <div className="m-5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

        {loading ? (
          <div className="py-16 text-center text-sm text-slate-400">Đang tải...</div>
        ) : items.length === 0 ? (
          <div className="py-20 text-center text-sm text-slate-400">
            <ClipboardList className="w-10 h-10 mx-auto mb-3 text-slate-300" />
            <p>Lớp này chưa có bài tập nào.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b border-slate-100">
                  <th className="px-5 py-3.5">Bài tập</th>
                  <th className="px-5 py-3.5">Hạn nộp</th>
                  <th className="px-5 py-3.5">Tiến độ nộp</th>
                  <th className="px-5 py-3.5 text-center">Đã chấm</th>
                  <th className="px-5 py-3.5 text-center">Điểm tối đa</th>
                  <th className="px-5 py-3.5 text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((a) => {
                  const total = a.totalStudents || 0;
                  const pct = total > 0 ? Math.round((a.submittedCount * 100) / total) : 0;
                  const overdue = a.minutesLeft != null && a.minutesLeft < 0;
                  return (
                    <tr key={a.id}>
                      <td className="px-5 py-4">
                        <p className="font-bold text-ink">{a.title}</p>
                        {a.attachmentUrl && (
                          <a href={a.attachmentUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-xs text-brand-600 hover:underline mt-0.5">
                            <Paperclip className="w-3 h-3" /> Đề bài đính kèm
                          </a>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold ${overdue ? 'bg-rose-50 text-rose-700' : 'bg-slate-100 text-slate-700'}`}>
                          <Clock className="w-3 h-3" />{fmtDateTime(a.deadline)}{overdue && ' · quá hạn'}
                        </span>
                      </td>
                      <td className="px-5 py-4 min-w-[170px]">
                        <div className="flex items-baseline justify-between mb-1">
                          <span className="text-sm font-bold text-ink">{a.submittedCount}<span className="text-slate-400 font-medium">/{total}</span></span>
                          <span className="text-xs font-semibold text-emerald-600">{pct}%</span>
                        </div>
                        <div className="h-2 rounded-full bg-slate-100 overflow-hidden"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${pct}%` }} /></div>
                      </td>
                      <td className="px-5 py-4 text-center text-lg font-extrabold text-brand-600 tabular-nums">{a.gradedCount}</td>
                      <td className="px-5 py-4 text-center font-bold text-ink">{a.maxScore}</td>
                      <td className="px-5 py-4 text-right">
                        <button onClick={() => setOpen(a.id)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100" title="Xem bài nộp"><Eye className="w-4 h-4" /></button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <SubmissionsModal isOpen={!!open} assignmentId={open} onClose={() => setOpen(null)} />
    </div>
  );
}
