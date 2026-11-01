import React from 'react';
import { X } from 'lucide-react';

export default function Modal({ isOpen, title, icon: Icon, onClose, children, maxWidth = 'max-w-md' }) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/50 backdrop-blur-sm p-4">
      <div className={`bg-white rounded-3xl shadow-2xl w-full ${maxWidth} max-h-[90vh] overflow-hidden flex flex-col`}>
        <div className="px-6 py-5 flex items-center justify-between shrink-0 border-b border-slate-100">
          <div className="flex items-center gap-3">
            {Icon && (
              <span className="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center">
                <Icon className="w-5 h-5" />
              </span>
            )}
            <h3 className="text-lg font-extrabold text-ink">{title}</h3>
          </div>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-ink p-2 rounded-xl hover:bg-slate-100 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-6 overflow-y-auto">{children}</div>
      </div>
    </div>
  );
}
