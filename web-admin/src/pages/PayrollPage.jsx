import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, Calculator, CheckCircle2, Banknote, Eye, Save } from 'lucide-react';
import { payrollApi } from '../api/client';
import StatCard from '../components/StatCard';
import Modal from '../components/Modal';
import ConfirmModal from '../components/ConfirmModal';
import { fmtMoney, fmtDate } from '../utils/format';
import { currentMonth } from '../utils/month';
import { cardCls, inputCls, btnPrimary } from '../components/ui';

export default function PayrollPage() {
  const [month, setMonth] = useState(currentMonth());
  const [period, setPeriod] = useState(null);
  const [rates, setRates] = useState([]);
  const [edit, setEdit] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [items, setItems] = useState(null);         // { teacher, list }
  const [approveOpen, setApproveOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [p, r] = await Promise.all([payrollApi.view(month), payrollApi.rates()]);
      setPeriod(p.data); setRates(r.data || []);
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  }, [month]);
  useEffect(() => { load(); }, [load]);

  const saveRate = async (t) => {
    setError(''); setMsg('');
    try { await payrollApi.setRate(t.teacherId, Number(edit[t.teacherId])); setMsg(`Đã lưu đơn giá cho ${t.fullName}`); setEdit((e) => ({ ...e, [t.teacherId]: undefined })); load(); }
    catch (e) { setError(e.message); }
  };
  const generate = async () => {
    setError(''); setMsg('');
    try {
      const r = (await payrollApi.generate(month)).data;
      setMsg(`Đã tính ${r.items} buổi dạy.${r.skippedNoRate > 0 ? ` ${r.skippedNoRate} buổi bị bỏ qua vì giảng viên chưa có đơn giá.` : ''}`);
      load();
    } catch (e) { setError(e.message); }
  };
  const approve = async () => {
    try { await payrollApi.approve(period.periodId); setApproveOpen(false); setMsg('Đã duyệt bảng lương, giảng viên đã nhận thông báo.'); load(); }
    catch (e) { setError(e.message); setApproveOpen(false); }
  };
  const showItems = async (t) => {
    try { setItems({ teacher: t, list: (await payrollApi.items(month, t.teacherId)).data || [] }); } catch (e) { setError(e.message); }
  };

  const approved = period?.status === 'APPROVED';
  const hours = (period?.teachers || []).reduce((s, t) => s + t.hours, 0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <input type="month" className={`${inputCls} w-44`} value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} />
        <button onClick={load} className="p-2.5 rounded-xl border border-slate-200 bg-white text-slate-500 hover:bg-slate-50"><RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /></button>
        <div className="ml-auto flex gap-2">
          <button onClick={generate} disabled={approved} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-brand-200 bg-white text-brand-700 hover:bg-brand-50 font-semibold text-sm disabled:opacity-40"><Calculator className="w-4 h-4" /> Tính lương tháng</button>
          <button onClick={() => setApproveOpen(true)} disabled={!period?.periodId || approved || period.teachers.length === 0} className={btnPrimary}><CheckCircle2 className="w-4 h-4" /> Duyệt bảng lương</button>
        </div>
      </div>
      {msg && <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">{msg}</div>}
      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <StatCard label={`Tổng lương tháng ${month}`} value={fmtMoney(period?.total ?? 0)} icon={Banknote} tone="emerald" />
        <StatCard label="Giảng viên có ca dạy" value={period?.teachers.length ?? 0} icon={Calculator} tone="blue" />
        <StatCard label="Tổng giờ dạy" value={Math.round(hours * 10) / 10} hint={approved ? 'ĐÃ DUYỆT' : period?.status === 'DRAFT' ? 'NHÁP' : 'chưa tính'} icon={CheckCircle2} tone={approved ? 'emerald' : 'amber'} />
      </div>

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 border-b border-slate-100"><h3 className="text-lg font-bold text-ink">Bảng lương theo giảng viên</h3>
          <p className="text-xs text-slate-500 mt-0.5">Chỉ tính các buổi đã kết thúc, không hủy và có điểm danh. Lương = số giờ × đơn giá/giờ.</p></div>
        {!period || period.teachers.length === 0 ? <div className="py-14 text-center text-sm text-slate-400">Chưa có dữ liệu. Đặt đơn giá cho giảng viên rồi bấm “Tính lương tháng”.</div> : (
          <table className="w-full text-sm"><thead><tr className="text-left border-b border-slate-100"><th className="px-5 py-3.5">Giảng viên</th><th className="px-5 py-3.5 text-right">Số buổi</th><th className="px-5 py-3.5 text-right">Số giờ</th><th className="px-5 py-3.5 text-right">Thành tiền</th><th className="px-5 py-3.5 text-right">Chi tiết</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{period.teachers.map((t) => (
              <tr key={t.teacherId}><td className="px-5 py-3.5 font-bold text-ink">{t.fullName}</td><td className="px-5 py-3.5 text-right tabular-nums">{t.lessons}</td><td className="px-5 py-3.5 text-right tabular-nums">{t.hours}</td><td className="px-5 py-3.5 text-right font-extrabold tabular-nums text-brand-700">{fmtMoney(t.amount)}</td>
                <td className="px-5 py-3.5 text-right"><button onClick={() => showItems(t)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100"><Eye className="w-4 h-4" /></button></td></tr>))}</tbody></table>
        )}
      </div>

      <div className={`${cardCls} overflow-hidden`}>
        <div className="p-5 border-b border-slate-100"><h3 className="text-lg font-bold text-ink">Đơn giá giảng viên (VNĐ / giờ)</h3>
          <p className="text-xs text-slate-500 mt-0.5">Đơn giá mới áp dụng từ hôm nay; các buổi dạy trước đó giữ đơn giá cũ.</p></div>
        <table className="w-full text-sm"><tbody className="divide-y divide-slate-100">{rates.map((t) => (
          <tr key={t.teacherId}><td className="px-5 py-3"><p className="font-bold text-ink">{t.fullName}</p><p className="text-xs text-slate-400">{t.email}</p></td>
            <td className="px-5 py-3 w-56"><input type="number" min="0" step="1000" className={inputCls} placeholder={t.hourlyRate == null ? 'Chưa có đơn giá' : String(t.hourlyRate)} value={edit[t.teacherId] ?? ''} onChange={(e) => setEdit((x) => ({ ...x, [t.teacherId]: e.target.value }))} /></td>
            <td className="px-5 py-3 text-xs text-slate-500">Hiện tại: <b>{t.hourlyRate == null ? '—' : fmtMoney(t.hourlyRate)}</b></td>
            <td className="px-5 py-3 text-right"><button disabled={!edit[t.teacherId]} onClick={() => saveRate(t)} className={btnPrimary}><Save className="w-4 h-4" /> Lưu</button></td></tr>))}
          {rates.length === 0 && <tr><td className="py-10 text-center text-slate-400">Chưa có giảng viên.</td></tr>}</tbody></table>
      </div>

      <Modal isOpen={!!items} title={items ? `Chi tiết lương: ${items.teacher.fullName}` : ''} icon={Banknote} onClose={() => setItems(null)} maxWidth="max-w-3xl">
        {items && <div className="overflow-x-auto rounded-xl border border-slate-200"><table className="w-full text-sm">
          <thead><tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase"><th className="px-4 py-3">Ngày</th><th className="px-4 py-3">Buổi dạy</th><th className="px-4 py-3 text-right">Giờ</th><th className="px-4 py-3 text-right">Đơn giá</th><th className="px-4 py-3 text-right">Thành tiền</th></tr></thead>
          <tbody className="divide-y divide-slate-100">{items.list.map((i) => (<tr key={i.id}><td className="px-4 py-2.5">{fmtDate(i.workDate)}</td><td className="px-4 py-2.5">{i.description}</td><td className="px-4 py-2.5 text-right tabular-nums">{i.hours}</td><td className="px-4 py-2.5 text-right tabular-nums">{fmtMoney(i.rate)}</td><td className="px-4 py-2.5 text-right font-bold tabular-nums">{fmtMoney(i.amount)}</td></tr>))}</tbody></table></div>}
      </Modal>
      <ConfirmModal isOpen={approveOpen} title="Duyệt bảng lương" confirmText="Duyệt" onClose={() => setApproveOpen(false)} onConfirm={approve}
        message={<>Duyệt bảng lương tháng <b>{month}</b> (tổng <b>{fmtMoney(period?.total ?? 0)}</b>)? Sau khi duyệt không thể tính lại và giảng viên sẽ xem được phiếu lương.</>} />
    </div>
  );
}
