import React, { useState, useEffect } from 'react';
import { Repeat, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { courseApi } from '../api/client';
import { WEEKDAYS, toIsoDate } from '../utils/format';
import { inputCls, labelCls, btnPrimary, btnGhost } from './ui';

export default function GenerateLessonsModal({ isOpen, onClose, courseId, defaultSessions, onSaved }) {
  const [startDate, setStartDate] = useState('');
  const [days, setDays] = useState([]);
  const [startTime, setStartTime] = useState('18:00');
  const [endTime, setEndTime] = useState('19:30');
  const [room, setRoom] = useState('');
  const [topic, setTopic] = useState('');
  const [sessions, setSessions] = useState('12');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setStartDate(toIsoDate(new Date()));
    setDays([]);
    setSessions(defaultSessions ? String(defaultSessions) : '12');
  }, [isOpen, defaultSessions]);

  const toggleDay = (n) => setDays((prev) => (prev.includes(n) ? prev.filter((x) => x !== n) : [...prev, n].sort()));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (days.length === 0) {
      setError('Chọn ít nhất 1 thứ trong tuần');
      return;
    }
    setLoading(true);
    try {
      await courseApi.generateLessons(courseId, {
        startDate,
        daysOfWeek: days,
        startTime,
        endTime,
        room: room.trim(),
        topic: topic.trim(),
        sessions: Number(sessions),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Sinh lịch học lặp theo tuần" icon={Repeat} onClose={onClose} maxWidth="max-w-lg">
      <form onSubmit={submit} className="space-y-4">
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2 text-sm text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" /><span>{error}</span>
          </div>
        )}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Bắt đầu từ ngày *</label>
            <input required type="date" className={inputCls} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </div>
          <div>
            <label className={labelCls}>Số buổi *</label>
            <input required type="number" min="1" max="200" className={inputCls} value={sessions} onChange={(e) => setSessions(e.target.value)} />
          </div>
        </div>
        <div>
          <label className={labelCls}>Học vào các thứ *</label>
          <div className="flex flex-wrap gap-2">
            {WEEKDAYS.map((name, i) => {
              const n = i + 1;
              const on = days.includes(n);
              return (
                <button
                  key={n}
                  type="button"
                  onClick={() => toggleDay(n)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                    on ? 'bg-brand-50 border-brand-500 text-brand-700 ring-2 ring-brand-500/20' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  {name}
                </button>
              );
            })}
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Giờ bắt đầu *</label>
            <input required type="time" className={inputCls} value={startTime} onChange={(e) => setStartTime(e.target.value)} />
          </div>
          <div>
            <label className={labelCls}>Giờ kết thúc *</label>
            <input required type="time" className={inputCls} value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Phòng</label>
            <input className={inputCls} placeholder="P.301" value={room} onChange={(e) => setRoom(e.target.value)} />
          </div>
          <div>
            <label className={labelCls}>Chủ đề chung</label>
            <input className={inputCls} value={topic} onChange={(e) => setTopic(e.target.value)} />
          </div>
        </div>
        <p className="text-xs text-slate-400">
          Chỉ cần 1 buổi trùng giờ giảng viên hoặc trùng phòng là cả đợt bị hủy (409) và hệ thống báo buổi nào bị trùng.
        </p>
        <div className="pt-2 flex items-center justify-end gap-2">
          <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
          <button type="submit" disabled={loading} className={btnPrimary}>{loading ? 'Đang tạo...' : 'Tạo lịch'}</button>
        </div>
      </form>
    </Modal>
  );
}
