import React, { useState } from 'react';
import { X, KeyRound, Copy, Check, AlertTriangle } from 'lucide-react';
import { adminApi } from '../api/client';

export default function ResetPasswordModal({ isOpen, onClose, user, onSuccess }) {
  const [loading, setLoading] = useState(false);
  const [tempPassword, setTempPassword] = useState('');
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !user) return null;

  const handleReset = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.resetPassword(user.id);
      setTempPassword(res.data.tempPassword);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.message || 'Không thể đặt lại mật khẩu');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(tempPassword);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleClose = () => {
    setTempPassword('');
    setError('');
    setCopied(false);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border border-slate-100">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-amber-50/50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center">
              <KeyRound className="w-4 h-4" />
            </div>
            <h3 className="font-bold text-slate-800">Cấp lại mật khẩu tạm</h3>
          </div>
          <button
            onClick={handleClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-200/50 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          {tempPassword ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">
                <p className="font-semibold text-emerald-900 mb-1">Mật khẩu tạm mới đã được tạo!</p>
                <p className="text-xs text-emerald-700">
                  Học viên/Giảng viên có thể đăng nhập bằng mật khẩu này và hệ thống sẽ buộc đổi mật khẩu mới ở bước tiếp theo.
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <p className="text-xs text-slate-500 font-medium mb-1.5">Mật khẩu tạm:</p>
                <div className="flex items-center justify-between bg-white px-3.5 py-2.5 rounded-lg border border-slate-300">
                  <span className="font-mono text-base font-bold text-slate-800 tracking-wider">
                    {tempPassword}
                  </span>
                  <button
                    onClick={handleCopy}
                    className="flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-md bg-brand-50 text-brand-700 hover:bg-brand-100 transition-colors"
                  >
                    {copied ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-600" />
                        <span className="text-emerald-700">Đã chép</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Sao chép</span>
                      </>
                    )}
                  </button>
                </div>
              </div>

              <button
                onClick={handleClose}
                className="w-full py-2.5 px-4 bg-slate-800 hover:bg-slate-900 text-white rounded-xl font-semibold text-sm transition-all"
              >
                Đóng
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-start gap-3 p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm">
                <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold">Bạn có chắc chắn muốn đặt lại mật khẩu?</p>
                  <p className="text-xs mt-1 text-amber-700">
                    Mật khẩu cũ của <strong>{user.fullName}</strong> ({user.email}) sẽ bị vô hiệu hóa. Hệ thống sẽ tạo một mật khẩu ngẫu nhiên mới và kích hoạt cờ <em>must_change_password</em>.
                  </p>
                </div>
              </div>

              {error && (
                <div className="p-3 text-xs rounded-lg bg-rose-50 border border-rose-200 text-rose-700">
                  {error}
                </div>
              )}

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-4 py-2.5 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  Hủy bỏ
                </button>
                <button
                  type="button"
                  onClick={handleReset}
                  disabled={loading}
                  className="px-5 py-2.5 rounded-xl text-sm font-semibold bg-amber-600 hover:bg-amber-700 text-white shadow-md shadow-amber-500/20 disabled:opacity-50 transition-all"
                >
                  {loading ? 'Đang tạo...' : 'Xác nhận tạo mật khẩu mới'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
