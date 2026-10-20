import React, { useState } from 'react';
import { X, UserPlus, CheckCircle, Copy, AlertCircle } from 'lucide-react';
import { adminApi } from '../api/client';

export default function CreateUserModal({ isOpen, onClose, defaultRole = 'STUDENT', onSuccess }) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState(defaultRole);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [createdResult, setCreatedResult] = useState(null);
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await adminApi.createUser({
        fullName: fullName.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
        role: role,
      });
      setCreatedResult(res.data);
      if (onSuccess) onSuccess(res.data);
    } catch (err) {
      setError(err.message || 'Không thể tạo tài khoản');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFullName('');
    setEmail('');
    setPhone('');
    setError('');
    setCreatedResult(null);
    setCopied(false);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border border-slate-100">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-brand-500/10 text-brand-600 flex items-center justify-center">
              <UserPlus className="w-4 h-4" />
            </div>
            <h3 className="font-bold text-slate-800">
              {createdResult ? 'Tạo tài khoản thành công' : `Cấp tài khoản mới (${role === 'TEACHER' ? 'Giảng viên' : 'Học viên'})`}
            </h3>
          </div>
          <button
            onClick={handleClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-200/50 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6">
          {createdResult ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                <div className="text-sm">
                  <p className="font-semibold text-emerald-900">Đã khởi tạo tài khoản thành công!</p>
                  <p className="text-emerald-700 mt-1">
                    Hệ thống đã lưu thông tin và gửi email hướng dẫn đăng nhập. Người dùng sẽ phải <strong>đổi mật khẩu mới</strong> ngay lần đầu đăng nhập trên Mobile App.
                  </p>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-2 text-sm">
                <div className="flex justify-between py-1 border-b border-slate-200">
                  <span className="text-slate-500">Họ và tên:</span>
                  <span className="font-semibold text-slate-800">{createdResult.fullName}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-200">
                  <span className="text-slate-500">Email:</span>
                  <span className="font-semibold text-slate-800">{createdResult.email}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-200">
                  <span className="text-slate-500">Vai trò:</span>
                  <span className="px-2 py-0.5 rounded text-xs font-semibold bg-brand-100 text-brand-800">
                    {createdResult.role}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Mật khẩu tạm:</span>
                  <span className="text-xs text-amber-600 font-medium">(Đã gửi qua email & ghi log backend)</span>
                </div>
              </div>

              <button
                type="button"
                onClick={handleClose}
                className="w-full mt-4 py-2.5 px-4 bg-brand-600 hover:bg-brand-700 text-white rounded-xl font-semibold text-sm transition-all shadow-md shadow-brand-500/20"
              >
                Hoàn tất & Đóng
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-sm text-rose-700">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Vai trò (Role)
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('STUDENT')}
                    className={`py-2 px-3 text-xs font-semibold rounded-lg border transition-all ${
                      role === 'STUDENT'
                        ? 'bg-brand-50 border-brand-500 text-brand-700 ring-2 ring-brand-500/20'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    Học viên (STUDENT)
                  </button>
                  <button
                    type="button"
                    onClick={() => setRole('TEACHER')}
                    className={`py-2 px-3 text-xs font-semibold rounded-lg border transition-all ${
                      role === 'TEACHER'
                        ? 'bg-purple-50 border-purple-500 text-purple-700 ring-2 ring-purple-500/20'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    Giảng viên (TEACHER)
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Họ và tên <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Nguyễn Văn An"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 text-sm transition-all"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Địa chỉ Email <span className="text-rose-500">*</span>
                </label>
                <input
                  type="email"
                  required
                  placeholder="Ví dụ: an.nguyen@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 text-sm transition-all"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Số điện thoại
                </label>
                <input
                  type="tel"
                  placeholder="Ví dụ: 0912345678"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 text-sm transition-all"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-4 py-2.5 rounded-xl text-sm font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-5 py-2.5 rounded-xl text-sm font-semibold bg-brand-600 hover:bg-brand-700 text-white shadow-md shadow-brand-500/20 disabled:opacity-50 transition-all"
                >
                  {loading ? 'Đang khởi tạo...' : 'Tạo tài khoản'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
