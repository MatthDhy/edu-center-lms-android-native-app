import React, { useState, useEffect, useMemo } from 'react';
import { UserPlus, Search, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { adminApi, courseApi } from '../api/client';
import { inputCls, btnPrimary, btnGhost } from './ui';

/** Chọn nhiều học viên (ACTIVE, chưa có trong lớp) để xếp vào lớp. */
export default function EnrollStudentsModal({ isOpen, onClose, courseId, enrolledIds, onSaved }) {
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState('');
  const [picked, setPicked] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setPicked([]);
    setSearch('');
    setError('');
    setLoading(true);
    adminApi
      .getUsers({ role: 'STUDENT', status: 'ACTIVE' })
      .then((res) => setStudents(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [isOpen]);

  const available = useMemo(() => {
    const q = search.trim().toLowerCase();
    return students.filter(
      (s) =>
        !enrolledIds.includes(s.id) &&
        (q === '' || s.fullName.toLowerCase().includes(q) || s.email.toLowerCase().includes(q))
    );
  }, [students, search, enrolledIds]);

  const toggle = (id) => setPicked((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const submit = async () => {
    setSaving(true);
    setError('');
    try {
      await courseApi.enroll(courseId, picked);
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Xếp học viên vào lớp" icon={UserPlus} onClose={onClose} maxWidth="max-w-lg">
      <div className="space-y-3">
        {error && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
          </div>
        )}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input className={`${inputCls} pl-9`} placeholder="Tìm theo tên hoặc email..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="border border-slate-200 rounded-xl divide-y divide-slate-100 max-h-72 overflow-y-auto">
          {loading ? (
            <div className="py-10 text-center text-sm text-slate-400">Đang tải...</div>
          ) : available.length === 0 ? (
            <div className="py-10 text-center text-sm text-slate-400">Không còn học viên nào để xếp.</div>
          ) : (
            available.map((s) => (
              <label key={s.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-slate-50 cursor-pointer">
                <input type="checkbox" checked={picked.includes(s.id)} onChange={() => toggle(s.id)} className="w-4 h-4 accent-blue-600" />
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-slate-800 truncate">{s.fullName}</p>
                  <p className="text-xs text-slate-400 truncate">{s.email}</p>
                </div>
              </label>
            ))
          )}
        </div>
        <div className="flex items-center justify-between pt-2">
          <span className="text-xs text-slate-500">Đã chọn: <strong>{picked.length}</strong></span>
          <div className="flex gap-2">
            <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
            <button type="button" onClick={submit} disabled={saving || picked.length === 0} className={btnPrimary}>
              {saving ? 'Đang xếp...' : 'Xếp vào lớp'}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
