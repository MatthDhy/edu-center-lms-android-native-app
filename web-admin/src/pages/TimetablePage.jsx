import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { ChevronLeft, ChevronRight, RefreshCw, AlertTriangle } from 'lucide-react';
import { courseApi } from '../api/client';
import LessonFormModal from '../components/LessonFormModal';
import { WEEKDAYS, addDays, mondayOf, toIsoDate, fmtTime } from '../utils/format';
import { inputCls, cardCls } from '../components/ui';

const PALETTE = [
  'bg-brand-50 border-brand-300 text-brand-900',
  'bg-blue-50 border-blue-300 text-blue-900',
  'bg-emerald-50 border-emerald-300 text-emerald-900',
  'bg-amber-50 border-amber-300 text-amber-900',
  'bg-pink-50 border-pink-300 text-pink-900',
  'bg-cyan-50 border-cyan-300 text-cyan-900',
];
const NO_ROOM = '(Chưa xếp phòng)';

function findConflicts(lessons) {
  const live = lessons.filter((l) => l.status !== 'CANCELED');
  const bad = new Set();
  for (let i = 0; i < live.length; i++) {
    for (let j = i + 1; j < live.length; j++) {
      const a = live[i], b = live[j];
      const overlap = new Date(a.startTime) < new Date(b.endTime) && new Date(b.startTime) < new Date(a.endTime);
      if (!overlap) continue;
      const sameRoom = a.room && b.room && a.room.toLowerCase() === b.room.toLowerCase();
      if (sameRoom || a.teacherId === b.teacherId) { bad.add(a.id); bad.add(b.id); }
    }
  }
  return bad;
}

const dm = (d) => `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;

export default function TimetablePage() {
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()));
  const [lessons, setLessons] = useState([]);
  const [teacherFilter, setTeacherFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [edit, setEdit] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await courseApi.schedule(toIsoDate(weekStart), toIsoDate(addDays(weekStart, 6)));
      setLessons(res.data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [weekStart]);

  useEffect(() => { load(); }, [load]);

  const teachers = useMemo(() => {
    const m = new Map();
    lessons.forEach((l) => m.set(l.teacherId, l.teacherName));
    return [...m.entries()];
  }, [lessons]);
  const visible = useMemo(() => lessons.filter((l) => !teacherFilter || String(l.teacherId) === teacherFilter), [lessons, teacherFilter]);
  const conflicts = useMemo(() => findConflicts(lessons), [lessons]);
  const rooms = useMemo(() => {
    const set = new Set(visible.map((l) => l.room || NO_ROOM));
    return [...set].sort((a, b) => (a === NO_ROOM ? 1 : b === NO_ROOM ? -1 : a.localeCompare(b, 'vi')));
  }, [visible]);

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
  const todayIso = toIsoDate(new Date());
  const cell = (room, day) =>
    visible.filter((l) => (l.room || NO_ROOM) === room && l.startTime.slice(0, 10) === toIsoDate(day))
      .sort((a, b) => a.startTime.localeCompare(b.startTime));

  return (
    <div className="space-y-5">
      <div className={`${cardCls} p-4 flex flex-col lg:flex-row lg:items-center gap-3`}>
        <div className="flex items-center gap-2">
          <button onClick={() => setWeekStart(addDays(weekStart, -7))} className="p-2.5 rounded-xl border border-slate-200 hover:bg-slate-50"><ChevronLeft className="w-4 h-4" /></button>
          <button onClick={() => setWeekStart(mondayOf(new Date()))} className="px-4 py-2.5 rounded-xl bg-brand-50 text-brand-700 hover:bg-brand-100 text-sm font-bold">Tuần này</button>
          <button onClick={() => setWeekStart(addDays(weekStart, 7))} className="p-2.5 rounded-xl border border-slate-200 hover:bg-slate-50"><ChevronRight className="w-4 h-4" /></button>
          <span className="ml-3 text-lg font-extrabold text-ink tabular-nums">{dm(weekStart)} – {dm(addDays(weekStart, 6))}</span>
        </div>
        <div className="flex items-center gap-2 lg:ml-auto">
          <select className={`${inputCls} w-56`} value={teacherFilter} onChange={(e) => setTeacherFilter(e.target.value)}>
            <option value="">Tất cả giảng viên</option>
            {teachers.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
          </select>
          <button onClick={load} className="p-2.5 text-slate-500 hover:bg-slate-100 rounded-xl border border-slate-200" title="Làm mới">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {conflicts.size > 0 && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-sm font-medium text-rose-700 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          Có <b>{conflicts.size}</b> buổi đang trùng giờ phòng/giảng viên (viền đỏ). Bấm vào buổi để chỉnh lại.
        </div>
      )}
      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      {rooms.length === 0 ? (
        <div className={`${cardCls} py-20 text-center text-sm text-slate-400`}>Tuần này chưa có buổi học nào. Tạo lịch trong mục “Quản lý lớp học”.</div>
      ) : (
        <div className={`${cardCls} overflow-x-auto`}>
          <table className="w-full text-sm border-collapse min-w-[960px]">
            <thead>
              <tr>
                <th className="px-4 py-3 text-left border-b border-r border-slate-100 w-36">Phòng</th>
                {days.map((d, i) => {
                  const today = toIsoDate(d) === todayIso;
                  return (
                    <th key={i} className={`px-3 py-3 text-left border-b border-slate-100 ${today ? '!bg-brand-500 !text-white' : ''}`}>
                      {WEEKDAYS[i]}
                      <span className={`block text-lg font-extrabold normal-case tracking-normal ${today ? 'text-white' : 'text-ink'}`}>{dm(d)}</span>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {rooms.map((room) => (
                <tr key={room} className="align-top">
                  <td className="px-4 py-3 font-extrabold text-ink border-r border-b border-slate-100 bg-slate-50/60">{room}</td>
                  {days.map((d, i) => (
                    <td key={i} className="px-2 py-2 border-b border-slate-100 min-w-[130px] space-y-1.5">
                      {cell(room, d).map((l) => (
                        <button key={l.id} onClick={() => setEdit(l)}
                          className={`w-full text-left px-2.5 py-2 rounded-xl border text-[11px] leading-tight transition-shadow hover:shadow-md ${PALETTE[l.courseId % PALETTE.length]} ${conflicts.has(l.id) ? '!border-rose-500 ring-2 ring-rose-300' : ''} ${l.status === 'CANCELED' ? 'opacity-50 line-through' : ''}`}>
                          <span className="font-extrabold text-xs block tabular-nums">{fmtTime(l.startTime)}–{fmtTime(l.endTime)}</span>
                          <span className="block font-bold truncate mt-0.5">{l.courseName}</span>
                          <span className="block opacity-70 truncate">{l.teacherName}</span>
                        </button>
                      ))}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <LessonFormModal isOpen={!!edit} onClose={() => setEdit(null)} courseId={edit?.courseId} lesson={edit} onSaved={load} />
    </div>
  );
}
