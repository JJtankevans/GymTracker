// Recordes pessoais por exercício (função records_summary do banco).
import * as api from "../api.js";
import { h } from "../lib/dom.js";
import { fmtKg } from "../lib/format.js";

export default async function records(root) {
  const rows = (await api.records()) || [];
  let q = "";
  const list = h("div", { class: "stack" });
  const search = h("input", { class: "input", type: "search", placeholder: "Buscar exercício", oninput: () => { q = search.value.toLowerCase(); paint(); } });
  root.append(h("a", { href: "#/mais", class: "small" }, "‹ Mais"), h("h1", {}, "Recordes"), search, list);
  const paint = () => {
    const f = rows.filter((r) => r.exercise_name.toLowerCase().includes(q)).sort((a, b) => b.best_weight_kg - a.best_weight_kg);
    list.replaceChildren(...(f.length ? f.map((r) => {
      const up = r.previous_best_kg != null ? r.best_weight_kg - r.previous_best_kg : null;
      return h("div", { class: "card row" }, h("div", { class: "grow" }, h("h3", {}, r.exercise_name), h("div", { class: "muted small" }, `${r.sessions} treino${r.sessions === 1 ? "" : "s"} · 1RM estimado ${fmtKg(Math.round(r.best_one_rm_kg))} kg`)),
        h("div", { style: { textAlign: "right" } }, h("div", { class: "bold accent", style: { fontSize: "22px" } }, `${fmtKg(r.best_weight_kg)} kg`), up > 0 ? h("div", { class: "ok small" }, `+${fmtKg(up)} kg`) : null));
    }) : [h("div", { class: "empty" }, rows.length ? "Nada encontrado." : "Seus recordes aparecem aqui depois do primeiro treino.")]));
  };
  paint();
}
