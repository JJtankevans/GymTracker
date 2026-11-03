// Estado compartilhado do app web (quem está logado, grupos, tema) e preferências guardadas no aparelho.
import * as api from "./api.js";

export const S = { uid: null, email: null, profile: null, groups: [], exercises: [] };

const ls = {
  get(k, d = null) { try { const v = localStorage.getItem(k); return v == null ? d : v; } catch { return d; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch { /* modo privado: ignora */ } },
  del(k) { try { localStorage.removeItem(k); } catch { /* ignora */ } },
};
export { ls };

export const THEMES = [
  { key: "", label: "Lima", color: "#C6F432" },
  { key: "oceano", label: "Oceano", color: "#38BDF8" },
  { key: "sakura", label: "Sakura", color: "#FF8FB8" },
  { key: "ouro", label: "Ouro", color: "#D8B25A" },
  { key: "papel", label: "Papel", color: "#B3261E" },
];
export function applyTheme(key) {
  const t = THEMES.some((x) => x.key === key) ? key : "";
  if (t) document.documentElement.dataset.theme = t; else delete document.documentElement.dataset.theme;
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.content = t === "papel" ? "#F2ECDF" : getComputedStyle(document.documentElement).getPropertyValue("--bg").trim() || "#0C0D0F";
  ls.set("gt.theme", t);
}
export const savedTheme = () => ls.get("gt.theme", "");

export async function loadBase() {
  const [profile, groups, exercises] = await Promise.all([api.profile(S.uid), api.muscleGroups(), api.exercises()]);
  S.profile = profile; S.groups = groups; S.exercises = exercises;
}
export async function reloadCatalog() {
  const [groups, exercises] = await Promise.all([api.muscleGroups(), api.exercises()]);
  S.groups = groups; S.exercises = exercises;
}
export const groupById = (id) => S.groups.find((g) => g.id === id);
export const exercisesOf = (groupId) => S.exercises.filter((e) => e.groupIds.includes(groupId));
export const exerciseById = (id) => S.exercises.find((e) => e.id === id);

// Histórico em memória (é o que mais pesa); as telas que mudam treinos chamam invalidateHistory().
let hist = null;
export async function getHistory(force = false) {
  if (!hist || force) hist = await api.history();
  return hist;
}
export const invalidateHistory = () => { hist = null; };
