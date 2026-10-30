import React from 'react';
import { AlertTriangle } from 'lucide-react';
import Modal from './Modal';
import { btnDanger, btnGhost, btnPrimary } from './ui';

export default function ConfirmModal({
  isOpen, title, message, confirmText = 'Xác nhận', danger = false, loading = false, error = '', onConfirm, onClose,
}) {
  return (
    <Modal isOpen={isOpen} title={title} icon={AlertTriangle} onClose={onClose}>
      <div className="space-y-4">
        <div className="text-sm text-slate-600 leading-relaxed">{message}</div>
        {error && (
          <div className="p-3 text-sm rounded-xl bg-rose-50 border border-rose-200 text-rose-700">{error}</div>
        )}
        <div className="flex items-center justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className={btnGhost}>Hủy</button>
          <button type="button" onClick={onConfirm} disabled={loading} className={danger ? btnDanger : btnPrimary}>
            {loading ? 'Đang xử lý...' : confirmText}
          </button>
        </div>
      </div>
    </Modal>
  );
}
