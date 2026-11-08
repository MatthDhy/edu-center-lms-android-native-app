const pad = (n) => String(n).padStart(2, '0');

/** "2026-11" theo giờ máy (không dùng toISOString vì nó đổi sang UTC). */
export function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}

/** 1500000 -> "1,5tr" ; 250000 -> "250k" */
export function shortMoney(v) {
  const n = Number(v || 0);
  if (n >= 1e9) return `${(n / 1e9).toFixed(1).replace('.', ',')}tỷ`;
  if (n >= 1e6) return `${(n / 1e6).toFixed(1).replace('.', ',').replace(/,0$/, '')}tr`;
  if (n >= 1e3) return `${Math.round(n / 1e3)}k`;
  return String(n);
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
