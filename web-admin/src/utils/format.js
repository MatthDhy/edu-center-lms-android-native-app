export const WEEKDAYS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];

const pad = (n) => String(n).padStart(2, '0');

export function fmtDateTime(s) {
  if (!s) return '—';
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getHours())}:${pad(d.getMinutes())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export function fmtDate(s) {
  if (!s) return '—';
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export function fmtTime(s) {
  if (!s) return '';
  const d = new Date(s);
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function fmtMoney(n) {
  return `${Number(n ?? 0).toLocaleString('vi-VN')} đ`;
}

/** Date -> "2026-11-02" theo giờ máy (không dùng toISOString vì nó đổi sang UTC). */
export function toIsoDate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** "2026-11-02T18:00:00" -> "2026-11-02T18:00" (giá trị của <input type="datetime-local">). */
export function toInputDateTime(s) {
  return s ? s.slice(0, 16) : '';
}

export function mondayOf(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const dow = (d.getDay() + 6) % 7; // Thứ 2 = 0
  d.setDate(d.getDate() - dow);
  return d;
}

export function addDays(date, n) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  d.setDate(d.getDate() + n);
  return d;
}
