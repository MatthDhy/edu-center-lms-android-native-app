import React, { useState, useEffect } from 'react';
import { ClipboardList, Eye, Paperclip, ChevronDown } from 'lucide-react';
import { courseApi } from '../api/client';
import SubmissionsModal from '../components/SubmissionsModal';
import { fmtDateTime } from '../utils/format';
import { inputCls } from '../components/ui';

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

  return (
    <div className="space-y-5">
      <div>
        <h3 className="text-lg font-bold text-slate-800">Theo dõi bài tập</h3>
        <p className="text-xs text-slate-500 mt-0.5">Giảng viên giao và chấm bài trên app Android; Admin chỉ xem tiến độ nộp bài của từng lớp.</p>
      </div>

      <div className="relative max-w-md">
        <select className={`${inputCls} appearance-none pr-8`} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
          {courses.length === 0 && <option value="">Chưa có lớp học</option>}
          {courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} — {c.teacherName}</option>)}
        </select>
        <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
      </div>

      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      {loading ? (
        <div className="py-16 text-center text-sm text-slate-400">Đang tải...</div>
      ) : items.length === 0 ? (
        <div className="py-20 text-center text-sm text-slate-400 bg-white rounded-xl border border-slate-200">
          <ClipboardList className="w-10 h-10 mx-auto mb-3 text-slate-300" />
          <p>Lớp này chưa có bài tập nào.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
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
                  <tr key={a.id} className="hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-semibold text-slate-800">{a.title}</p>
                      {a.attachmentUrl && (
                        <a href={a.attachmentUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-[11px] text-brand-600 hover:underline">
                          <Paperclip className="w-3 h-3" /> Đề bài đính kèm
                        </a>
                      )}
                    </td>
                    <td className={`px-5 py-3.5 text-xs ${overdue ? 'text-rose-600 font-semibold' : 'text-slate-600'}`}>
                      {fmtDateTime(a.deadline)}{overdue && ' (quá hạn)'}
                    </td>
                    <td className="px-5 py-3.5 min-w-[150px]">
                      <div className="h-1.5 rounded-full bg-slate-100 overflow-hidden"><div className="h-full bg-emerald-500" style={{ width: `${pct}%` }} /></div>
                      <p className="text-[11px] text-slate-500 mt-1">{a.submittedCount}/{total} học viên</p>
                    </td>
                    <td className="px-5 py-3.5 text-center">{a.gradedCount}</td>
                    <td className="px-5 py-3.5 text-center">{a.maxScore}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button onClick={() => setOpen(a.id)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100" title="Xem bài nộp">
                        <Eye className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <SubmissionsModal isOpen={!!open} assignmentId={open} onClose={() => setOpen(null)} />
    </div>
  );
}
