import React, { useState, useEffect } from 'react';
import { Zap, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import BarChart from './BarChart';
import { quizApi } from '../api/client';
import { fmtDateTime } from '../utils/format';

const STATUS = {
  SUBMITTED: ['Đã nộp', 'bg-emerald-50 text-emerald-700'],
  IN_PROGRESS: ['Đang làm', 'bg-amber-50 text-amber-700'],
  NOT_STARTED: ['Chưa làm', 'bg-slate-100 text-slate-600'],
};

/** Admin xem (chỉ đọc) kết quả quiz: phổ điểm, từng học viên, tỷ lệ đúng từng câu. */
export default function QuizResultsModal({ isOpen, quizId, onClose }) {
  const [data, setData] = useState(null);
  const [tab, setTab] = useState('students');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !quizId) return;
    setData(null); setTab('students'); setError('');
    quizApi.results(quizId).then((r) => setData(r.data)).catch((e) => setError(e.message));
  }, [isOpen, quizId]);

  return (
    <Modal isOpen={isOpen} title={data ? `Kết quả: ${data.quiz.title}` : 'Kết quả quiz'} icon={Zap} onClose={onClose} maxWidth="max-w-4xl">
      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700"><AlertCircle className="w-4 h-4" />{error}</div>}
      {!data && !error && <div className="py-10 text-center text-sm text-slate-400">Đang tải...</div>}
      {data && (
        <div className="space-y-5">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-center">
            {[['Đã nộp', `${data.submittedCount}/${data.totalStudents}`], ['Đang làm', data.inProgressCount], ['Điểm TB', data.avgScore ?? '—'], ['Cao / thấp', `${data.maxScore ?? '—'} / ${data.minScore ?? '—'}`]].map(([k, v]) => (
              <div key={k} className="rounded-xl bg-slate-50 border border-slate-200 py-3">
                <p className="text-2xl font-extrabold text-ink tabular-nums">{v}</p>
                <p className="text-xs text-slate-500">{k}</p>
              </div>
            ))}
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Phổ điểm (số học viên theo khoảng điểm)</p>
            <BarChart height={110} format={String} data={data.histogram.map((v, i) => ({ label: i === 9 ? '9-10' : `${i}-${i + 1}`, value: v }))} />
          </div>
          <div className="flex gap-2">
            {[['students', 'Học viên'], ['questions', 'Thống kê từng câu']].map(([id, label]) => (
              <button key={id} onClick={() => setTab(id)} className={`px-4 py-2 rounded-xl text-sm font-semibold border ${tab === id ? 'bg-brand-50 border-brand-500 text-brand-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}>{label}</button>
            ))}
          </div>
          <div className="overflow-x-auto rounded-xl border border-slate-200">
            {tab === 'students' ? (
              <table className="w-full text-sm">
                <thead><tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase"><th className="px-4 py-3">Học viên</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3">Điểm</th><th className="px-4 py-3">Thoát app</th><th className="px-4 py-3">Nộp lúc</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {data.students.map((s) => {
                    const [label, cls] = STATUS[s.status];
                    return (
                      <tr key={s.studentId}>
                        <td className="px-4 py-2.5"><p className="font-semibold text-slate-800">{s.fullName}</p><p className="text-xs text-slate-400">{s.email}</p></td>
                        <td className="px-4 py-2.5"><span className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${cls}`}>{label}</span></td>
                        <td className="px-4 py-2.5 font-bold tabular-nums">{s.score ?? '—'}</td>
                        <td className="px-4 py-2.5">{s.status === 'NOT_STARTED' ? '—' : <span className={s.autoSubmitted ? 'text-rose-600 font-bold' : s.exitCount > 0 ? 'text-amber-600 font-bold' : ''}>{s.exitCount}/2{s.autoSubmitted ? ' · tự nộp' : ''}</span>}</td>
                        <td className="px-4 py-2.5 text-xs text-slate-500">{fmtDateTime(s.submittedAt)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <table className="w-full text-sm">
                <thead><tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase"><th className="px-4 py-3">Câu</th><th className="px-4 py-3">Nội dung</th><th className="px-4 py-3 text-right">Trả lời</th><th className="px-4 py-3 text-right">Đúng</th><th className="px-4 py-3">Tỷ lệ đúng</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {data.questionStats.map((q) => (
                    <tr key={q.questionId}>
                      <td className="px-4 py-2.5 font-bold">{q.orderIndex}</td>
                      <td className="px-4 py-2.5 max-w-md truncate" title={q.content}>{q.content}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{q.answeredCount}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{q.correctCount}</td>
                      <td className="px-4 py-2.5 min-w-[140px]">
                        {q.correctRate == null ? '—' : (
                          <div className="flex items-center gap-2"><div className="h-2 flex-1 rounded-full bg-slate-100 overflow-hidden"><div className={`h-full ${q.correctRate < 50 ? 'bg-rose-500' : 'bg-emerald-500'}`} style={{ width: `${q.correctRate}%` }} /></div><span className="text-xs font-semibold w-10 text-right">{q.correctRate}%</span></div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </Modal>
  );
}
