import React, { useState, useEffect } from 'react';
import { FileText, AlertCircle } from 'lucide-react';
import Modal from './Modal';
import { courseApi } from '../api/client';
import { inputCls, labelCls, btnPrimary, btnGhost } from './ui';

export default function MaterialFormModal({ isOpen, onClose, courseId, onSaved }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setTitle(''); setDescription(''); setFile(null); setError('');
  }, [isOpen]);

  const submit = async (e) => {
    e.preventDefault();
    if (!file) { setError('Vui lòng chọn tệp'); return; }
    setLoading(true);
    setError('');
    try {
      const fd = new FormData();
      fd.append('title', title.trim());
      if (description.trim()) fd.append('description', description.trim());
      fd.append('file', file);
      await courseApi.addMaterial(courseId, fd);
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Thêm tài liệu" icon={FileText} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
          </div>
        )}
        <div>
          <label className={labelCls}>Tiêu đề *</label>
          <input required maxLength={200} className={inputCls} placeholder="Slide buổi 1 - Đạo hàm" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div>
          <label className={labelCls}>Mô tả</label>
          <textarea rows={3} className={inputCls} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div>
          <label className={labelCls}>Tệp * (pdf, doc, ppt, xls, zip, ảnh… ≤ 20MB)</label>
          <input type="file" className={inputCls} onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        </div>
        <div className="pt-2 flex justify-end gap-2">
          <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
          <button type="submit" disabled={loading} className={btnPrimary}>{loading ? 'Đang tải lên...' : 'Tải lên'}</button>
        </div>
      </form>
    </Modal>
  );
}