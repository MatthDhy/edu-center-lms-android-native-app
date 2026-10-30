import React, { useState, useEffect } from 'react';
import { School, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { courseApi } from '../api/client';
import { inputCls, labelCls, btnPrimary, btnGhost } from './ui';

const EMPTY = { courseName: '', teacherId: '', tuitionFee: '', totalSessions: '', description: '', status: 'ACTIVE' };

export default function CourseFormModal({ isOpen, onClose, course, teachers, onSaved }) {
  const editing = !!course;
  const [f, setF] = useState(EMPTY);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setF(
      course
        ? {
            courseName: course.courseName,
            teacherId: String(course.teacherId),
            tuitionFee: String(course.tuitionFee ?? ''),
            totalSessions: course.totalSessions == null ? '' : String(course.totalSessions),
            description: course.description ?? '',
            status: course.status,
          }
        : EMPTY
    );
  }, [isOpen, course]);

  const set = (k) => (e) => setF((prev) => ({ ...prev, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const body = {
        courseName: f.courseName.trim(),
        description: f.description.trim(),
        teacherId: Number(f.teacherId),
        tuitionFee: f.tuitionFee === '' ? 0 : Number(f.tuitionFee),
        totalSessions: f.totalSessions === '' ? undefined : Number(f.totalSessions),
      };
      if (editing) {
        body.status = f.status;
        await courseApi.update(course.id, body);
      } else {
        await courseApi.create(body);
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
    <Modal isOpen={isOpen} title={editing ? 'Sửa lớp học' : 'Tạo lớp học mới'} icon={School} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
          </div>
        )}
        <div>
          <label className={labelCls}>Tên lớp <span className="text-rose-500">*</span></label>
          <input required className={inputCls} placeholder="Ví dụ: Toán 11A" value={f.courseName} onChange={set('courseName')} />
        </div>
        <div>
          <label className={labelCls}>Giảng viên phụ trách <span className="text-rose-500">*</span></label>
          <select required className={inputCls} value={f.teacherId} onChange={set('teacherId')}>
            <option value="">-- Chọn giảng viên --</option>
            {teachers.map((t) => (
              <option key={t.id} value={t.id}>{t.fullName} ({t.email})</option>
            ))}
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Học phí (VNĐ)</label>
            <input type="number" min="0" step="1000" className={inputCls} placeholder="1500000" value={f.tuitionFee} onChange={set('tuitionFee')} />
          </div>
          <div>
            <label className={labelCls}>Tổng số buổi</label>
            <input type="number" min="1" className={inputCls} placeholder="36" value={f.totalSessions} onChange={set('totalSessions')} />
          </div>
        </div>
        {editing && (
          <div>
            <label className={labelCls}>Trạng thái</label>
            <select className={inputCls} value={f.status} onChange={set('status')}>
              <option value="ACTIVE">ACTIVE (đang mở)</option>
              <option value="CLOSED">CLOSED (đã đóng)</option>
            </select>
          </div>
        )}
        <div>
          <label className={labelCls}>Mô tả</label>
          <textarea rows={3} className={inputCls} value={f.description} onChange={set('description')} />
        </div>
        <div className="pt-2 flex items-center justify-end gap-2">
          <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
          <button type="submit" disabled={loading} className={btnPrimary}>
            {loading ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo lớp'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
