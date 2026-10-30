import React, { useState, useEffect, useCallback } from 'react';
import {
  Search, PlusCircle, RefreshCw, Lock, Unlock, KeyRound, Trash2,
  Filter, ChevronDown, UserX, UserCheck, Mail, Phone, Calendar
} from 'lucide-react';
import { adminApi } from '../api/client';
import CreateUserModal from '../components/CreateUserModal';
import ResetPasswordModal from '../components/ResetPasswordModal';
import ConfirmModal from '../components/ConfirmModal';

function StatusBadge({ status }) {
  return status === 'ACTIVE' ? (
    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> ACTIVE
    </span>
  ) : (
    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
      <span className="w-1.5 h-1.5 rounded-full bg-rose-500"></span> LOCKED
    </span>
  );
}

function UserTable({ users, onToggleStatus, onResetPassword, onDelete, updatingId }) {
  if (users.length === 0) {
    return (
      <div className="py-20 text-center text-sm text-slate-400">
        <UserX className="w-10 h-10 mx-auto mb-3 text-slate-300" />
        <p>Không có tài khoản nào phù hợp.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
            <th className="px-5 py-3.5">Tên đầy đủ</th>
            <th className="px-5 py-3.5">Email & SĐT</th>
            <th className="px-5 py-3.5">Trạng thái</th>
            <th className="px-5 py-3.5">Lần tạo</th>
            <th className="px-5 py-3.5 text-center">Phải đổi pass?</th>
            <th className="px-5 py-3.5 text-right">Hành động</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {users.map((u) => (
            <tr key={u.id} className="hover:bg-slate-50/60 transition-colors">
              <td className="px-5 py-3.5">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-slate-200 to-slate-300 flex items-center justify-center font-bold text-sm text-slate-700 shrink-0">
                    {u.fullName?.charAt(0) || '?'}
                  </div>
                  <div>
                    <p className="font-semibold text-slate-800">{u.fullName}</p>
                    <p className="text-[11px] text-slate-400">ID: #{u.id}</p>
                  </div>
                </div>
              </td>
              <td className="px-5 py-3.5">
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 text-xs text-slate-600">
                    <Mail className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-medium">{u.email}</span>
                  </div>
                  {u.phone && (
                    <div className="flex items-center gap-1.5 text-xs text-slate-500">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      <span>{u.phone}</span>
                    </div>
                  )}
                </div>
              </td>
              <td className="px-5 py-3.5">
                <StatusBadge status={u.status} />
              </td>
              <td className="px-5 py-3.5">
                <div className="flex items-center gap-1.5 text-xs text-slate-500">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  <span>{u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}</span>
                </div>
              </td>
              <td className="px-5 py-3.5 text-center">
                {u.mustChangePassword ? (
                  <span className="inline-block px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                    Chưa đổi
                  </span>
                ) : (
                  <span className="inline-block px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    Đã đổi
                  </span>
                )}
              </td>
              <td className="px-5 py-3.5">
                <div className="flex items-center justify-end gap-2">
                  <button
                    onClick={() => onResetPassword(u)}
                    title="Cấp lại mật khẩu tạm"
                    className="p-2 rounded-lg text-amber-600 hover:text-amber-700 hover:bg-amber-50 border border-amber-200 transition-colors"
                  >
                    <KeyRound className="w-4 h-4" />
                  </button>
                  {u.status === 'ACTIVE' ? (
                    <button
                      onClick={() => onToggleStatus(u, 'LOCKED')}
                      disabled={updatingId === u.id}
                      title="Khóa tài khoản"
                      className="p-2 rounded-lg text-rose-600 hover:text-rose-700 hover:bg-rose-50 border border-rose-200 transition-colors disabled:opacity-40"
                    >
                      <Lock className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      onClick={() => onToggleStatus(u, 'ACTIVE')}
                      disabled={updatingId === u.id}
                      title="Mở khóa tài khoản"
                      className="p-2 rounded-lg text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 border border-emerald-200 transition-colors disabled:opacity-40"
                    >
                      <Unlock className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={() => onDelete(u)}
                    title="Xóa tài khoản"
                    className="p-2 rounded-lg text-slate-500 hover:text-white hover:bg-rose-600 border border-slate-200 hover:border-rose-600 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
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
      const res = await adminApi.getUsers({
        role,
        status: statusFilter || undefined,
        search: search.trim() || undefined,
      });
      setUsers(res.data || []);
    } catch (err) {
      console.error('Error fetching users:', err);
    } finally {
      setLoading(false);
    }
  }, [role, statusFilter, search]);

  useEffect(() => {
    const debounce = setTimeout(fetchUsers, 300);
    return () => clearTimeout(debounce);
  }, [fetchUsers]);

  const handleToggleStatus = async (user, newStatus) => {
    setUpdatingId(user.id);
    try {
      await adminApi.updateStatus(user.id, newStatus);
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, status: newStatus } : u))
      );
    } catch (err) {
      alert('Không thể cập nhật trạng thái: ' + err.message);
    } finally {
      setUpdatingId(null);
    }
  };

  const openDelete = (user) => {
    setDeleteError('');
    setDeleteTarget(user);
  };

  const handleDelete = async () => {
    setDeleting(true);
    setDeleteError('');
    try {
      await adminApi.deleteUser(deleteTarget.id);
      setUsers((prev) => prev.filter((u) => u.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch (err) {
      // 409: tài khoản đã có dữ liệu (lớp, bài nộp...) -> gợi ý khóa thay vì xóa
      setDeleteError(err.message);
    } finally {
      setDeleting(false);
    }
  };

  const handleCreateSuccess = () => {
    fetchUsers();
    setShowModal(false);
  };

  const activeCount = users.filter((u) => u.status === 'ACTIVE').length;
  const lockedCount = users.filter((u) => u.status === 'LOCKED').length;

  return (
    <div className="space-y-6 animate-in fade-in duration-150">
      {/* Page header + Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h3 className="text-lg font-bold text-slate-800">{title}</h3>
          <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold text-sm shadow-md shadow-brand-500/20 transition-all"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Cấp tài khoản mới</span>
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center font-black text-slate-700">
            {users.length}
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium">Tổng số</p>
            <p className="text-sm font-bold text-slate-700">Tài khoản</p>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <UserCheck className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium">Đang hoạt động</p>
            <p className="text-sm font-bold text-emerald-700">{activeCount} ACTIVE</p>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl border border-slate-200 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <UserX className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium">Đã khóa</p>
            <p className="text-sm font-bold text-rose-700">{lockedCount} LOCKED</p>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Tìm theo tên, email, SĐT..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 transition-all"
          />
        </div>
        <div className="relative">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="appearance-none pl-9 pr-8 py-2.5 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-500 bg-white transition-all cursor-pointer"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">ACTIVE (đang hoạt động)</option>
            <option value="LOCKED">LOCKED (đã khóa)</option>
          </select>
          <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
        </div>
        <button
          onClick={fetchUsers}
          disabled={loading}
          className="p-2.5 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-xl border border-slate-200 transition-colors"
          title="Làm mới"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Table */}
      {loading && users.length === 0 ? (
        <div className="py-20 text-center text-sm text-slate-400">Đang tải dữ liệu từ PostgreSQL...</div>
      ) : (
        <UserTable
          users={users}
          onToggleStatus={handleToggleStatus}
          onResetPassword={(u) => setResetTarget(u)}
          onDelete={openDelete}
          updatingId={updatingId}
        />
      )}

      {/* Modals */}
      <CreateUserModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        defaultRole={role}
        onSuccess={handleCreateSuccess}
      />
      <ResetPasswordModal
        isOpen={!!resetTarget}
        onClose={() => setResetTarget(null)}
        user={resetTarget}
        onSuccess={fetchUsers}
      />
      <ConfirmModal
        isOpen={!!deleteTarget}
        title="Xóa tài khoản"
        danger
        confirmText="Xóa vĩnh viễn"
        loading={deleting}
        error={deleteError}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        message={
          deleteTarget && (
            <>
              Xóa vĩnh viễn <strong>{deleteTarget.fullName}</strong> ({deleteTarget.email})? Không thể hoàn tác.
              <br />
              Chỉ xóa được tài khoản chưa có lớp học, bài nộp hay điểm danh; nếu đã có dữ liệu hãy dùng nút <strong>Khóa</strong>.
            </>
          )
        }
      />
    </div>
  );
}
