// Histórico de treinos (filtro por grupo) e detalhe de um treino.
import * as api from "../api.js";
import { S, getHistory, groupById, exerciseById, invalidateHistory } from "../state.js";
import { h, toast, confirmDialog } from "../lib/dom.js";
import { fmtDateTime, fmtKg, fmtMinutes } from "../lib/format.js";

const dur = (w) => w.finished_at ? Math.max(0, Math.round((new Date(w.finished_at) - new Date(w.started_at)) / 60000)) : 0;

export default async function history(root, params, { go }) {
  if (params[0]) return detail(root, params[0], go);
  const all = await getHistory(true);
  let filter = null;
  const chips = h("div", { class: "row", style: { overflowX: "auto", gap: "8px", scrollbarWidth: "none" } });
  const list = h("div", { class: "stack" });
  root.append(h("h1", {}, "Histórico"), chips, list);
  const paint = () => {
    chips.replaceChildren(h("button", { class: "chip" + (filter ? "" : " on"), onclick: () => { filter = null; paint(); } }, "Todos"),
      ...S.groups.map((g) => h("button", { class: "chip" + (filter === g.id ? " on" : ""), onclick: () => { filter = g.id; paint(); } }, g.name)));
    const rows = all.filter((w) => !filter || w.muscle_group_id === filter);
    list.replaceChildren(...(rows.length ? rows.map((w) => {
      const g = groupById(w.muscle_group_id);
      return h("button", { class: "card tap row", style: { width: "100%", textAlign: "left" }, onclick: () => go("historico/" + w.id) },
        h("span", { class: "dot", style: { background: g?.color_hex || "var(--muted)" } }),
        h("div", { class: "grow" }, h("h3", {}, g?.name || "Treino"), h("div", { class: "muted small" }, fmtDateTime(w.started_at))),
        h("span", { class: "pill" }, fmtMinutes(dur(w))));
    }) : [h("div", { class: "empty" }, all.length ? "Nenhum treino nesse grupo." : "Você ainda não terminou nenhum treino.")]));
  };
  paint();
}

async function detail(root, id, go) {
  const [w, sets] = await Promise.all([api.workout(id), api.setsForWorkout(id)]);
  const g = groupById(w.muscle_group_id);
  const byEx = new Map();
  for (const s of sets) { if (!byEx.has(s.exercise_id)) byEx.set(s.exercise_id, []); byEx.get(s.exercise_id).push(s); }
  const vol = sets.reduce((a, s) => a + s.weight_kg * s.reps, 0);
  root.append(
    h("a", { href: "#/historico", class: "small" }, "‹ Histórico"),
    h("div", {}, h("h1", {}, g?.name || "Treino"), h("p", { class: "muted" }, fmtDateTime(w.started_at))),
    h("div", { class: "card row between center" },
      ...[["Duração", fmtMinutes(dur(w))], ["Séries", sets.length], ["Volume", `${Math.round(vol)} kg`]].map(([l, v]) => h("div", { class: "grow" }, h("div", { class: "bold", style: { fontSize: "20px" } }, v), h("div", { class: "tiny muted" }, l)))),
    w.notes ? h("div", { class: "card" }, h("div", { class: "tiny muted" }, "Anotação"), h("p", {}, w.notes)) : null,
    ...[...byEx].map(([exId, list]) => h("div", { class: "card stack" }, h("h3", {}, exerciseById(exId)?.name || "Exercício"),
      h("div", { class: "row wrap" }, list.map((s, i) => h("span", { class: "set", style: { cursor: "default" } }, `${i + 1}. ${fmtKg(s.weight_kg)} kg × ${s.reps}`))))),
    h("button", { class: "btn danger block", onclick: async () => {
      if (!(await confirmDialog({ title: "Apagar este treino?", text: "Não dá para desfazer.", ok: "Apagar", danger: true }))) return;
      try { await api.discardWorkout(id); invalidateHistory(); toast("Treino apagado", "ok"); go("historico"); } catch (e) { toast(e.message, "err"); }
    } }, "Apagar treino"));
}
