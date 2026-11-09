import React, { useState, useEffect } from 'react';
import { ClipboardList, Eye, Paperclip, ChevronDown, FileCheck2, Clock, CheckCircle2, Zap } from 'lucide-react';
import { courseApi, quizApi } from '../api/client';
import SubmissionsModal from '../components/SubmissionsModal';
import QuizResultsModal from '../components/QuizResultsModal';
import StatCard from '../components/StatCard';
import { fmtDateTime } from '../utils/format';
import { inputCls, cardCls } from '../components/ui';

export default function AssignmentsPage() {
  const [kind, setKind] = useState('essay');
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [items, setItems] = useState([]);
  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(null);
  const [openQuiz, setOpenQuiz] = useState(null);

  useEffect(() => {
    courseApi.list({}).then((res) => {
      const list = res.data || [];
      setCourses(list);
      if (list.length > 0) setCourseId(String(list[0].id));
    }).catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    if (!courseId) { setItems([]); setQuizzes([]); return; }
    setLoading(true); setError('');
    const req = kind === 'essay' ? courseApi.assignments(courseId).then((r) => setItems(r.data || [])) : quizApi.byCourse(courseId).then((r) => setQuizzes(r.data || []));
    req.catch((err) => setError(err.message)).finally(() => setLoading(false));
  }, [courseId, kind]);

  const submitted = items.reduce((s, a) => s + (a.submittedCount || 0), 0);
  const graded = items.reduce((s, a) => s + (a.gradedCount || 0), 0);
  const gradable = items.filter((a) => a.graded !== false).reduce((s, a) => s + (a.submittedCount || 0), 0);

  return (
    <div className="space-y-6">
      <div className="flex gap-2">
        {[['essay', 'Bài tập tự luận', ClipboardList], ['quiz', 'Quiz trắc nghiệm', Zap]].map(([id, label, Icon]) => (
          <button key={id} onClick={() => setKind(id)} className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold border ${kind === id ? 'bg-brand-500 border-brand-500 text-white shadow-md shadow-brand-500/25' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}><Icon className="w-4 h-4" />{label}</button>
        ))}
      </div>

      {kind === 'essay' && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <StatCard label="Bài tập đã giao" value={items.length} icon={ClipboardList} tone="brand" />
          <StatCard label="Lượt nộp bài" value={submitted} icon={FileCheck2} tone="blue" />
          <StatCard label="Đã chấm điểm" value={graded} hint={`còn ${Math.max(0, gradable - graded)} chờ chấm`} icon={CheckCircle2} tone="emerald" />
        </div>
      )}

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 flex flex-col sm:flex-row sm:items-center gap-3 border-b border-slate-100">
          <div className="sm:mr-auto">
            <h3 className="text-lg font-bold text-ink">{kind === 'essay' ? 'Tiến độ nộp bài' : 'Danh sách quiz'}</h3>
            <p className="text-xs text-slate-500 mt-0.5">Giảng viên giao, chấm và tạo quiz trên app Android; Admin chỉ theo dõi.</p>
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

        {loading ? <div className="py-16 text-center text-sm text-slate-400">Đang tải...</div> : kind === 'essay' ? (
          items.length === 0 ? <div className="py-20 text-center text-sm text-slate-400"><ClipboardList className="w-10 h-10 mx-auto mb-3 text-slate-300" /><p>Lớp này chưa có bài tập nào.</p></div> : (
            <div className="overflow-x-auto"><table className="w-full text-sm">
              <thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Bài tập</th><th className="px-5 py-3.5">Hạn nộp</th><th className="px-5 py-3.5">Tiến độ nộp</th><th className="px-5 py-3.5 text-center">Chấm điểm</th><th className="px-5 py-3.5 text-center">Đã chấm</th><th className="px-5 py-3.5 text-right">Chi tiết</th></tr></thead>
              <tbody className="divide-y divide-slate-100">{items.map((a) => {
                const total = a.totalStudents || 0;
                const pct = total > 0 ? Math.round((a.submittedCount * 100) / total) : 0;
                const overdue = a.minutesLeft != null && a.minutesLeft < 0;
                return (
                  <tr key={a.id}>
                    <td className="px-5 py-4"><p className="font-bold text-ink">{a.title}</p>{a.attachmentUrl && <a href={a.attachmentUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-xs text-brand-600 hover:underline mt-0.5"><Paperclip className="w-3 h-3" /> Đề bài đính kèm</a>}</td>
                    <td className="px-5 py-4"><span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold ${overdue ? 'bg-rose-50 text-rose-700' : 'bg-slate-100 text-slate-700'}`}><Clock className="w-3 h-3" />{fmtDateTime(a.deadline)}{overdue && ' · quá hạn'}</span></td>
                    <td className="px-5 py-4 min-w-[170px]"><div className="flex items-baseline justify-between mb-1"><span className="text-sm font-bold text-ink">{a.submittedCount}<span className="text-slate-400 font-medium">/{total}</span></span><span className="text-xs font-semibold text-emerald-600">{pct}%</span></div><div className="h-2 rounded-full bg-slate-100 overflow-hidden"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${pct}%` }} /></div></td>
                    <td className="px-5 py-4 text-center">{a.graded === false ? <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-500">Không chấm</span> : <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-brand-50 text-brand-700">Thang {a.maxScore}</span>}</td>
                    <td className="px-5 py-4 text-center text-lg font-extrabold text-brand-600 tabular-nums">{a.graded === false ? '—' : a.gradedCount}</td>
                    <td className="px-5 py-4 text-right"><button onClick={() => setOpen(a.id)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100" title="Xem bài nộp"><Eye className="w-4 h-4" /></button></td>
                  </tr>);
              })}</tbody></table></div>
          )
        ) : (
          quizzes.length === 0 ? <div className="py-20 text-center text-sm text-slate-400"><Zap className="w-10 h-10 mx-auto mb-3 text-slate-300" /><p>Lớp này chưa có quiz nào.</p></div> : (
            <div className="overflow-x-auto"><table className="w-full text-sm">
              <thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Quiz</th><th className="px-5 py-3.5 text-center">Số câu</th><th className="px-5 py-3.5 text-center">Thời gian</th><th className="px-5 py-3.5">Mở – đóng</th><th className="px-5 py-3.5">Trạng thái</th><th className="px-5 py-3.5 text-right">Kết quả</th></tr></thead>
              <tbody className="divide-y divide-slate-100">{quizzes.map((q) => (
                <tr key={q.id}>
                  <td className="px-5 py-4 font-bold text-ink">{q.title}</td>
                  <td className="px-5 py-4 text-center tabular-nums font-semibold">{q.questionCount}</td>
                  <td className="px-5 py-4 text-center tabular-nums">{q.durationMinutes}'</td>
                  <td className="px-5 py-4 text-xs text-slate-600">{q.startTime ? fmtDateTime(q.startTime) : 'Ngay khi xuất bản'}<br />→ {fmtDateTime(q.endTime)}</td>
                  <td className="px-5 py-4"><span className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${q.published ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{q.published ? 'Đã xuất bản' : 'Bản nháp'}</span></td>
                  <td className="px-5 py-4 text-right"><button disabled={!q.published} onClick={() => setOpenQuiz(q.id)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100 disabled:opacity-30" title="Xem kết quả"><Eye className="w-4 h-4" /></button></td>
                </tr>))}</tbody></table></div>
          )
        )}
      </div>
      <SubmissionsModal isOpen={!!open} assignmentId={open} onClose={() => setOpen(null)} />
      <QuizResultsModal isOpen={!!openQuiz} quizId={openQuiz} onClose={() => setOpenQuiz(null)} />
    </div>
  );
}
