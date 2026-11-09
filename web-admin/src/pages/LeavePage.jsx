import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, Inbox, ArrowRight } from 'lucide-react';
import { leaveApi } from '../api/client';
import { fmtDateTime, fmtTime } from '../utils/format';
import { cardCls } from '../components/ui';

const ST = {
  PENDING: ['Chờ duyệt', 'bg-amber-50 text-amber-700'],
  APPROVED: ['Đã duyệt', 'bg-emerald-50 text-emerald-700'],
  REJECTED: ['Từ chối', 'bg-rose-50 text-rose-700'],
};

/** Admin xem (chỉ đọc) đơn xin nghỉ & học bù toàn trung tâm. Giảng viên duyệt trên app. */
export default function LeavePage() {
  const [status, setStatus] = useState('');
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setList((await leaveApi.list(status)).data || []); } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, [status]);
  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-2">
        {[['', 'Tất cả'], ['PENDING', 'Chờ duyệt'], ['APPROVED', 'Đã duyệt'], ['REJECTED', 'Từ chối']].map(([id, label]) => (
          <button key={id} onClick={() => setStatus(id)} className={`px-4 py-2 rounded-xl text-sm font-semibold border ${status === id ? 'bg-brand-500 border-brand-500 text-white' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}>{label}</button>
        ))}
        <button onClick={load} className="ml-auto p-2.5 rounded-xl border border-slate-200 bg-white text-slate-500 hover:bg-slate-50"><RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /></button>
      </div>
      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}
      <div className={`${cardCls} overflow-hidden`}>
        {list.length === 0 ? <div className="py-20 text-center text-sm text-slate-400"><Inbox className="w-10 h-10 mx-auto mb-3 text-slate-300" />Không có đơn nào.</div> : (
          <div className="overflow-x-auto"><table className="w-full text-sm">
            <thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Học viên</th><th className="px-5 py-3.5">Buổi xin nghỉ</th><th className="px-5 py-3.5">Học bù</th><th className="px-5 py-3.5">Lý do</th><th className="px-5 py-3.5">Trạng thái</th><th className="px-5 py-3.5">Gửi lúc</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{list.map((r) => (
              <tr key={r.id}>
                <td className="px-5 py-3 font-bold text-ink">{r.studentName}</td>
                <td className="px-5 py-3"><p className="font-semibold text-slate-700">{r.courseName}</p><p className="text-xs text-slate-400">{fmtDateTime(r.lessonStart)}{r.room ? ` • ${r.room}` : ''}</p></td>
                <td className="px-5 py-3">{r.makeupLessonId ? <div><p className="font-semibold text-brand-700 flex items-center gap-1"><ArrowRight className="w-3.5 h-3.5" />{r.makeupCourseName}</p><p className="text-xs text-slate-400">{fmtDateTime(r.makeupStart)}–{fmtTime(r.makeupEnd)}{r.makeupRoom ? ` • ${r.makeupRoom}` : ''}</p></div> : <span className="text-slate-300">—</span>}</td>
                <td className="px-5 py-3 max-w-xs"><p className="text-slate-700 line-clamp-2" title={r.reason}>{r.reason}</p>{r.rejectReason && <p className="text-xs text-rose-600 mt-0.5">Từ chối: {r.rejectReason}</p>}</td>
                <td className="px-5 py-3"><span className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${ST[r.status][1]}`}>{ST[r.status][0]}</span></td>
                <td className="px-5 py-3 text-xs text-slate-500">{fmtDateTime(r.createdAt)}</td>
              </tr>))}</tbody></table></div>
        )}
      </div>
    </div>
  );
}
