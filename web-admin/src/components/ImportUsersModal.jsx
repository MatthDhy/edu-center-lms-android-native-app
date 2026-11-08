import React, { useState, useEffect } from 'react';
import { Upload, Download, AlertCircle, CheckCircle } from 'lucide-react';
import Modal from './Modal';
import { adminApi } from '../api/client';
import { downloadBlob } from '../utils/month';
import { btnPrimary, btnGhost } from './ui';

/** Nhập hàng loạt học viên / giảng viên từ Excel (cột fullName, email, phone, role). */
export default function ImportUsersModal({ isOpen, onClose, onDone }) {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (isOpen) { setFile(null); setError(''); setResult(null); }
  }, [isOpen]);

  const template = async () => {
    try { downloadBlob(await adminApi.importTemplate(), 'users_template.xlsx'); } catch (e) { setError(e.message); }
  };

  const submit = async () => {
    if (!file) return;
    setLoading(true);
    setError('');
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await adminApi.importUsers(fd);
      setResult(res.data);
      onDone?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Nhập tài khoản từ Excel" icon={Upload} onClose={onClose} maxWidth="max-w-lg">
      <div className="space-y-4">
        {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2 text-sm text-rose-700"><AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />{error}</div>}
        {result ? (
          <div className="space-y-3">
            <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-3 text-emerald-800">
              <CheckCircle className="w-6 h-6 shrink-0" />
              <span className="text-sm">Đã tạo <b className="text-xl mx-1">{result.created}</b> tài khoản. Mật khẩu tạm được gửi qua email từng người.</span>
            </div>
            {result.failed.length > 0 && (
              <div className="border border-rose-200 rounded-xl overflow-hidden">
                <p className="px-4 py-2 bg-rose-50 text-xs font-bold text-rose-700">{result.failed.length} dòng lỗi (không được tạo)</p>
                <ul className="max-h-48 overflow-y-auto divide-y divide-slate-100 text-sm">
                  {result.failed.map((f, i) => (
                    <li key={i} className="px-4 py-2"><b>Dòng {f.row}</b> {f.email && <span className="text-slate-500">· {f.email}</span>}<br /><span className="text-rose-600 text-xs">{f.error}</span></li>
                  ))}
                </ul>
              </div>
            )}
            <div className="flex justify-end"><button className={btnPrimary} onClick={onClose}>Đóng</button></div>
          </div>
        ) : (
          <>
            <p className="text-sm text-slate-600">File .xlsx gồm các cột <code className="px-1 bg-slate-100 rounded">fullName</code>, <code className="px-1 bg-slate-100 rounded">email</code>, <code className="px-1 bg-slate-100 rounded">phone</code> (tùy chọn), <code className="px-1 bg-slate-100 rounded">role</code> (STUDENT hoặc TEACHER, mặc định STUDENT). Tối đa 500 dòng.</p>
            <button type="button" onClick={template} className="inline-flex items-center gap-2 text-sm font-semibold text-brand-600 hover:underline"><Download className="w-4 h-4" /> Tải file mẫu</button>
            <input type="file" accept=".xlsx" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="block w-full text-sm border border-slate-200 rounded-xl p-2" />
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
              <button type="button" onClick={submit} disabled={!file || loading} className={btnPrimary}>{loading ? 'Đang nhập...' : 'Nhập tài khoản'}</button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
