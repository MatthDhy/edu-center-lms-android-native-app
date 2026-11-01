import React, { useState } from 'react';
import { Lock, Mail, AlertCircle, GraduationCap, CalendarDays, ClipboardList, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const FEATURES = [
  { icon: Users, text: 'Quản lý học viên & giảng viên' },
  { icon: CalendarDays, text: 'Xếp lịch, phòng, cảnh báo trùng giờ' },
  { icon: ClipboardList, text: 'Theo dõi bài tập và tiến độ lớp' },
];

export default function LoginPage() {
  const { login, loading } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
    } catch (err) {
      setError(err.message || 'Đăng nhập không thành công');
    }
  };

  const field = 'w-full pl-11 pr-4 py-3.5 bg-white border border-slate-200 rounded-xl text-ink placeholder-slate-400 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 transition-all';

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-[#F5F5FB]">
      <div className="hidden lg:flex flex-col justify-between p-12 text-white bg-gradient-to-br from-brand-500 via-brand-700 to-brand-900 relative overflow-hidden">
        <div className="absolute -top-24 -right-24 w-96 h-96 rounded-full bg-white/10 blur-2xl" />
        <div className="absolute bottom-0 -left-20 w-80 h-80 rounded-full bg-brand-300/20 blur-3xl" />
        <div className="flex items-center gap-3 relative">
          <span className="w-12 h-12 rounded-2xl bg-white/15 flex items-center justify-center"><GraduationCap className="w-7 h-7" /></span>
          <span className="text-2xl font-extrabold tracking-tight">CeLeHub</span>
        </div>
        <div className="relative">
          <h2 className="text-5xl font-extrabold leading-tight tracking-tight">Cùng kết nối<br />tri thức.</h2>
          <p className="mt-4 text-brand-100 text-lg max-w-md">Một nơi duy nhất để vận hành trung tâm dạy học của bạn.</p>
          <ul className="mt-8 space-y-3">
            {FEATURES.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-center gap-3 text-brand-50 font-medium">
                <span className="w-9 h-9 rounded-xl bg-white/15 flex items-center justify-center"><Icon className="w-4 h-4" /></span>{text}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-brand-200">Learn. Connect. Grow.</p>
      </div>

      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-md bg-white rounded-3xl p-8 shadow-[0_8px_40px_rgba(46,29,115,0.10)] border border-slate-100">
          <h1 className="text-3xl font-extrabold tracking-tight text-ink">Đăng nhập quản trị</h1>
          <p className="text-sm text-slate-500 mt-1.5 mb-7">Dành cho giáo vụ, kế toán và giám đốc trung tâm.</p>

          {error && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2.5 text-sm text-rose-700">
              <AlertCircle className="w-4 h-4 shrink-0" /><span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">Email</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
                <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="admin@celehub.edu.vn" className={field} />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">Mật khẩu</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
                <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className={field} />
              </div>
            </div>
            <button type="submit" disabled={loading}
              className="w-full mt-2 py-3.5 bg-brand-500 hover:bg-brand-600 text-white rounded-xl font-bold text-sm shadow-lg shadow-brand-500/30 transition-all disabled:opacity-50">
              {loading ? 'Đang xác thực...' : 'Đăng nhập'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
