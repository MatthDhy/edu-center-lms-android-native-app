import React, { useState, useEffect } from 'react';
import { Megaphone, Send, CheckCircle, AlertCircle } from 'lucide-react';
import { courseApi, notificationApi } from '../api/client';
import ConfirmModal from '../components/ConfirmModal';
import { inputCls, labelCls } from '../components/ui';

const TARGETS = [
  { id: 'ALL', label: 'Tất cả', hint: 'Mọi học viên và giảng viên đang hoạt động' },
  { id: 'STUDENTS', label: 'Học viên', hint: 'Chỉ học viên đang hoạt động' },
  { id: 'TEACHERS', label: 'Giảng viên', hint: 'Chỉ giảng viên đang hoạt động' },
  { id: 'COURSE', label: 'Một lớp học', hint: 'Học viên đang học + giảng viên của lớp' },
];

export default function BroadcastPage() {
  const [target, setTarget] = useState('ALL');
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [confirm, setConfirm] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    courseApi.list({ status: 'ACTIVE' }).then((r) => setCourses(r.data || [])).catch(() => {});
  }, []);

  const targetInfo = TARGETS.find((t) => t.id === target);
  const canSend = title.trim() && (target !== 'COURSE' || courseId);

  const send = async () => {
    setSending(true);
    setError('');
    try {
      const res = await notificationApi.broadcast({
        title: title.trim(),
        content: content.trim() || undefined,
        target,
        courseId: target === 'COURSE' ? Number(courseId) : undefined,
      });
      setResult(res.data.recipients);
      setConfirm(false);
      setTitle('');
      setContent('');
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="max-w-2xl space-y-5">
      <div>
        <h3 className="text-lg font-bold text-slate-800">Gửi thông báo chung</h3>
        <p className="text-xs text-slate-500 mt-0.5">Thông báo hiện ở chuông trên app Android của người nhận.</p>
      </div>

      {result != null && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-3 text-sm text-emerald-800">
          <CheckCircle className="w-5 h-5 shrink-0" />
          Đã gửi thông báo cho <strong>{result}</strong> người.
        </div>
      )}
      {error && (
        <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
          <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-5">
        <div>
          <label className={labelCls}>Gửi đến</label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {TARGETS.map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => { setTarget(t.id); setResult(null); }}
                className={`py-2.5 px-3 rounded-xl border text-sm font-semibold transition-all ${
                  target === t.id ? 'bg-brand-50 border-brand-500 text-brand-700 ring-2 ring-brand-500/20' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
          <p className="text-xs text-slate-400 mt-2">{targetInfo.hint}</p>
        </div>

        {target === 'COURSE' && (
          <div>
            <label className={labelCls}>Chọn lớp</label>
            <select className={inputCls} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              <option value="">-- Chọn lớp --</option>
              {courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} ({c.studentCount} học viên)</option>)}
            </select>
          </div>
        )}

        <div>
          <label className={labelCls}>Tiêu đề *</label>
          <input className={inputCls} maxLength={200} placeholder="Ví dụ: Nghỉ lễ Quốc khánh" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div>
          <label className={labelCls}>Nội dung</label>
          <textarea rows={4} maxLength={2000} className={inputCls} value={content} onChange={(e) => setContent(e.target.value)} />
        </div>

        <div className="flex justify-end">
          <button
            type="button"
            disabled={!canSend}
            onClick={() => { setError(''); setConfirm(true); }}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold text-sm shadow-md shadow-brand-500/20 disabled:opacity-50 transition-all"
          >
            <Send className="w-4 h-4" /> Gửi thông báo
          </button>
        </div>
      </div>

      <ConfirmModal
        isOpen={confirm}
        title="Xác nhận gửi thông báo"
        confirmText="Gửi ngay"
        loading={sending}
        error={error}
        onClose={() => setConfirm(false)}
        onConfirm={send}
        message={<>Gửi “<strong>{title}</strong>” đến <strong>{targetInfo.label.toLowerCase()}</strong>? Thông báo đã gửi không thể thu hồi.</>}
      />
    </div>
  );
}
