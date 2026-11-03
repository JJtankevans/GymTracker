// Mais: perfil, tema, descanso padrão, exportar CSV, grupos, recordes, sair e excluir conta.
import * as api from "../api.js";
import { S, THEMES, applyTheme, savedTheme, ls, getHistory, groupById, exerciseById } from "../state.js";
import { h, toast, confirmDialog, promptDialog } from "../lib/dom.js";
import { logout } from "../main.js";
import { initial } from "../lib/format.js";

/** Células que começam com = + - @ viram texto (evita fórmula ao abrir no Excel). */
const cell = (v) => {
  let s = v == null ? "" : String(v);
  if (/^[=+\-@\t\r]/.test(s)) s = "'" + s;
  return /[",;\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
};

async function exportCsv() {
  const [hist, sets] = await Promise.all([getHistory(true), api.allSets()]);
  const wk = new Map(hist.map((w) => [w.id, w]));
  const rows = [["data", "grupo", "exercicio", "serie", "carga_kg", "repeticoes"]];
  for (const s of sets) {
    const w = wk.get(s.workout_id);
    if (!w) continue;
    rows.push([w.started_at.slice(0, 10), groupById(w.muscle_group_id)?.name, exerciseById(s.exercise_id)?.name, s.set_number, s.weight_kg, s.reps]);
  }
  const blob = new Blob(["﻿" + rows.map((r) => r.map(cell).join(";")).join("\r\n")], { type: "text/csv;charset=utf-8" });
  const a = h("a", { href: URL.createObjectURL(blob), download: "gymtracker-treinos.csv" });
  document.body.append(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 4000);
  toast(`${rows.length - 1} séries exportadas`, "ok");
}

export default function more(root, _p, { go }) {
  const name = S.profile?.username || S.email;
  const rest = parseInt(ls.get("gt.rest", "90"), 10) || 90;
  const row = (label, sub, onclick, cls = "") => h("button", { class: "card tap row " + cls, style: { width: "100%", textAlign: "left" }, onclick }, h("div", { class: "grow" }, h("h3", {}, label), sub ? h("div", { class: "muted small" }, sub) : null), h("span", { class: "muted" }, "›"));
  const themeRow = h("div", { class: "row wrap" });
  const paintThemes = () => themeRow.replaceChildren(...THEMES.map((t) => h("button", { class: "chip" + (savedTheme() === t.key ? " on" : ""), onclick: () => { applyTheme(t.key); paintThemes(); } }, h("span", { class: "dot", style: { background: t.color, display: "inline-block", marginRight: "6px" } }), t.label)));
  paintThemes();

  root.append(
    h("div", { class: "row" }, h("div", { class: "av", style: { width: "56px", height: "56px", fontSize: "22px" } }, S.profile?.avatar_url ? h("img", { src: S.profile.avatar_url, alt: "" }) : initial(name)),
      h("div", { class: "grow" }, h("h2", {}, name), h("div", { class: "muted small" }, S.email))),
    row("Recordes", "Sua melhor carga em cada exercício", () => go("recordes")),
    row("Grupos e exercícios", `${S.groups.length} grupos · ${S.exercises.length} exercícios`, () => go("grupos")),
    h("div", { class: "card stack" }, h("h3", {}, "Tema"), themeRow),
    row("Descanso padrão", `${rest} segundos entre as séries`, async () => {
      const v = await promptDialog({ title: "Descanso padrão", label: "Segundos entre as séries", value: String(rest), inputMode: "numeric", type: "number" });
      const n = parseInt(v, 10);
      if (v == null) return;
      if (!(n >= 10 && n <= 600)) return toast("Use de 10 a 600 segundos.", "err");
      ls.set("gt.rest", String(n)); go("mais");
    }),
    row("Exportar treinos (CSV)", "Abre no Excel ou Planilhas", async () => { try { await exportCsv(); } catch (e) { toast(e.message, "err"); } }),
    row("Sair da conta", null, async () => { if (await confirmDialog({ title: "Sair da conta?", ok: "Sair" })) logout(); }),
    row("Excluir minha conta", "Apaga a conta e todos os dados, sem volta", async () => {
      if (!(await confirmDialog({ title: "Excluir conta", text: "Todos os seus treinos, grupos e recordes serão apagados para sempre.", ok: "Excluir tudo", danger: true, typed: "EXCLUIR" }))) return;
      try { await api.deleteAccount(); ls.del("gt.theme"); await logout(); toast("Conta excluída.", "ok"); } catch (e) { toast(e.message, "err"); }
    }, "danger"),
    h("p", { class: "center small muted" }, "GymTracker web · no iPhone: Compartilhar › Adicionar à Tela de Início"));
}
