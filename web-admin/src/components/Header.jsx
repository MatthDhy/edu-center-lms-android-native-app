import React from 'react';
import { Bell, ChevronRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const DAYS = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

function today() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${DAYS[d.getDay()]}, ${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export default function Header({ title }) {
  const { user } = useAuth();
  const initials = (user?.fullName || 'QT').split(/\s+/).map((w) => w[0]).slice(-2).join('').toUpperCase();

  return (
    <header className="h-16 bg-white/85 backdrop-blur border-b border-slate-200 px-8 flex items-center justify-between sticky top-0 z-10">
      <div className="flex items-center gap-1.5 text-sm">
        <span className="text-slate-400">Quản trị</span>
        <ChevronRight className="w-4 h-4 text-slate-300" />
        <span className="font-bold text-ink">{title}</span>
      </div>
      <div className="flex items-center gap-4">
        <span className="text-sm font-medium text-slate-500">{today()}</span>
        <span className="relative p-2 rounded-xl text-slate-500">
          <Bell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500 ring-2 ring-white" />
        </span>
        <span className="w-9 h-9 rounded-full bg-brand-100 text-brand-700 font-bold text-xs flex items-center justify-center">{initials}</span>
      </div>
    </header>
  );
}
