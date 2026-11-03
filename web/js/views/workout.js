// Treino: sem id mostra "escolha o grupo" (ou leva ao treino ativo); com id é a tela de registro de séries.
import * as api from "../api.js";
import { S, groupById, exercisesOf, exerciseById, reloadCatalog, invalidateHistory, ls } from "../state.js";
import { h, mount, toast, openSheet, confirmDialog, confetti } from "../lib/dom.js";
import { fmtKg, parseNum, fmtClock, fmtMinutes, minutesSince } from "../lib/format.js";
import { RestTimer, beep, unlockAudio, keepAwake } from "../lib/rest.js";

const norm = (s) => String(s).normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();

export default async function workoutView(root, params, { go }) {
  if (!params[0]) return chooser(root, go);
  return session(root, params[0], go);
}

async function chooser(root, go) {
  const active = await api.activeWorkout(S.uid).catch(() => null);
  if (active) return go("treino/" + active.id);
  root.append(h("h1", {}, "Treinar"), h("p", { class: "muted" }, "Escolha o grupo de hoje."));
  if (!S.groups.length) return root.append(h("div", { class: "card empty" }, "Crie um grupo de treino primeiro.", h("br"), h("a", { href: "#/grupos" }, "Criar grupo")));
  root.append(h("div", { class: "stack" }, S.groups.map((g) => h("button", { class: "card tap row", style: { width: "100%", textAlign: "left" }, onclick: async () => {
    try { const w = await api.startWorkout(S.uid, g.id); go("treino/" + w.id); } catch (e) { toast(e.message, "err"); }
  } }, h("span", { class: "dot", style: { background: g.color_hex || "var(--accent)" } }), h("div", { class: "grow" }, h("h3", {}, g.name), h("div", { class: "muted small" }, `${exercisesOf(g.id).length} exercícios`)), h("span", { class: "accent bold" }, "Iniciar")))));
}

