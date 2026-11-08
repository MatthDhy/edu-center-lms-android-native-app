import React, { useState, useEffect } from 'react';
import { Wallet, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { financeApi } from '../api/client';
import { fmtMoney } from '../utils/format';
import { inputCls, labelCls, btnPrimary, btnGhost } from './ui';

/** Tạo khoản học phí cho CẢ LỚP (mọi học viên đang học). Học viên đã có khoản cùng tiêu đề sẽ được bỏ qua. */
export default function CreateBillsModal({ isOpen, onClose, courses, defaultCourseId, onSaved }) {
  const [courseId, setCourseId] = useState('');
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (!isOpen) return;
    setCourseId(defaultCourseId ? String(defaultCourseId) : '');
    setTitle(''); setAmount(''); setDueDate(''); setError(''); setResult(null);
  }, [isOpen, defaultCourseId]);

  const course = courses.find((c) => String(c.courseId ?? c.id) === courseId);
  const fee = course ? (course.tuitionFee ?? 0) : 0;

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const res = await financeApi.createBills({
        courseId: Number(courseId), title: title.trim(),
        amount: amount === '' ? undefined : Number(amount), dueDate: dueDate || undefined,
      });
      setResult(res.data);
      onSaved?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Tạo khoản thu học phí" icon={Wallet} onClose={onClose}>
      {result ? (
        <div className="space-y-4">
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">
            Đã tạo <b>{result.created}</b> khoản thu{result.skipped > 0 && <>, bỏ qua <b>{result.skipped}</b> (đã có khoản cùng tiêu đề)</>}. Học viên đã nhận thông báo.
          </div>
          <div className="flex justify-end"><button className={btnPrimary} onClick={onClose}>Đóng</button></div>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700"><AlertCircle className="w-4 h-4 shrink-0" />{error}</div>}
          <div>
            <label className={labelCls}>Lớp học *</label>
            <select required className={inputCls} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              <option value="">-- Chọn lớp --</option>
              {courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} ({c.studentCount} HV)</option>)}
            </select>
          </div>
          <div>
            <label className={labelCls}>Tiêu đề khoản thu *</label>
            <input required className={inputCls} placeholder="Học phí tháng 11/2026" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Số tiền (VNĐ)</label>
              <input type="number" min="0" step="1000" className={inputCls} placeholder="Mặc định = học phí lớp" value={amount} onChange={(e) => setAmount(e.target.value)} />
              {course && amount === '' && <p className="text-[11px] text-slate-400 mt-1">Dùng học phí lớp: {fmtMoney(fee)}</p>}
            </div>
            <div>
              <label className={labelCls}>Hạn đóng</label>
              <input type="date" className={inputCls} value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
            <button type="submit" disabled={loading} className={btnPrimary}>{loading ? 'Đang tạo...' : 'Tạo khoản thu'}</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
