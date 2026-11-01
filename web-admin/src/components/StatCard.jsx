import React from 'react';
import { cardCls } from './ui';

const TONES = {
  brand: 'bg-brand-50 text-brand-600',
  blue: 'bg-blue-50 text-blue-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  rose: 'bg-rose-50 text-rose-600',
  amber: 'bg-amber-50 text-amber-600',
};

/** Thẻ số liệu: con số lớn, đậm là điểm nhấn; nhãn và ghi chú nhỏ, nhạt hơn. */
export default function StatCard({ label, value, hint, icon: Icon, tone = 'brand' }) {
  return (
    <div className={`${cardCls} p-5`}>
      <div className="flex items-start justify-between gap-3">
        <span className="text-sm font-semibold text-slate-500">{label}</span>
        {Icon && (
          <span className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${TONES[tone]}`}>
            <Icon className="w-5 h-5" />
          </span>
        )}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-4xl font-extrabold tracking-tight text-ink tabular-nums">{value ?? 0}</span>
        {hint && <span className="text-xs font-medium text-slate-400">{hint}</span>}
      </div>
    </div>
  );
}
