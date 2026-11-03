// Formatação em português (vírgula decimal, datas curtas, durações).

export const pad2 = (n) => String(n).padStart(2, "0");

/** 40 -> "40", 42.5 -> "42,5" */
export function fmtKg(n) {
  const v = Math.round((Number(n) || 0) * 100) / 100;
  return Number.isInteger(v) ? String(v) : String(v).replace(".", ",");
}

/** "42,5" ou "42.5" -> 42.5; vazio/inválido -> NaN */
export function parseNum(s) {
  return parseFloat(String(s).replace(",", ".").trim());
}

/** 3725 -> "1 h 02 min"; 45*60 -> "45 min"; 20 -> "0 min" */
export function fmtMinutes(totalMin) {
  const m = Math.max(0, Math.round(totalMin));
  const h = Math.floor(m / 60);
  const r = m % 60;
  if (h === 0) return `${m} min`;
  return r === 0 ? `${h} h` : `${h} h ${pad2(r)} min`;
}

/** 83 -> "01:23" */
export function fmtClock(sec) {
  const s = Math.max(0, Math.floor(sec));
  return `${pad2(Math.floor(s / 60))}:${pad2(s % 60)}`;
}

const DIAS = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];

/** ISO -> "ter, 03/11 · 18:40" */
export function fmtDateTime(iso) {
  const d = new Date(iso);
  if (isNaN(d)) return "";
  return `${DIAS[d.getDay()]}, ${pad2(d.getDate())}/${pad2(d.getMonth() + 1)} · ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

export function fmtDay(iso) {
  const d = new Date(iso);
  if (isNaN(d)) return "";
  return `${DIAS[d.getDay()]}, ${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`;
}

/** Minutos desde um instante (ISO). */
export function minutesSince(iso, now = Date.now()) {
  return Math.max(0, Math.floor((now - new Date(iso).getTime()) / 60000));
}

/** "agora", "5 min", "2 h", "3 dias" */
export function ago(iso, now = Date.now()) {
  const m = minutesSince(iso, now);
  if (m < 1) return "agora";
  if (m < 60) return `${m} min`;
  if (m < 1440) return `${Math.floor(m / 60)} h`;
  const d = Math.floor(m / 1440);
  return d === 1 ? "1 dia" : `${d} dias`;
}

export const initial = (name) => (String(name || "?").trim()[0] || "?").toUpperCase();

/** Escapa texto para usar dentro de string HTML (só para o pouco SVG montado por string). */
export const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
