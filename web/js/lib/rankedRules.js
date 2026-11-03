// Regras da ranqueada (as mesmas de shared/.../presentation/ranked/RankedRules.kt e BoardAnalysis.kt).
// O banco decide de verdade (migração 018); aqui é só para mostrar na tela ("faltam 40 pontos") e testar.

export const TIERS = [
  { key: "IRON", label: "Ferro", color: "#8A8F98" },
  { key: "BRONZE", label: "Bronze", color: "#CD7F32" },
  { key: "SILVER", label: "Prata", color: "#C9D1D9" },
  { key: "GOLD", label: "Ouro", color: "#FFC53D" },
  { key: "PLATINUM", label: "Platina", color: "#3EE0C5" },
  { key: "DIAMOND", label: "Diamante", color: "#5CA8FF" },
  { key: "LEGEND", label: "Lenda", color: "#FF3D6E" },
];
export const RP_PER_DIVISION = 100;
export const RP_PER_TIER = 300;
export const LEGEND_RP = 1800;
const ROMAN = { 1: "I", 2: "II", 3: "III" };

export function rankFor(rp) {
  const safe = Math.max(0, Math.floor(rp || 0));
  if (safe >= LEGEND_RP) {
    return { tier: 6, ...TIERS[6], division: 0, rp: safe, isTop: true, label: "Lenda", rpInDivision: safe - LEGEND_RP, progress: 1, rpToNext: 0 };
  }
  const tier = Math.floor(safe / RP_PER_TIER);
  const division = 3 - Math.floor((safe % RP_PER_TIER) / RP_PER_DIVISION);
  const rpInDivision = safe % RP_PER_DIVISION;
  return {
    tier, ...TIERS[tier], division, rp: safe, isTop: false,
    label: `${TIERS[tier].label} ${ROMAN[division]}`,
    rpInDivision, progress: rpInDivision / RP_PER_DIVISION, rpToNext: RP_PER_DIVISION - rpInDivision,
  };
}

export const BASE_POINTS = 100;
export const MIN_COHORT_FOR_PERCENTILES = 5;
export const promoteCount = (size) => (size < MIN_COHORT_FOR_PERCENTILES ? 0 : Math.floor((size * 20 + 99) / 100));
export const demoteCount = (size) => (size < MIN_COHORT_FOR_PERCENTILES ? 0 : Math.floor((size * 20 + 99) / 100));

export function zoneFor(position, size, points) {
  if (points <= 0) return "DEMOTE";
  if (position <= promoteCount(size)) return "PROMOTE";
  if (size >= MIN_COHORT_FOR_PERCENTILES && position > size - demoteCount(size)) return "DEMOTE";
  return "SAFE";
}

export function settleDelta(position, size, points) {
  if (points <= 0) return -35;
  if (size < MIN_COHORT_FOR_PERCENTILES) return points >= BASE_POINTS ? 10 : -20;
  if (position === 1) return 40;
  if (position <= promoteCount(size)) return 30;
  if (position > size - demoteCount(size)) return -25;
  if (position <= Math.floor((size + 1) / 2)) return 10;
  return -10;
}

export function duelDelta(rpSelf, rpOpp, ptSelf, ptOpp, ranked = true) {
  if (!ranked || (ptSelf === 0 && ptOpp === 0)) return 0;
  const score = ptSelf > ptOpp ? 1 : ptSelf < ptOpp ? 0 : 0.5;
  const expected = 1 / (1 + Math.pow(10, (rpOpp - rpSelf) / 400));
  const raw = 24 * (score - expected);
  return Math.min(35, Math.max(-35, Math.floor(raw + 0.5)));
}

export const canDuel = (rpA, rpB) => rankFor(rpA).tier === rankFor(rpB).tier;

export const SEASON_WEEKS = 8;
/** Segunda-feira em que a temporada 1 começou: 26/10/2026, em dias desde 1970. */
export const SEASON_EPOCH_DAY = Math.floor(Date.UTC(2026, 9, 26) / 86400000);
export function seasonOf(weekStartDay) {
  const weeks = Math.max(0, Math.floor((weekStartDay - SEASON_EPOCH_DAY) / 7));
  return { number: Math.floor(weeks / SEASON_WEEKS) + 1, weekInSeason: (weeks % SEASON_WEEKS) + 1, isLastWeek: weeks % SEASON_WEEKS === SEASON_WEEKS - 1 };
}

/** Onde estou na turma e o que falta para mudar de faixa (BoardAnalysis.kt). */
export function analyzeBoard(points, myPoints) {
  const size = points.length;
  const position = 1 + points.filter((p) => p > myPoints).length;
  const promote = promoteCount(size);
  const demote = demoteCount(size);
  const zone = zoneFor(position, size, myPoints);
  const others = [...points];
  const i = others.indexOf(myPoints);
  if (i >= 0) others.splice(i, 1);
  others.sort((a, b) => b - a);
  let toPromotion = null;
  if (promote > 0 && zone !== "PROMOTE" && others[promote - 1] !== undefined) {
    toPromotion = Math.max(myPoints <= 0 ? BASE_POINTS : 1, others[promote - 1] - myPoints);
  }
  let toSafety = null;
  if (zone === "DEMOTE") {
    const safeSpot = size - demote;
    const byPosition = size >= MIN_COHORT_FOR_PERCENTILES && safeSpot >= 1 ? (others[safeSpot - 1] ?? 0) - myPoints : 0;
    toSafety = myPoints <= 0 ? Math.max(BASE_POINTS, byPosition) : Math.max(1, byPosition);
  }
  return { size, position, myPoints, zone, promote, demote, projectedDelta: settleDelta(position, size, myPoints), toPromotion, toSafety };
}

/** Dias que faltam para fechar a semana, contando hoje (segunda = 7, domingo = 1). */
export const weekDaysLeft = (weekdayMon0) => 7 - weekdayMon0;
