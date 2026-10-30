import React, { useState, useEffect } from 'react';
import { CalendarDays, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { courseApi } from '../api/client';
import { toInputDateTime } from '../utils/format';
import { inputCls, labelCls, btnPrimary, btnGhost } from './ui';

const EMPTY = { startTime: '', endTime: '', room: '', topic: '', status: 'SCHEDULED' };

/** Thêm 1 buổi (lesson = null) hoặc sửa buổi. Backend trả 409 nếu trùng giờ giảng viên / phòng. */
export default function LessonFormModal({ isOpen, onClose, courseId, lesson, onSaved }) {
  const editing = !!lesson;
  const [f, setF] = useState(EMPTY);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setF(
      lesson
        ? {
            startTime: toInputDateTime(lesson.startTime),
            endTime: toInputDateTime(lesson.endTime),
            room: lesson.room ?? '',
            topic: lesson.topic ?? '',
            status: lesson.status,
          }
        : EMPTY
    );
  }, [isOpen, lesson]);

  const set = (k) => (e) => setF((prev) => ({ ...prev, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const body = { startTime: f.startTime, endTime: f.endTime, room: f.room.trim(), topic: f.topic.trim() };
      if (editing) {
        body.status = f.status;
        await courseApi.updateLesson(lesson.id, body);
      } else {
        await courseApi.addLesson(courseId, body);
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title={editing ? 'Sửa buổi học' : 'Thêm buổi học'} icon={CalendarDays} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
          </div>
        )}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Bắt đầu *</label>
            <input required type="datetime-local" className={inputCls} value={f.startTime} onChange={set('startTime')} />
          </div>
          <div>
            <label className={labelCls}>Kết thúc *</label>
            <input required type="datetime-local" className={inputCls} value={f.endTime} onChange={set('endTime')} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Phòng</label>
            <input className={inputCls} placeholder="P.301" value={f.room} onChange={set('room')} />
          </div>
          {editing && (
            <div>
              <label className={labelCls}>Trạng thái</label>
              <select className={inputCls} value={f.status} onChange={set('status')}>
                <option value="SCHEDULED">SCHEDULED (sắp diễn ra)</option>
                <option value="DONE">DONE (đã dạy)</option>
                <option value="CANCELED">CANCELED (hủy)</option>
              </select>
            </div>
          )}
        </div>
        <div>
          <label className={labelCls}>Chủ đề</label>
          <input className={inputCls} placeholder="Ví dụ: Đạo hàm" value={f.topic} onChange={set('topic')} />
        </div>
        <p className="text-xs text-slate-400">Buổi học phải diễn ra trong cùng một ngày. Đổi giờ/phòng sẽ tự báo cho học viên.</p>
        <div className="pt-2 flex items-center justify-end gap-2">
          <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
          <button type="submit" disabled={loading} className={btnPrimary}>{loading ? 'Đang lưu...' : 'Lưu'}</button>
        </div>
      </form>
    </Modal>
  );
}
