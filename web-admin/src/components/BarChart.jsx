import React from 'react';
import { shortMoney } from '../utils/month';

/** Biểu đồ cột thuần CSS (không cần thư viện). data: [{ label, value }]. */
export default function BarChart({ data, highlight, height = 180, format = shortMoney, color = 'bg-brand-500' }) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div className="flex items-stretch gap-2" style={{ height }}>
      {data.map((d) => {
        const on = highlight === undefined || highlight === d.label;
        return (
          <div key={d.label} className="flex-1 flex flex-col min-w-0" title={`${d.label}: ${d.value}`}>
            <span className="text-[10px] font-semibold text-slate-500 text-center h-4 tabular-nums truncate">{d.value > 0 ? format(d.value) : ''}</span>
            <div className="flex-1 w-full flex items-end">
              <div className={`w-full rounded-t-md transition-all ${color} ${on ? '' : 'opacity-40'}`} style={{ height: `${(d.value / max) * 100}%`, minHeight: 3 }} />
            </div>
            <span className={`text-[10px] text-center mt-1 truncate ${on ? 'font-bold text-ink' : 'text-slate-400'}`}>{d.label}</span>
          </div>
        );
      })}
    </div>
  );
}
