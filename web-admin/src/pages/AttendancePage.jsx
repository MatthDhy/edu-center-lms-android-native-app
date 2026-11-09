import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, UserCheck, AlertTriangle, CalendarCheck, Users } from 'lucide-react';
import { courseApi, attendanceApi } from '../api/client';
import StatCard from '../components/StatCard';
import { fmtDate, fmtTime } from '../utils/format';
import { cardCls, inputCls } from '../components/ui';

const CELL = {
  PRESENT: ['bg-emerald-100 text-emerald-700', 'Có'],
  LATE: ['bg-amber-100 text-amber-700', 'Trễ'],
  EXCUSED: ['bg-sky-100 text-sky-700', 'Phép'],
  ABSENT: ['bg-rose-100 text-rose-700', 'Vắng'],
};

/** Admin xem chuyên cần: ma trận học viên × buổi (mỗi phiên điểm danh = 1 buổi). Vắng > 20% bị cảnh báo. */
export default function AttendancePage() {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    courseApi.list({}).then((r) => { const l = r.data || []; setCourses(l); if (l.length) setCourseId(String(l[0].id)); }).catch((e) => setError(e.message));
  }, []);

  const load = useCallback(async () => {
    if (!courseId) { setData(null); return; }
    setLoading(true); setError('');
    try { setData((await attendanceApi.matrix(courseId, from, to)).data); } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, [courseId, from, to]);
  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-6">
      <div className={`${cardCls} p-4 flex flex-wrap items-end gap-3`}>
        <div className="min-w-[240px]"><label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">Lớp học</label>
          <select className={inputCls} value={courseId} onChange={(e) => setCourseId(e.target.value)}>{courses.length === 0 && <option value="">Chưa có lớp</option>}{courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} — {c.teacherName}</option>)}</select></div>
        <div><label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">Từ ngày</label><input type="date" className={inputCls} value={from} onChange={(e) => setFrom(e.target.value)} /></div>
        <div><label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">Đến ngày</label><input type="date" className={inputCls} value={to} onChange={(e) => setTo(e.target.value)} /></div>
        <button onClick={load} className="p-2.5 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50"><RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /></button>
      </div>
      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      {data && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
            <StatCard label="Số buổi đã điểm danh" value={data.sessions.length} icon={CalendarCheck} tone="brand" />
            <StatCard label="Chuyên cần trung bình" value={`${data.avgAttendanceRate}%`} icon={UserCheck} tone="emerald" />
            <StatCard label="Học viên vắng > 20%" value={data.warningCount} hint={`trên ${data.students.length} học viên`} icon={AlertTriangle} tone="rose" />
          </div>
          <div className={`${cardCls} overflow-hidden`}>
            {data.sessions.length === 0 ? (
              <div className="py-20 text-center text-sm text-slate-400"><Users className="w-10 h-10 mx-auto mb-3 text-slate-300" />Lớp chưa có buổi điểm danh nào trong khoảng này.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="text-sm border-collapse">
                  <thead><tr>
                    <th className="sticky left-0 bg-white z-10 px-4 py-3 text-left border-b border-slate-100 min-w-[200px]">Học viên</th>
                    {data.sessions.map((s) => <th key={s.id} className="px-2 py-3 border-b border-slate-100 text-center min-w-[64px]"><span className="block text-xs font-bold normal-case">{fmtDate(s.startTime).slice(0, 5)}</span><span className="block text-[10px] font-medium text-slate-400 normal-case">{fmtTime(s.startTime)}</span></th>)}
                    <th className="px-3 py-3 border-b border-slate-100 text-center">Vắng</th>
                  </tr></thead>
                  <tbody>{data.students.map((s) => (
                    <tr key={s.studentId} className={s.warning ? 'bg-rose-50/60' : ''}>
                      <td className="sticky left-0 z-10 px-4 py-2.5 border-b border-slate-100 font-semibold text-slate-800" style={{ background: s.warning ? '#fff5f6' : '#fff' }}>{s.fullName}{s.warning && <AlertTriangle className="inline w-3.5 h-3.5 ml-1.5 text-rose-500" />}</td>
                      {s.cells.map((c, i) => <td key={i} className="px-2 py-2 border-b border-slate-100 text-center"><span className={`inline-block w-12 py-0.5 rounded-md text-[11px] font-bold ${CELL[c][0]}`}>{CELL[c][1]}</span></td>)}
                      <td className={`px-3 py-2 border-b border-slate-100 text-center font-extrabold tabular-nums ${s.warning ? 'text-rose-600' : 'text-slate-600'}`}>{s.absent}/{data.sessions.length} <span className="text-[11px] font-medium">({s.absentRate}%)</span></td>
                    </tr>))}</tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