async function session(root, id, go) {
  const w = await api.workout(id).catch(() => null);
  if (!w) { toast("Treino não encontrado.", "err"); return go("home"); }
  if (w.finished_at) return go("historico/" + id);
  const group = groupById(w.muscle_group_id);
  const allSets = await api.setsForWorkout(id);
  const sets = new Map(); // exercício -> séries
  for (const s of allSets) { if (!sets.has(s.exercise_id)) sets.set(s.exercise_id, []); sets.get(s.exercise_id).push(s); }

  // exercícios do treino: os do grupo, mais os que já têm série aqui (temporários de antes)
  let exs = exercisesOf(w.muscle_group_id).slice();
  for (const exId of sets.keys()) if (!exs.some((e) => e.id === exId)) { const e = exerciseById(exId); if (e) exs.push(e); }
  const finished = new Set(w.finished_exercise_ids || []);
  const base = new Map();
  const draft = new Map(); // exercício -> {kg, reps}
  let cur = Math.max(0, exs.findIndex((e) => !finished.has(e.id)));
  let lastSetAt = allSets.length ? Math.max(...allSets.map((s) => new Date(s.completed_at).getTime())) : 0;

  const loadBase = (list) => Promise.all(list.map(async (e) => { try { base.set(e.id, await api.baseline(e.id, id)); } catch { base.set(e.id, { lastKg: null, lastReps: null, recordKg: null }); } }));
  loadBase(exs).then(() => draw());

  const restSec = () => parseInt(ls.get("gt.rest", "90"), 10) || 90;
  const restBox = document.createElement("div");
  restBox.className = "rest"; restBox.hidden = true;
  const restTime = h("div", { class: "t" }, "00:00");
  const restBar = h("div", { class: "bar", style: { width: "100%" } });
  const timer = new RestTimer((left, total) => {
    restTime.textContent = fmtClock(left);
    restBar.style.width = (total ? (left / total) * 100 : 0) + "%";
  }, () => { restBox.hidden = true; beep(3); toast("Descanso acabou. Bora pra próxima série!", "ok"); });
  restBox.append(h("div", { class: "grow" }, h("div", { class: "tiny muted" }, "Descanso"), restTime),
    h("button", { class: "btn small", onclick: () => timer.add(-15) }, "−15"), h("button", { class: "btn small", onclick: () => timer.add(15) }, "+15"),
    h("button", { class: "btn small primary", onclick: () => { timer.stop(); restBox.hidden = true; } }, "Pular"), restBar);
  document.body.append(restBox);

  window.__gtWantsAwake = true; keepAwake(true);
  const elapsed = h("span", {}, "");
  const tick = () => { elapsed.textContent = fmtMinutes(minutesSince(w.started_at)); };
  tick(); const iv = setInterval(tick, 20000);

  const head = h("div", { class: "row between" });
  const strip = h("div", { class: "row", style: { overflowX: "auto", gap: "8px", paddingBottom: "4px", scrollbarWidth: "none" } });
  const body = h("div", { class: "stack" });
  root.append(head, strip, body);

  const setsOf = (exId) => sets.get(exId) || [];
  const totalSets = () => [...sets.values()].reduce((a, l) => a + l.length, 0);

  function prFlags(exId) {
    const b = base.get(exId); let max = 0; const out = new Set();
    for (const s of setsOf(exId)) {
      if (b?.recordKg != null && s.weight_kg > b.recordKg && s.weight_kg > max) out.add(s.id);
      max = Math.max(max, s.weight_kg);
    }
    return out;
  }

  function draw() {
    mount(head,
      h("div", {}, h("button", { class: "link small", onclick: () => go("home") }, "‹ Sair (continua salvo)"), h("h2", {}, group?.name || "Treino"), h("div", { class: "muted small" }, "Há ", elapsed, ` · ${totalSets()} séries`)),
      h("button", { class: "btn primary", onclick: finish }, "Finalizar"));
    mount(strip, ...exs.map((e, i) => {
      const c = h("button", { class: "chip" + (i === cur ? " on" : ""), onclick: () => { cur = i; draw(); } }, finished.has(e.id) ? "✓ " : "", e.name);
      return c;
    }), h("button", { class: "chip", onclick: addExerciseSheet }, "+ Exercício"));
    requestAnimationFrame(() => strip.children[cur]?.scrollIntoView?.({ inline: "center", block: "nearest", behavior: "smooth" }));
    drawBody();
  }

  function drawBody() {
    const e = exs[cur];
    if (!e) { mount(body, h("div", { class: "card empty" }, "Este grupo ainda não tem exercícios.", h("br"), h("button", { class: "link", onclick: addExerciseSheet }, "Adicionar exercício"))); return; }
    const b = base.get(e.id);
    const list = setsOf(e.id);
    const last = list[list.length - 1];
    const d = draft.get(e.id) || { kg: last?.weight_kg ?? b?.lastKg ?? 0, reps: last?.reps ?? b?.lastReps ?? e.default_reps ?? 10 };
    draft.set(e.id, d);
    const kg = h("input", { inputMode: "decimal", value: fmtKg(d.kg), "aria-label": "Carga em kg", onchange: () => { const v = parseNum(kg.value); if (!isNaN(v) && v >= 0) d.kg = v; kg.value = fmtKg(d.kg); } });
    const reps = h("input", { inputMode: "numeric", value: String(d.reps), "aria-label": "Repetições", onchange: () => { const v = parseInt(reps.value, 10); if (v >= 1 && v <= 999) d.reps = v; reps.value = String(d.reps); } });
    const step = (el, field, delta, min, fmt) => () => { d[field] = Math.max(min, Math.round((d[field] + delta) * 100) / 100); el.value = fmt(d[field]); };
    const stepper = (label, input, field, delta, min, fmt) => h("div", { class: "stepper" },
      h("button", { class: "iconbtn", "aria-label": `menos ${label}`, onclick: step(input, field, -delta, min, fmt) }, "−"),
      h("div", { class: "val" }, input, h("small", {}, label)),
      h("button", { class: "iconbtn", "aria-label": `mais ${label}`, onclick: step(input, field, delta, min, fmt) }, "+"));
    const prs = prFlags(e.id);

    const info = [];
    if (b?.lastKg != null) info.push(h("span", { class: "pill" }, `Última: ${fmtKg(b.lastKg)} kg × ${b.lastReps}`));
    if (b?.recordKg != null) info.push(h("span", { class: "pill accent" }, `Recorde: ${fmtKg(b.recordKg)} kg`));

    mount(body,
      h("div", { class: "card stack" },
        h("div", { class: "row between" }, h("h2", {}, e.name), finished.has(e.id) ? h("span", { class: "pill ok" }, "Concluído") : null),
        info.length ? h("div", { class: "row wrap" }, info) : null,
        stepper("kg", kg, "kg", 2.5, 0, fmtKg), stepper("reps", reps, "reps", 1, 1, String),
        h("button", { class: "btn primary block big", onclick: () => logSet(e, d) }, `Registrar série ${list.length + 1}`)),
      list.length ? h("div", { class: "card stack" }, h("h3", {}, "Séries"), h("div", { class: "row wrap" }, list.map((s, i) => h("button", { class: "set" + (prs.has(s.id) ? " pr" : ""), onclick: () => editSet(e, s) }, `${i + 1}. ${fmtKg(s.weight_kg)} kg × ${s.reps}`, prs.has(s.id) ? " 🏆" : "")))) : null,
      h("div", { class: "row" },
        h("button", { class: "btn ghost grow", disabled: cur === 0, onclick: () => { cur--; draw(); } }, "‹ Anterior"),
        h("button", { class: "btn grow", onclick: () => toggleDone(e) }, finished.has(e.id) ? "Reabrir" : "Concluir exercício"),
        h("button", { class: "btn ghost grow", disabled: cur >= exs.length - 1, onclick: () => { cur++; draw(); } }, "Próximo ›")),
      h("button", { class: "btn ghost block", onclick: notesSheet }, w.notes ? "Editar anotação do treino" : "Anotação do treino"));
  }

  async function logSet(e, d) {
    if (!(d.reps >= 1)) return toast("Informe as repetições.", "err");
    unlockAudio();
    const now = Date.now();
    const gap = lastSetAt && now - lastSetAt < 600000 ? Math.round((now - lastSetAt) / 1000) : null;
    try {
      const row = await api.addSet({ workoutId: id, exerciseId: e.id, setNumber: setsOf(e.id).length + 1, reps: d.reps, weightKg: d.kg, restSeconds: gap });
      if (!sets.has(e.id)) sets.set(e.id, []);
      sets.get(e.id).push(row); lastSetAt = now;
      if (prFlags(e.id).has(row.id)) { confetti(); toast(`Recorde! ${fmtKg(row.weight_kg)} kg 🏆`, "ok"); }
      timer.start(restSec()); restBox.hidden = false;
      draw();
    } catch (err) { toast(err.message, "err"); }
  }

  function editSet(e, s) {
    openSheet((close) => {
      const kg = h("input", { class: "input", inputMode: "decimal", value: fmtKg(s.weight_kg) });
      const reps = h("input", { class: "input", inputMode: "numeric", value: String(s.reps) });
      return h("div", { class: "stack" }, h("h2", {}, "Editar série"),
        h("div", { class: "field" }, h("label", {}, "Carga (kg)"), kg), h("div", { class: "field" }, h("label", {}, "Repetições"), reps),
        h("button", { class: "btn primary block", onclick: async () => {
          const k = parseNum(kg.value); const r = parseInt(reps.value, 10);
          if (isNaN(k) || k < 0 || !(r >= 1)) return toast("Valores inválidos.", "err");
          try { const row = await api.updateSet(s.id, r, k); Object.assign(s, row); close(); draw(); } catch (err) { toast(err.message, "err"); }
        } }, "Salvar"),
        h("button", { class: "btn danger block", onclick: async () => {
          try { await api.deleteSet(s.id); const l = sets.get(e.id); l.splice(l.indexOf(s), 1); close(); draw(); } catch (err) { toast(err.message, "err"); }
        } }, "Apagar série"));
    });
  }

  async function toggleDone(e) {
    if (finished.has(e.id)) finished.delete(e.id); else finished.add(e.id);
    try { await api.saveFinishedExercises(id, [...finished]); } catch (err) { toast(err.message, "err"); }
    if (finished.has(e.id)) { const nxt = exs.findIndex((x, i) => i > cur && !finished.has(x.id)); if (nxt >= 0) cur = nxt; }
    draw();
  }

  function notesSheet() {
    openSheet((close) => {
      const t = h("textarea", { class: "textarea", maxLength: 1000, placeholder: "Como foi o treino?" }, w.notes || "");
      return h("div", { class: "stack" }, h("h2", {}, "Anotação do treino"), t,
        h("button", { class: "btn primary block", onclick: async () => { try { await api.saveNotes(id, t.value); w.notes = t.value.trim() || null; close(); draw(); toast("Anotação salva", "ok"); } catch (err) { toast(err.message, "err"); } } }, "Salvar"));
    });
  }

  async function addExerciseSheet() {
    const catalog = await fetch("catalog.json").then((r) => r.json()).catch(() => ({ exercises: [] }));
    openSheet((close) => {
      let mode = "temp";
      const name = h("input", { class: "input", placeholder: "Nome do exercício", list: "gt-ex-names" });
      const dl = h("datalist", { id: "gt-ex-names" }, [...S.exercises.map((e) => e.name), ...catalog.exercises.map((e) => e.name)].filter((v, i, a) => a.indexOf(v) === i).map((n) => h("option", { value: n })));
      const modeBtns = h("div", { class: "row" });
      const hint = h("p", { class: "muted small" });
      const paint = () => {
        mount(modeBtns,
          h("button", { class: "chip" + (mode === "temp" ? " on" : ""), onclick: () => { mode = "temp"; paint(); } }, "Só neste treino"),
          h("button", { class: "chip" + (mode === "group" ? " on" : ""), onclick: () => { mode = "group"; paint(); } }, "Guardar no grupo"));
        hint.textContent = mode === "temp" ? "Entra só nesta sessão: não fica no grupo." : `Fica salvo no grupo ${group?.name || ""} para os próximos treinos.`;
      };
      paint();
      return h("div", { class: "stack" }, h("h2", {}, "Adicionar exercício"), modeBtns, hint, name, dl,
        h("button", { class: "btn primary block", onclick: async () => {
          const n = name.value.trim();
          if (n.length < 2) return toast("Digite o nome do exercício.", "err");
          try {
            let ex = S.exercises.find((e) => norm(e.name) === norm(n));
            if (!ex) {
              const cat = catalog.exercises.find((e) => norm(e.name) === norm(n));
              ex = await api.createExercise(S.uid, { name: cat?.name || n, sets: cat?.sets || 3, reps: cat?.reps || 12, groupIds: [] });
            }
            if (mode === "group" && w.muscle_group_id && !ex.groupIds.includes(w.muscle_group_id)) { await api.linkExercise(ex.id, [w.muscle_group_id]); }
            await reloadCatalog();
            ex = exerciseById(ex.id) || ex;
            if (!exs.some((x) => x.id === ex.id)) { exs.push(ex); await loadBase([ex]); }
            cur = exs.findIndex((x) => x.id === ex.id);
            close(); draw();
          } catch (err) { toast(err.message, "err"); }
        } }, "Adicionar"));
    });
  }

  async function finish() {
    if (totalSets() === 0) {
      const ok = await confirmDialog({ title: "Nenhuma série registrada", text: "Descartar este treino?", ok: "Descartar", danger: true });
      if (!ok) return;
      try { await api.discardWorkout(id); invalidateHistory(); go("home"); } catch (e) { toast(e.message, "err"); }
      return;
    }
    const ok = await confirmDialog({ title: "Finalizar treino?", text: `${totalSets()} séries registradas.`, ok: "Finalizar" });
    if (!ok) return;
    try {
      await api.saveFinishedExercises(id, [...finished]);
      await api.finishWorkout(id);
      invalidateHistory();
      const prCount = exs.reduce((n, e) => n + prFlags(e.id).size, 0);
      const vol = [...sets.values()].flat().reduce((a, s) => a + s.weight_kg * s.reps, 0);
      timer.stop(); restBox.hidden = true;
      openSheet((close) => h("div", { class: "stack center" }, h("h2", {}, "Treino finalizado! 💪"),
        h("div", { class: "row between" }, stat("Duração", fmtMinutes(minutesSince(w.started_at))), stat("Séries", totalSets()), stat("Volume", `${Math.round(vol)} kg`)),
        prCount ? h("p", { class: "accent bold" }, `${prCount} recorde${prCount > 1 ? "s" : ""} batido${prCount > 1 ? "s" : ""} 🏆`) : null,
        h("button", { class: "btn primary block", onclick: () => { close(); go("historico/" + id); } }, "Ver resumo")));
      if (prCount) confetti();
    } catch (e) { toast(e.message, "err"); }
  }
  const stat = (l, v) => h("div", { class: "grow" }, h("div", { class: "bold", style: { fontSize: "20px" } }, v), h("div", { class: "tiny muted" }, l));

  draw();
  return () => { clearInterval(iv); timer.stop(); restBox.remove(); window.__gtWantsAwake = false; keepAwake(false); };
}
