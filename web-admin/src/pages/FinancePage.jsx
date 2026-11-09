import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, Wallet, TrendingUp, AlertTriangle, Percent, PlusCircle, CheckCircle2, Trash2, Bell, Banknote, Search, Zap } from 'lucide-react';
import { financeApi, courseApi } from '../api/client';
import StatCard from '../components/StatCard';
import BarChart from '../components/BarChart';
import Modal from '../components/Modal';
import ConfirmModal from '../components/ConfirmModal';
import CreateBillsModal from '../components/CreateBillsModal';
import { fmtMoney, fmtDate, fmtDateTime } from '../utils/format';
import { currentMonth } from '../utils/month';
import { cardCls, inputCls, btnPrimary, btnGhost, labelCls } from '../components/ui';

const STUDENT_STATUS = {
  PAID: ['Đã đóng đủ', 'bg-emerald-50 text-emerald-700'],
  DEBT: ['Còn nợ', 'bg-amber-50 text-amber-700'],
  OVERDUE: ['Quá hạn', 'bg-rose-50 text-rose-700'],
  NO_BILL: ['Chưa có khoản thu', 'bg-slate-100 text-slate-600'],
};
const BILL_STATUS = {
  PAID: ['Đã đóng', 'bg-emerald-50 text-emerald-700'],
  UNPAID: ['Chưa đóng', 'bg-amber-50 text-amber-700'],
  OVERDUE: ['Quá hạn', 'bg-rose-50 text-rose-700'],
};
const Badge = ({ map, k }) => { const [l, c] = map[k] || [k, 'bg-slate-100 text-slate-600']; return <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold whitespace-nowrap ${c}`}>{l}</span>; };
const Err = ({ text }) => text ? <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{text}</div> : null;
const Rate = ({ v }) => (
  <div className="flex items-center gap-2 min-w-[120px]">
    <div className="h-2 flex-1 rounded-full bg-slate-100 overflow-hidden"><div className={`h-full ${v >= 80 ? 'bg-emerald-500' : v >= 50 ? 'bg-amber-500' : 'bg-rose-500'}`} style={{ width: `${Math.min(100, v)}%` }} /></div>
    <span className="text-xs font-bold w-11 text-right tabular-nums">{v}%</span>
  </div>
);

export default function FinancePage() {
  const [tab, setTab] = useState('overview');
  const [month, setMonth] = useState(currentMonth());
  const [ov, setOv] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);          // { course, students }
  const [createFor, setCreateFor] = useState({ open: false, courseId: null });
  const [remindConfirm, setRemindConfirm] = useState(false);
  const [remindMsg, setRemindMsg] = useState('');

  // công nợ
  const [bills, setBills] = useState([]);
  const [fStatus, setFStatus] = useState('');
  const [fCourse, setFCourse] = useState('');
  const [fSearch, setFSearch] = useState('');
  const [cashTarget, setCashTarget] = useState(null);
  const [delTarget, setDelTarget] = useState(null);
  const [simTarget, setSimTarget] = useState(null);
  const [act, setAct] = useState({ loading: false, error: '' });

  const loadOverview = useCallback(async () => {
    setLoading(true); setError('');
    try { setOv((await financeApi.overview(month)).data); } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, [month]);

  const loadBills = useCallback(async () => {
    setLoading(true); setError('');
    try { setBills((await financeApi.bills({ status: fStatus || undefined, courseId: fCourse || undefined, search: fSearch.trim() || undefined })).data || []); }
    catch (e) { setError(e.message); } finally { setLoading(false); }
  }, [fStatus, fCourse, fSearch]);

  useEffect(() => { courseApi.list({}).then((r) => setCourses(r.data || [])).catch(() => {}); }, []);
  useEffect(() => { if (tab === 'overview') loadOverview(); }, [tab, loadOverview]);
  useEffect(() => {
    if (tab !== 'bills') return undefined;
    const t = setTimeout(loadBills, 300);
    return () => clearTimeout(t);
  }, [tab, loadBills]);

  const openDetail = async (courseId) => {
    setError('');
    try { setDetail((await financeApi.course(courseId, month)).data); } catch (e) { setError(e.message); }
  };
  const refreshAll = () => { loadOverview(); if (tab === 'bills') loadBills(); if (detail) openDetail(detail.course.courseId); };

  const run = async (fn, done) => {
    setAct({ loading: true, error: '' });
    try { await fn(); done(); refreshAll(); setAct({ loading: false, error: '' }); }
    catch (e) { setAct({ loading: false, error: e.message }); }
  };

  const totalBilled = (ov?.courses || []).reduce((s, c) => s + c.billed, 0);
  const totalPaid = (ov?.courses || []).reduce((s, c) => s + c.paid, 0);
  const rate = totalBilled ? Math.round((totalPaid * 1000) / totalBilled) / 10 : 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        {[['overview', 'Doanh thu & theo lớp'], ['bills', 'Công nợ chi tiết']].map(([id, label]) => (
          <button key={id} onClick={() => setTab(id)} className={`px-5 py-2.5 rounded-xl text-sm font-bold border transition-all ${tab === id ? 'bg-brand-500 border-brand-500 text-white shadow-md shadow-brand-500/25' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}>{label}</button>
        ))}
        <div className="ml-auto flex flex-wrap items-center gap-2">
          {tab === 'overview' && <input type="month" className={`${inputCls} w-44`} value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} />}
          <button onClick={refreshAll} className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-500 hover:bg-slate-50" title="Làm mới"><RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /></button>
          <button onClick={() => { setRemindMsg(''); setRemindConfirm(true); }} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 font-semibold text-sm"><Bell className="w-4 h-4" /> Nhắc học phí tháng</button>
          <button onClick={() => setCreateFor({ open: true, courseId: null })} className={btnPrimary}><PlusCircle className="w-4 h-4" /> Tạo khoản thu</button>
        </div>
      </div>
      {remindMsg && <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">{remindMsg}</div>}
      <Err text={error} />

      {tab === 'overview' && ov && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
            <StatCard label={`Doanh thu tháng ${ov.month}`} value={fmtMoney(ov.revenueMonth)} hint={`QR ${fmtMoney(ov.revenueMonthVietQr)} · TM ${fmtMoney(ov.revenueMonthCash)}`} icon={TrendingUp} tone="emerald" />
            <StatCard label="Công nợ còn phải thu" value={fmtMoney(ov.outstandingTotal)} hint={`${ov.outstandingCount} khoản`} icon={Wallet} tone="amber" />
            <StatCard label="Nợ quá hạn" value={fmtMoney(ov.overdueTotal)} hint={`${ov.overdueCount} khoản`} icon={AlertTriangle} tone="rose" />
            <StatCard label="Tỷ lệ đã thu (cộng dồn)" value={`${rate}%`} hint={`${fmtMoney(totalPaid)} / ${fmtMoney(totalBilled)}`} icon={Percent} tone="brand" />
          </div>

          <div className={`${cardCls} p-6`}>
            <h3 className="text-lg font-bold text-ink mb-1">Doanh thu 12 tháng gần nhất</h3>
            <p className="text-xs text-slate-500 mb-4">Tổng tiền học phí đã thu theo ngày thanh toán. Cột đậm là tháng đang chọn.</p>
            <BarChart data={ov.monthly.map((m) => ({ label: m.month.slice(2).replace('-', '/'), value: m.amount }))}
              highlight={ov.month.slice(2).replace('-', '/')} height={200} />
          </div>

          <div className={`${cardCls} overflow-hidden`}>
            <div className="p-5 border-b border-slate-100">
              <h3 className="text-lg font-bold text-ink">Tình hình đóng học phí theo lớp</h3>
              <p className="text-xs text-slate-500 mt-0.5">Bấm vào một lớp để xem từng học viên đã đóng / còn nợ.</p>
            </div>
            {ov.courses.length === 0 ? <div className="py-16 text-center text-sm text-slate-400">Chưa có lớp học nào.</div> : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Lớp</th><th className="px-5 py-3.5 text-center">Sĩ số</th><th className="px-5 py-3.5">Đã thu / Phải thu</th><th className="px-5 py-3.5">Tỷ lệ</th><th className="px-5 py-3.5 text-right">Còn nợ</th><th className="px-5 py-3.5 text-right">Quá hạn</th><th className="px-5 py-3.5 text-center">HV đã đóng / còn nợ</th><th className="px-5 py-3.5 text-right">Thu trong tháng</th></tr></thead>
                  <tbody className="divide-y divide-slate-100">
                    {ov.courses.map((c) => (
                      <tr key={c.courseId} className="cursor-pointer" onClick={() => openDetail(c.courseId)}>
                        <td className="px-5 py-3.5"><p className="font-bold text-brand-700">{c.courseName}</p><p className="text-xs text-slate-400">{c.teacherName}</p></td>
                        <td className="px-5 py-3.5 text-center font-bold tabular-nums">{c.studentCount}</td>
                        <td className="px-5 py-3.5 tabular-nums"><b>{fmtMoney(c.paid)}</b><span className="text-slate-400"> / {fmtMoney(c.billed)}</span></td>
                        <td className="px-5 py-3.5"><Rate v={c.collectionRate} /></td>
                        <td className="px-5 py-3.5 text-right font-semibold tabular-nums text-amber-700">{fmtMoney(c.unpaid)}</td>
                        <td className={`px-5 py-3.5 text-right font-semibold tabular-nums ${c.overdue > 0 ? 'text-rose-600' : 'text-slate-300'}`}>{fmtMoney(c.overdue)}</td>
                        <td className="px-5 py-3.5 text-center"><span className="text-emerald-600 font-bold">{c.paidStudents}</span> / <span className={c.debtStudents > 0 ? 'text-rose-600 font-bold' : 'text-slate-300'}>{c.debtStudents}</span></td>
                        <td className="px-5 py-3.5 text-right font-bold tabular-nums">{fmtMoney(c.revenueInMonth)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {tab === 'bills' && (
        <div className={`${cardCls} overflow-hidden`}>
          <div className="p-5 flex flex-col lg:flex-row lg:items-center gap-3 border-b border-slate-100">
            <h3 className="text-lg font-bold text-ink lg:mr-auto">Danh sách khoản thu</h3>
            <div className="relative"><Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" /><input className={`${inputCls} pl-9 lg:w-64`} placeholder="Tên, email, nội dung CK..." value={fSearch} onChange={(e) => setFSearch(e.target.value)} /></div>
            <select className={inputCls + ' lg:w-48'} value={fStatus} onChange={(e) => setFStatus(e.target.value)}><option value="">Mọi trạng thái</option><option value="UNPAID">Chưa đóng</option><option value="OVERDUE">Quá hạn</option><option value="PAID">Đã đóng</option></select>
            <select className={inputCls + ' lg:w-56'} value={fCourse} onChange={(e) => setFCourse(e.target.value)}><option value="">Mọi lớp</option>{courses.map((c) => <option key={c.id} value={c.id}>{c.courseName}</option>)}</select>
          </div>
          {bills.length === 0 ? <div className="py-16 text-center text-sm text-slate-400">Không có khoản thu nào.</div> : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Học viên</th><th className="px-5 py-3.5">Lớp / Khoản thu</th><th className="px-5 py-3.5 text-right">Số tiền</th><th className="px-5 py-3.5 text-right">Đã đóng</th><th className="px-5 py-3.5">Hạn</th><th className="px-5 py-3.5">Trạng thái</th><th className="px-5 py-3.5">Nội dung CK</th><th className="px-5 py-3.5 text-right">Thao tác</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {bills.map((b) => (
                    <tr key={b.id}>
                      <td className="px-5 py-3"><p className="font-bold text-ink">{b.studentName}</p><p className="text-xs text-slate-400">{b.studentEmail}</p></td>
                      <td className="px-5 py-3"><p className="font-semibold text-slate-700">{b.courseName}</p><p className="text-xs text-slate-400">{b.title}</p></td>
                      <td className="px-5 py-3 text-right font-bold tabular-nums">{fmtMoney(b.amount)}</td>
                      <td className="px-5 py-3 text-right tabular-nums text-emerald-700">{fmtMoney(b.paid)}{b.paidAt && <p className="text-[11px] text-slate-400">{fmtDateTime(b.paidAt)}</p>}</td>
                      <td className="px-5 py-3 text-xs text-slate-600">{b.dueDate ? fmtDate(b.dueDate) : '—'}</td>
                      <td className="px-5 py-3"><Badge map={BILL_STATUS} k={b.status} /></td>
                      <td className="px-5 py-3 font-mono text-xs text-slate-500">{b.transferContent}</td>
                      <td className="px-5 py-3">
                        {b.status !== 'PAID' ? (
                          <div className="flex justify-end gap-2">
                            <button title="Xác nhận thu tiền mặt" onClick={() => { setAct({ loading: false, error: '' }); setCashTarget(b); }} className="p-2 rounded-lg text-emerald-600 hover:bg-emerald-50 border border-emerald-200"><CheckCircle2 className="w-4 h-4" /></button>
                            <button title="Giả lập chuyển khoản (demo)" onClick={() => { setAct({ loading: false, error: '' }); setSimTarget(b); }} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100"><Zap className="w-4 h-4" /></button>
                            <button title="Xóa khoản thu" onClick={() => { setAct({ loading: false, error: '' }); setDelTarget(b); }} className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200"><Trash2 className="w-4 h-4" /></button>
                          </div>
                        ) : <p className="text-right text-xs text-slate-400">BL{String(b.paymentId ?? '').padStart(6, '0')}</p>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Chi tiết một lớp */}
      <Modal isOpen={!!detail} title={detail ? `Học phí lớp ${detail.course.courseName}` : ''} icon={Banknote} onClose={() => setDetail(null)} maxWidth="max-w-4xl">
        {detail && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-center">
              {[['Phải thu', fmtMoney(detail.course.billed)], ['Đã thu', fmtMoney(detail.course.paid)], ['Còn nợ', fmtMoney(detail.course.unpaid)], ['Quá hạn', fmtMoney(detail.course.overdue)]].map(([k, v]) => (
                <div key={k} className="rounded-xl bg-slate-50 border border-slate-200 py-3"><p className="text-lg font-extrabold text-ink tabular-nums">{v}</p><p className="text-xs text-slate-500">{k}</p></div>
              ))}
            </div>
            <div className="flex justify-between items-center"><Rate v={detail.course.collectionRate} />
              <button className={btnPrimary} onClick={() => setCreateFor({ open: true, courseId: detail.course.courseId })}><PlusCircle className="w-4 h-4" /> Tạo khoản thu cho lớp</button></div>
            <div className="overflow-x-auto rounded-xl border border-slate-200">
              <table className="w-full text-sm">
                <thead><tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase"><th className="px-4 py-3">Học viên</th><th className="px-4 py-3 text-right">Phải đóng</th><th className="px-4 py-3 text-right">Đã đóng</th><th className="px-4 py-3 text-right">Còn nợ</th><th className="px-4 py-3">Tình trạng</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {detail.students.map((s) => (
                    <tr key={s.studentId}>
                      <td className="px-4 py-2.5"><p className="font-semibold text-slate-800">{s.fullName}</p><p className="text-xs text-slate-400">{s.email}{s.phone ? ` • ${s.phone}` : ''}</p></td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{fmtMoney(s.billed)}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums text-emerald-700">{fmtMoney(s.paid)}</td>
                      <td className={`px-4 py-2.5 text-right tabular-nums font-semibold ${s.debt > 0 ? 'text-rose-600' : 'text-slate-300'}`}>{fmtMoney(s.debt)}</td>
                      <td className="px-4 py-2.5"><Badge map={STUDENT_STATUS} k={s.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>

      <CreateBillsModal isOpen={createFor.open} onClose={() => setCreateFor({ open: false, courseId: null })} courses={courses} defaultCourseId={createFor.courseId} onSaved={refreshAll} />

      <ConfirmModal isOpen={!!cashTarget} title="Xác nhận thu tiền mặt" confirmText="Đã thu đủ" loading={act.loading} error={act.error} onClose={() => setCashTarget(null)}
        onConfirm={() => run(() => financeApi.confirmCash(cashTarget.id), () => setCashTarget(null))}
        message={cashTarget && <>Xác nhận đã nhận <b>{fmtMoney(cashTarget.amount - cashTarget.paid)}</b> tiền mặt của <b>{cashTarget.studentName}</b> ({cashTarget.title})? Học viên sẽ nhận thông báo và biên lai.</>} />
      <ConfirmModal isOpen={!!delTarget} danger title="Xóa khoản thu" confirmText="Xóa" loading={act.loading} error={act.error} onClose={() => setDelTarget(null)}
        onConfirm={() => run(() => financeApi.deleteBill(delTarget.id), () => setDelTarget(null))}
        message={delTarget && <>Xóa khoản <b>{delTarget.title}</b> của <b>{delTarget.studentName}</b>? Chỉ xóa được khi chưa có giao dịch.</>} />
      <ConfirmModal isOpen={remindConfirm} title="Gửi thông báo học phí tháng" confirmText="Gửi ngay" loading={act.loading} error={act.error} onClose={() => setRemindConfirm(false)}
        onConfirm={() => run(async () => { const r = await financeApi.sendMonthlyReminder(); setRemindMsg(`Đã gửi cho ${r.data.sent} học viên (mỗi người chỉ nhận 1 lần/tháng).`); }, () => setRemindConfirm(false))}
        message="Gửi thông báo “Học phí tháng này” tới mọi học viên đang học (kèm số nợ nếu có). Bình thường hệ thống tự gửi lúc 08:00 ngày 1 hằng tháng." />
      <SimModal bill={simTarget} onClose={() => setSimTarget(null)} act={act} onRun={(body) => run(() => financeApi.simulate(body), () => setSimTarget(null))} />
    </div>
  );
}

/** Giả lập webhook ngân hàng để demo khi chưa nối SePay/Casso. */
function SimModal({ bill, onClose, act, onRun }) {
  const [amount, setAmount] = useState('');
  useEffect(() => { if (bill) setAmount(String(bill.amount - bill.paid)); }, [bill]);
  return (
    <Modal isOpen={!!bill} title="Giả lập chuyển khoản (demo)" icon={Zap} onClose={onClose}>
      {bill && (
        <div className="space-y-4">
          <p className="text-sm text-slate-600">Mô phỏng ngân hàng báo có tiền vào với nội dung <span className="font-mono font-bold">{bill.transferContent}</span>. Khoản thu sẽ chuyển sang “Đã đóng” nếu đủ tiền.</p>
          <div><label className={labelCls}>Số tiền (VNĐ)</label><input type="number" className={inputCls} value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
          <Err text={act.error} />
          <div className="flex justify-end gap-2"><button className={btnGhost} onClick={onClose}>Hủy</button>
            <button className={btnPrimary} disabled={act.loading || !amount} onClick={() => onRun({ transferContent: bill.transferContent, amount: Number(amount) })}>{act.loading ? 'Đang xử lý...' : 'Giả lập thanh toán'}</button></div>
        </div>
      )}
    </Modal>
  );
}
