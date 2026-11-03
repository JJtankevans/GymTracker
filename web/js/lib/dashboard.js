// Contas da Home (mesmas do DashboardCalculator do app): semana de segunda a domingo, sequência de dias e de semanas.
// Um "dia" é um número inteiro (dias desde 1970 no fuso local): evita erro de fuso e de horário de verão.

export const dayOf = (date) => Math.floor((Date.UTC(date.getFullYear(), date.getMonth(), date.getDate())) / 86400000);
export const dayOfIso = (iso) => dayOf(new Date(iso));
export const dateOfDay = (day) => new Date(day * 86400000 + new Date().getTimezoneOffset() * 0); // UTC meia-noite do dia
/** 1970-01-01 foi quinta-feira: segunda = 0. */
export const weekdayOf = (day) => (((day + 3) % 7) + 7) % 7;
export const weekStart = (day) => day - weekdayOf(day);

export function weeklyCount(days, today) {
  const s = weekStart(today);
  return days.filter((d) => d >= s && d <= s + 6).length;
}

/** Dias seguidos com treino terminando hoje ou ontem. */
export function streak(days, today) {
  const u = [...new Set(days)].sort((a, b) => b - a);
  if (!u.length || u[0] < today - 1 || u[0] > today) return 0;
  let n = 1;
  let expected = u[0] - 1;
  for (const d of u.slice(1)) {
    if (d === expected) { n++; expected--; } else if (d < expected) break;
  }
  return n;
}

/** Semanas seguidas com pelo menos `goal` treinos; a semana atual só conta se já bateu. */
export function weekStreak(days, today, goal) {
  const need = Math.max(1, goal || 1);
  const counts = new Map();
  for (const d of days) counts.set(weekStart(d), (counts.get(weekStart(d)) || 0) + 1);
  const cur = weekStart(today);
  let w = (counts.get(cur) || 0) >= need ? cur : cur - 7;
  let n = 0;
  while ((counts.get(w) || 0) >= need) { n++; w -= 7; }
  return n;
}

/** Últimas `n` semanas (a mais antiga primeiro) com a contagem de treinos. */
export function weeklyBuckets(days, today, n = 8) {
  const cur = weekStart(today);
  return Array.from({ length: n }, (_, i) => {
    const s = cur - 7 * (n - 1 - i);
    return { start: s, count: days.filter((d) => d >= s && d <= s + 6).length };
  });
}
