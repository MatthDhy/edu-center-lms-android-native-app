import React, { useState, useEffect } from 'react';
import { Send, CheckCircle, AlertCircle, Users, GraduationCap, BookOpen, School, ImagePlus, X } from 'lucide-react';
import { courseApi, notificationApi } from '../api/client';
import ConfirmModal from '../components/ConfirmModal';
import { inputCls, labelCls, cardCls, btnPrimary } from '../components/ui';

const TARGETS = [
  { id: 'ALL', label: 'Tất cả', hint: 'Mọi học viên và giảng viên đang hoạt động', icon: Users },
  { id: 'STUDENTS', label: 'Học viên', hint: 'Chỉ học viên đang hoạt động', icon: GraduationCap },
  { id: 'TEACHERS', label: 'Giảng viên', hint: 'Chỉ giảng viên đang hoạt động', icon: BookOpen },
  { id: 'COURSE', label: 'Một lớp học', hint: 'Học viên đang học + giảng viên của lớp', icon: School },
];

export default function BroadcastPage() {
  const [target, setTarget] = useState('ALL');
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState('');
  const [confirm, setConfirm] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => { courseApi.list({ status: 'ACTIVE' }).then((r) => setCourses(r.data || [])).catch(() => {}); }, []);
  useEffect(() => {
    if (!image) { setPreview(''); return undefined; }
    const url = URL.createObjectURL(image);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [image]);

  const info = TARGETS.find((t) => t.id === target);
  const canSend = title.trim() && (target !== 'COURSE' || courseId);

  const pickImage = (f) => {
    if (!f) return;
    if (f.size > 5 * 1024 * 1024) { setError('Ảnh tối đa 5MB'); return; }
    setError(''); setImage(f);
  };

  const send = async () => {
    setSending(true); setError('');
    try {
      let payload;
      if (image) {
        payload = new FormData();
        payload.append('title', title.trim());
        if (content.trim()) payload.append('content', content.trim());
        payload.append('target', target);
        if (target === 'COURSE') payload.append('courseId', courseId);
        payload.append('image', image);
      } else {
        payload = { title: title.trim(), content: content.trim() || undefined, target, courseId: target === 'COURSE' ? Number(courseId) : undefined };
      }
      const res = await notificationApi.broadcast(payload);
      setResult(res.data.recipients);
      setConfirm(false); setTitle(''); setContent(''); setImage(null);
    } catch (err) { setError(err.message); } finally { setSending(false); }
  };

  return (
    <div className="max-w-3xl space-y-5">
      {result != null && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center gap-3 text-emerald-800"><CheckCircle className="w-6 h-6 shrink-0" />
          <span className="text-sm">Đã gửi thông báo cho <b className="text-2xl font-extrabold mx-1">{result}</b> người (có push tới điện thoại nếu đã bật Firebase).</span></div>
      )}
      {error && !confirm && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700"><AlertCircle className="w-4 h-4 shrink-0" />{error}</div>}

      <div className={`${cardCls} p-6 space-y-6`}>
        <div>
          <label className={labelCls}>Gửi đến</label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {TARGETS.map(({ id, label, icon: Icon }) => (
              <button key={id} type="button" onClick={() => { setTarget(id); setResult(null); }} className={`flex flex-col items-center gap-2 py-4 rounded-2xl border-2 text-sm font-bold transition-all ${target === id ? 'bg-brand-50 border-brand-500 text-brand-700' : 'border-slate-200 text-slate-600 hover:border-brand-200 hover:bg-slate-50'}`}><Icon className="w-6 h-6" />{label}</button>
            ))}
          </div>
          <p className="text-xs text-slate-500 mt-2.5">{info.hint}</p>
        </div>
        {target === 'COURSE' && (
          <div><label className={labelCls}>Chọn lớp</label>
            <select className={inputCls} value={courseId} onChange={(e) => setCourseId(e.target.value)}><option value="">-- Chọn lớp --</option>{courses.map((c) => <option key={c.id} value={c.id}>{c.courseName} ({c.studentCount} học viên)</option>)}</select></div>
        )}
        <div><label className={labelCls}>Tiêu đề *</label><input className={`${inputCls} text-base font-semibold`} maxLength={200} placeholder="Ví dụ: Nghỉ lễ Quốc khánh" value={title} onChange={(e) => setTitle(e.target.value)} /></div>
        <div><label className={labelCls}>Nội dung</label><textarea rows={5} maxLength={2000} className={inputCls} value={content} onChange={(e) => setContent(e.target.value)} /><p className="text-right text-xs text-slate-400 mt-1 tabular-nums">{content.length}/2000</p></div>
        <div>
          <label className={labelCls}>Ảnh đính kèm (tùy chọn, 1 ảnh ≤ 5MB)</label>
          {preview ? (
            <div className="relative inline-block"><img src={preview} alt="" className="max-h-48 rounded-xl border border-slate-200" />
              <button type="button" onClick={() => setImage(null)} className="absolute -top-2 -right-2 w-7 h-7 rounded-full bg-rose-600 text-white flex items-center justify-center shadow"><X className="w-4 h-4" /></button></div>
          ) : (
            <label className="flex items-center justify-center gap-2 py-6 rounded-xl border-2 border-dashed border-slate-300 text-sm font-semibold text-slate-500 hover:border-brand-400 hover:text-brand-600 cursor-pointer"><ImagePlus className="w-5 h-5" /> Chọn ảnh<input type="file" accept="image/png,image/jpeg,image/webp" className="hidden" onChange={(e) => pickImage(e.target.files?.[0])} /></label>
          )}
        </div>
        <div className="flex justify-end"><button type="button" disabled={!canSend} onClick={() => { setError(''); setConfirm(true); }} className={btnPrimary}><Send className="w-4 h-4" /> Gửi thông báo</button></div>
      </div>

      <ConfirmModal isOpen={confirm} title="Xác nhận gửi thông báo" confirmText="Gửi ngay" loading={sending} error={error} onClose={() => setConfirm(false)} onConfirm={send}
        message={<>Gửi “<strong>{title}</strong>” đến <strong>{info.label.toLowerCase()}</strong>{image ? ' kèm 1 ảnh' : ''}? Thông báo đã gửi không thể thu hồi.</>} />
    </div>
  );
}
