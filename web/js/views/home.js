// Início: treino ativo, meta da semana, sequências, últimas 8 semanas e botões para começar um grupo.
import * as api from "../api.js";
import { S, getHistory, groupById, exercisesOf } from "../state.js";
import { h, svg, toast, promptDialog } from "../lib/dom.js";
import { dayOf, dayOfIso, weeklyCount, streak, weekStreak, weeklyBuckets } from "../lib/dashboard.js";
import { fmtMinutes, minutesSince, fmtDay, initial } from "../lib/format.js";
import { rankFor } from "../lib/rankedRules.js";
import { emblemSvg } from "../lib/emblem.js";

function ring(done, goal) {
  const pct = Math.min(1, goal ? done / goal : 0);
  const r = 52; const c = 2 * Math.PI * r;
  const el = h("div", { class: "ring" });
  el.append(svg(`<svg viewBox="0 0 120 120"><circle cx="60" cy="60" r="${r}" fill="none" stroke="var(--card2)" stroke-width="10"/><circle cx="60" cy="60" r="${r}" fill="none" stroke="var(--accent)" stroke-width="10" stroke-linecap="round" stroke-dasharray="${c}" stroke-dashoffset="${c * (1 - pct)}"/></svg>`));
  el.append(h("div", { class: "mid" }, h("div", {}, h("div", { class: "bold", style: { fontSize: "26px" } }, `${done}/${goal}`), h("div", { class: "tiny muted" }, "na semana"))));
  return el;
}

function bars(buckets, goal) {
  const max = Math.max(goal, ...buckets.map((b) => b.count), 1);
  return h("div", { class: "row", style: { alignItems: "flex-end", height: "84px", gap: "6px" } }, buckets.map((b, i) => h("div", { class: "grow center", style: { display: "flex", flexDirection: "column", justifyContent: "flex-end", height: "100%", gap: "4px" } },
    h("div", { class: "tiny muted" }, b.count || ""),
    h("div", { style: { height: Math.max(4, (b.count / max) * 56) + "px", borderRadius: "6px", background: b.count >= goal ? "var(--accent)" : "var(--card2)", opacity: i === buckets.length - 1 ? 1 : 0.85 } }))));
}

export default async function home(root, _p, { go }) {
  const uid = S.uid;
  const [active, hist, rp] = await Promise.all([
    api.activeWorkout(uid).catch(() => null),
    getHistory().catch(() => []),
    api.rankedPlayer(uid).catch(() => null),
  ]);
  const today = dayOf(new Date());
  const days = hist.map((w) => dayOfIso(w.finished_at));
  const goal = S.profile?.weekly_goal || 4;
  const done = weeklyCount(days, today);
  const name = S.profile?.username || S.email?.split("@")[0] || "atleta";

  root.append(h("div", { class: "row between" },
    h("div", {}, h("p", { class: "muted small" }, new Date().getHours() < 12 ? "Bom dia" : new Date().getHours() < 18 ? "Boa tarde" : "Boa noite"), h("h1", {}, name)),
    h("a", { href: "#/mais", class: "av", style: { width: "44px", height: "44px" }, "aria-label": "Perfil" }, S.profile?.avatar_url ? h("img", { src: S.profile.avatar_url, alt: "" }) : initial(name))));

  if (active) {
    const g = groupById(active.muscle_group_id);
    const mins = minutesSince(active.started_at);
    root.append(h("div", { class: "card tap", style: { borderColor: "var(--accent)" }, onclick: () => go("treino/" + active.id) },
      h("div", { class: "row between" },
        h("div", {}, h("div", { class: "tiny accent bold" }, "Treino em andamento"), h("h3", {}, g?.name || "Treino"), h("p", { class: "muted small" }, `Começou há ${fmtMinutes(mins)}`)),
        h("button", { class: "btn primary" }, "Continuar"))));
  }

  root.append(h("div", { class: "card row", style: { gap: "18px" } }, ring(done, goal),
    h("div", { class: "stack grow" },
      h("div", {}, h("span", { class: "hero-num" }, streak(days, today)), h("span", { class: "muted" }, " dias seguidos")),
      h("div", { class: "muted small" }, `${weekStreak(days, today, goal)} semanas batendo a meta`),
      h("button", { class: "link", style: { textAlign: "left" }, onclick: async () => {
        const v = await promptDialog({ title: "Meta semanal", label: "Quantos treinos por semana?", value: String(goal), inputMode: "numeric", type: "number" });
        const n = parseInt(v, 10);
        if (v == null) return;
        if (!(n >= 1 && n <= 14)) return toast("Escolha de 1 a 14 treinos.", "err");
        try { await api.saveWeeklyGoal(uid, n); S.profile.weekly_goal = n; toast("Meta salva", "ok"); go("home"); } catch (e) { toast(e.message, "err"); }
      } }, "Mudar meta"))));

  root.append(h("div", { class: "card stack" }, h("div", { class: "row between" }, h("h3", {}, "Últimas 8 semanas"), h("span", { class: "muted small" }, `meta ${goal}`)), bars(weeklyBuckets(days, today, 8), goal)));

  if (rp) {
    const r = rankFor(rp.rp);
    const box = h("a", { href: "#/ranqueada", class: "card row tap", style: { textDecoration: "none", color: "inherit" } });
    const em = h("div", { style: { width: "64px", height: "64px" } }); em.append(svg(emblemSvg(r.tier, r.division)));
    em.firstChild.style.cssText = "width:64px;height:64px";
    box.append(em, h("div", { class: "grow" }, h("div", { class: "tiny muted" }, "Ranqueada"), h("h3", {}, r.label), h("div", { class: "muted small" }, r.isTop ? `${r.rpInDivision} RP acima da Lenda` : `${r.rpToNext} RP para subir`)));
    root.append(box);
  }

  root.append(h("h2", {}, "Começar treino"));
  if (!S.groups.length) {
    root.append(h("div", { class: "card empty" }, "Você ainda não tem grupos de treino.", h("br"), h("a", { href: "#/grupos" }, "Criar o primeiro grupo")));
  } else {
    root.append(h("div", { class: "stack" }, S.groups.map((g) => {
      const last = hist.find((w) => w.muscle_group_id === g.id);
      return h("button", { class: "card tap row", style: { textAlign: "left", width: "100%" }, onclick: async () => {
        if (active) { toast("Termine ou descarte o treino em andamento primeiro."); return go("treino/" + active.id); }
        try { const w = await api.startWorkout(uid, g.id); go("treino/" + w.id); } catch (e) { toast(e.message, "err"); }
      } },
        h("span", { class: "dot", style: { background: g.color_hex || "var(--accent)" } }),
        h("div", { class: "grow" }, h("h3", {}, g.name), h("div", { class: "muted small" }, `${exercisesOf(g.id).length} exercícios · ${last ? "último " + fmtDay(last.started_at) : "nunca treinado"}`)),
        h("span", { class: "accent bold" }, "Iniciar"));
    })));
  }
}
