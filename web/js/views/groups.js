// Grupos de treino e seus exercícios (criar, renomear, apagar, adicionar do catálogo ou novo).
import * as api from "../api.js";
import { S, reloadCatalog, exercisesOf, invalidateHistory } from "../state.js";
import { h, toast, openSheet, confirmDialog, mount } from "../lib/dom.js";

const COLORS = ["#C6F432", "#38BDF8", "#FF8FB8", "#FFC53D", "#FF5A5F", "#A78BFA", "#3EE0C5", "#FB923C"];
const norm = (s) => String(s).normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();

export default async function groups(root, params, { go }) {
  if (params[0]) return groupDetail(root, params[0], go);
  root.append(h("a", { href: "#/mais", class: "small" }, "‹ Mais"), h("div", { class: "row between" }, h("h1", {}, "Grupos"), h("button", { class: "btn primary small", onclick: () => groupSheet(null, go) }, "+ Novo")));
  root.append(h("div", { class: "stack" }, S.groups.length ? S.groups.map((g) => h("button", { class: "card tap row", style: { width: "100%", textAlign: "left" }, onclick: () => go("grupos/" + g.id) },
    h("span", { class: "dot", style: { background: g.color_hex } }), h("div", { class: "grow" }, h("h3", {}, g.name), h("div", { class: "muted small" }, `${exercisesOf(g.id).length} exercícios`)), h("span", { class: "muted" }, "›")) ) : [h("div", { class: "card empty" }, "Nenhum grupo ainda. Crie o primeiro (ex.: Peito e tríceps).")]));
}

function groupSheet(g, go) {
  openSheet((close) => {
    let color = g?.color_hex || COLORS[0];
    const name = h("input", { class: "input", value: g?.name || "", placeholder: "Ex.: Peito e tríceps", maxLength: 40 });
    const sw = h("div", { class: "row wrap" });
    const paint = () => mount(sw, ...COLORS.map((c) => h("button", { class: "iconbtn", "aria-label": c, style: { background: c, outline: c === color ? "3px solid var(--text)" : "none" }, onclick: () => { color = c; paint(); } })));
    paint();
    return h("div", { class: "stack" }, h("h2", {}, g ? "Editar grupo" : "Novo grupo"), h("div", { class: "field" }, h("label", {}, "Nome"), name), sw,
      h("button", { class: "btn primary block", onclick: async () => {
        const n = name.value.trim();
        if (n.length < 2) return toast("Dê um nome ao grupo.", "err");
        try {
          if (g) await api.renameGroup(g.id, n, color); else await api.createGroup(S.uid, n, color);
          await reloadCatalog(); close(); go(g ? "grupos/" + g.id : "grupos");
        } catch (e) { toast(e.message, "err"); }
      } }, "Salvar"));
  });
}

async function groupDetail(root, id, go) {
  const g = S.groups.find((x) => x.id === id);
  if (!g) return go("grupos");
  const catalog = await fetch("catalog.json").then((r) => r.json()).catch(() => ({ exercises: [], muscles: [] }));
  const list = h("div", { class: "stack" });
  const paint = () => {
    const exs = exercisesOf(id);
    mount(list, ...(exs.length ? exs.map((e) => h("div", { class: "card row" }, h("div", { class: "grow" }, h("h3", {}, e.name), h("div", { class: "muted small" }, `${e.default_sets}×${e.default_reps}`)),
      h("button", { class: "btn small danger", onclick: async () => {
        if (!(await confirmDialog({ title: `Tirar "${e.name}" do grupo?`, text: "O histórico e os recordes continuam salvos.", ok: "Tirar" }))) return;
        try { await api.unlinkExercise(e.id, id); await reloadCatalog(); paint(); } catch (err) { toast(err.message, "err"); }
      } }, "Tirar"))) : [h("div", { class: "empty" }, "Sem exercícios. Adicione abaixo.")]));
  };
  root.append(h("a", { href: "#/grupos", class: "small" }, "‹ Grupos"),
    h("div", { class: "row" }, h("span", { class: "dot", style: { background: g.color_hex } }), h("h1", { class: "grow" }, g.name), h("button", { class: "btn small", onclick: () => groupSheet(g, go) }, "Editar")),
    list,
    h("button", { class: "btn primary block", onclick: () => addSheet() }, "+ Adicionar exercício"),
    h("button", { class: "btn danger block", onclick: async () => {
      if (!(await confirmDialog({ title: `Apagar "${g.name}"?`, text: "Os treinos já feitos nesse grupo continuam no histórico.", ok: "Apagar grupo", danger: true }))) return;
      try { await api.deleteGroup(id); invalidateHistory(); await reloadCatalog(); toast("Grupo apagado", "ok"); go("grupos"); } catch (e) { toast(e.message, "err"); }
    } }, "Apagar grupo"));
  paint();

  function addSheet() {
    openSheet((close) => {
      const q = h("input", { class: "input", type: "search", placeholder: "Buscar ou digitar um novo" });
      const res = h("div", { class: "stack" });
      const have = new Set(exercisesOf(id).map((e) => norm(e.name)));
      const add = async (name, extra = {}) => {
        try {
          let ex = S.exercises.find((e) => norm(e.name) === norm(name));
          if (ex) await api.linkExercise(ex.id, [id]);
          else await api.createExercise(S.uid, { name, sets: extra.sets || 3, reps: extra.reps || 12, groupIds: [id] });
          await reloadCatalog(); close(); paint();
        } catch (e) { toast(e.message, "err"); }
      };
      const show = () => {
        const t = norm(q.value);
        const pool = [...S.exercises.map((e) => ({ name: e.name, own: true })), ...catalog.exercises.map((e) => ({ name: e.name, sets: e.sets, reps: e.reps, sub: e.muscle }))]
          .filter((e, i, a) => a.findIndex((x) => norm(x.name) === norm(e.name)) === i && !have.has(norm(e.name)) && norm(e.name).includes(t)).slice(0, 30);
        mount(res, ...pool.map((e) => h("button", { class: "card tap row", style: { width: "100%", textAlign: "left" }, onclick: () => add(e.name, e) }, h("div", { class: "grow" }, h("div", { class: "bold" }, e.name), e.sub ? h("div", { class: "muted small" }, e.sub) : null), h("span", { class: "accent" }, "+"))),
          t.length >= 2 && !pool.some((e) => norm(e.name) === t) ? h("button", { class: "btn block", onclick: () => add(q.value.trim()) }, `Criar "${q.value.trim()}"`) : null);
      };
      q.addEventListener("input", show); show();
      return h("div", { class: "stack" }, h("h2", {}, "Adicionar exercício"), q, res);
    });
  }
}
