import React, { useState, useEffect } from 'react';
import { ClipboardList, ExternalLink, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { assignmentApi } from '../api/client';
import { fmtDateTime } from '../utils/format';

/** Admin xem (chỉ đọc) ai đã nộp / chưa nộp 1 bài tập. Chấm điểm do giảng viên làm trên app. */
export default function SubmissionsModal({ isOpen, assignmentId, onClose }) {
  const [data, setData] = useState(null);
  const [tab, setTab] = useState('submitted');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen || !assignmentId) return;
    setData(null);
    setTab('submitted');
    setError('');
    setLoading(true);
    assignmentApi
      .submissions(assignmentId)
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [isOpen, assignmentId]);

  const submitted = data?.submitted ?? [];
  const notSubmitted = data?.notSubmitted ?? [];

  return (
    <Modal isOpen={isOpen} title={data ? `Bài nộp: ${data.assignment.title}` : 'Bài nộp'} icon={ClipboardList} onClose={onClose} maxWidth="max-w-3xl">
      {error && (
        <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
          <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
        </div>
      )}
      {loading && <div className="py-10 text-center text-sm text-slate-400">Đang tải...</div>}
      {data && (
        <div className="space-y-4">
          <div className="flex gap-2">
            {[
              ['submitted', `Đã nộp (${submitted.length})`],
              ['pending', `Chưa nộp (${notSubmitted.length})`],
            ].map(([id, label]) => (
              <button
                key={id}
                onClick={() => setTab(id)}
                className={`px-4 py-2 rounded-xl text-sm font-semibold border transition-all ${
                  tab === id ? 'bg-brand-50 border-brand-500 text-brand-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {tab === 'submitted' ? (
            submitted.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">Chưa có bài nộp nào.</p>
            ) : (
              <div className="overflow-x-auto rounded-xl border border-slate-200">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                      <th className="px-4 py-3">Học viên</th>
                      <th className="px-4 py-3">Nộp lúc</th>
                      <th className="px-4 py-3">Điểm</th>
                      <th className="px-4 py-3">Bài làm</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {submitted.map((s) => (
                      <tr key={s.id}>
                        <td className="px-4 py-3">
                          <p className="font-semibold text-slate-800">{s.studentName}</p>
                          <p className="text-xs text-slate-400">{s.studentEmail}</p>
                        </td>
                        <td className="px-4 py-3 text-xs text-slate-600">
                          {fmtDateTime(s.submittedAt)}
                          {s.late && <span className="ml-2 px-1.5 py-0.5 rounded bg-rose-50 text-rose-700 border border-rose-200 font-semibold">Trễ</span>}
                        </td>
                        <td className="px-4 py-3">
                          {s.score == null ? (
                            <span className="text-xs text-amber-600 font-semibold">Chưa chấm</span>
                          ) : (
                            <span className="font-bold text-emerald-700">{s.score} / {data.assignment.maxScore}</span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          {s.fileUrl ? (
                            <a href={s.fileUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600 hover:underline">
                              Mở file <ExternalLink className="w-3 h-3" />
                            </a>
                          ) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : notSubmitted.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">Tất cả học viên đã nộp bài.</p>
          ) : (
            <ul className="divide-y divide-slate-100 border border-slate-200 rounded-xl">
              {notSubmitted.map((s) => (
                <li key={s.id} className="px-4 py-2.5">
                  <p className="text-sm font-semibold text-slate-800">{s.fullName}</p>
                  <p className="text-xs text-slate-400">{s.email}{s.phone ? ` • ${s.phone}` : ''}</p>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </Modal>
  );
}
