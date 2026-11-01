import React, { useState, useEffect, useCallback } from 'react';
import {
  Search, PlusCircle, RefreshCw, Lock, Unlock, KeyRound, Trash2, Filter, ChevronDown,
  UserX, UserCheck, Users, Mail, Phone,
} from 'lucide-react';
import { adminApi } from '../api/client';
import CreateUserModal from '../components/CreateUserModal';
import ResetPasswordModal from '../components/ResetPasswordModal';
import ConfirmModal from '../components/ConfirmModal';
import StatCard from '../components/StatCard';
import { cardCls, btnPrimary, inputCls } from '../components/ui';

const initials = (n = '') => n.split(/\s+/).map((w) => w[0]).slice(-2).join('').toUpperCase();

function StatusBadge({ status }) {
  const on = status === 'ACTIVE';
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold ${on ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${on ? 'bg-emerald-500' : 'bg-rose-500'}`} />
      {on ? 'Đang hoạt động' : 'Đã khóa'}
    </span>
  );
}

function UserTable({ users, role, onToggleStatus, onResetPassword, onDelete, updatingId }) {
  if (users.length === 0) {
    return (
      <div className="py-20 text-center text-sm text-slate-400">
        <UserX className="w-10 h-10 mx-auto mb-3 text-slate-300" />
        <p>Không có tài khoản nào phù hợp.</p>
      </div>
    );
  }
  const avatar = role === 'TEACHER' ? 'bg-blue-50 text-blue-700' : 'bg-brand-100 text-brand-700';
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left border-b border-slate-100">
            <th className="px-5 py-3.5">Họ và tên</th>
            <th className="px-5 py-3.5">Liên hệ</th>
            <th className="px-5 py-3.5">Trạng thái</th>
            <th className="px-5 py-3.5">Ngày tạo</th>
            <th className="px-5 py-3.5">Mật khẩu</th>
            <th className="px-5 py-3.5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {users.map((u) => (
            <tr key={u.id} className="transition-colors">
              <td className="px-5 py-4">
                <div className="flex items-center gap-3">
                  <span className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm shrink-0 ${avatar}`}>{initials(u.fullName) || '?'}</span>
                  <div>
                    <p className="font-bold text-ink">{u.fullName}</p>
                    <p className="text-xs text-brand-600 font-semibold">#{u.id}</p>
                  </div>
                </div>
              </td>
              <td className="px-5 py-4 space-y-1">
                <p className="flex items-center gap-1.5 text-slate-700 font-medium"><Mail className="w-3.5 h-3.5 text-slate-400" />{u.email}</p>
                {u.phone && <p className="flex items-center gap-1.5 text-xs text-slate-500"><Phone className="w-3.5 h-3.5 text-slate-400" />{u.phone}</p>}
              </td>
              <td className="px-5 py-4"><StatusBadge status={u.status} /></td>
              <td className="px-5 py-4 text-slate-600">{u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}</td>
              <td className="px-5 py-4">
                <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${u.mustChangePassword ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-slate-600'}`}>
                  {u.mustChangePassword ? 'Chưa đổi' : 'Đã đổi'}
                </span>
              </td>
              <td className="px-5 py-4">
                <div className="flex items-center justify-end gap-2">
                  <button onClick={() => onResetPassword(u)} title="Cấp lại mật khẩu tạm" className="p-2 rounded-lg text-amber-600 hover:bg-amber-50 border border-amber-200 transition-colors"><KeyRound className="w-4 h-4" /></button>
                  {u.status === 'ACTIVE' ? (
                    <button onClick={() => onToggleStatus(u, 'LOCKED')} disabled={updatingId === u.id} title="Khóa tài khoản" className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200 disabled:opacity-40"><Lock className="w-4 h-4" /></button>
                  ) : (
                    <button onClick={() => onToggleStatus(u, 'ACTIVE')} disabled={updatingId === u.id} title="Mở khóa" className="p-2 rounded-lg text-emerald-600 hover:bg-emerald-50 border border-emerald-200 disabled:opacity-40"><Unlock className="w-4 h-4" /></button>
                  )}
                  <button onClick={() => onDelete(u)} title="Xóa tài khoản" className="p-2 rounded-lg text-slate-500 hover:text-white hover:bg-rose-600 border border-slate-200 hover:border-rose-600 transition-colors"><Trash2 className="w-4 h-4" /></button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function UserListPage({ role, title, subtitle }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [resetTarget, setResetTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const [updatingId, setUpdatingId] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.getUsers({ role, status: statusFilter || undefined, search: search.trim() || undefined });
      setUsers(res.data || []);
    } catch (err) {
      console.error('Error fetching users:', err);
    } finally {
      setLoading(false);
    }
  }, [role, statusFilter, search]);

  useEffect(() => {
    const t = setTimeout(fetchUsers, 300);
    return () => clearTimeout(t);
  }, [fetchUsers]);

  const handleToggleStatus = async (user, newStatus) => {
    setUpdatingId(user.id);
    try {
      await adminApi.updateStatus(user.id, newStatus);
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u)));
    } catch (err) {
      alert('Không thể cập nhật trạng thái: ' + err.message);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    setDeleteError('');
    try {
      await adminApi.deleteUser(deleteTarget.id);
      setUsers((prev) => prev.filter((u) => u.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch (err) {
      setDeleteError(err.message);
    } finally {
      setDeleting(false);
    }
  };

  const activeCount = users.filter((u) => u.status === 'ACTIVE').length;
  const lockedCount = users.filter((u) => u.status === 'LOCKED').length;
  const pendingCount = users.filter((u) => u.mustChangePassword).length;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <StatCard label="Tổng tài khoản" value={users.length} hint="theo bộ lọc" icon={Users} tone="brand" />
        <StatCard label="Đang hoạt động" value={activeCount} icon={UserCheck} tone="emerald" />
        <StatCard label="Đã khóa" value={lockedCount} icon={UserX} tone="rose" />
        <StatCard label="Chưa đổi mật khẩu" value={pendingCount} icon={KeyRound} tone="amber" />
      </div>

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 flex flex-col lg:flex-row lg:items-center gap-3 border-b border-slate-100">
          <div className="lg:mr-auto">
            <h3 className="text-lg font-bold text-ink">{title}</h3>
            <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input className={`${inputCls} pl-9 lg:w-72`} placeholder="Tìm theo tên, email, SĐT..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
            <select className={`${inputCls} appearance-none pl-9 pr-8`} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="LOCKED">Đã khóa</option>
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          </div>
          <button onClick={fetchUsers} disabled={loading} className="p-2.5 text-slate-500 hover:bg-slate-100 rounded-xl border border-slate-200" title="Làm mới">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button onClick={() => setShowModal(true)} className={btnPrimary}><PlusCircle className="w-4 h-4" /> Cấp tài khoản</button>
        </div>
        {loading && users.length === 0 ? (
          <div className="py-20 text-center text-sm text-slate-400">Đang tải dữ liệu...</div>
        ) : (
          <UserTable users={users} role={role} onToggleStatus={handleToggleStatus}
            onResetPassword={setResetTarget} onDelete={(u) => { setDeleteError(''); setDeleteTarget(u); }} updatingId={updatingId} />
        )}
      </div>

      <CreateUserModal isOpen={showModal} onClose={() => setShowModal(false)} defaultRole={role}
        onSuccess={() => { fetchUsers(); setShowModal(false); }} />
      <ResetPasswordModal isOpen={!!resetTarget} onClose={() => setResetTarget(null)} user={resetTarget} onSuccess={fetchUsers} />
      <ConfirmModal
        isOpen={!!deleteTarget} title="Xóa tài khoản" danger confirmText="Xóa vĩnh viễn"
        loading={deleting} error={deleteError} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete}
        message={deleteTarget && (<>
          Xóa vĩnh viễn <strong>{deleteTarget.fullName}</strong> ({deleteTarget.email})? Không thể hoàn tác.<br />
          Chỉ xóa được tài khoản chưa có lớp học, bài nộp hay điểm danh; nếu đã có dữ liệu hãy dùng nút <strong>Khóa</strong>.
        </>)}
      />
    </div>
  );
}
