// Ranqueada: elo, turma da semana, resultado da semana passada e x1 (mesmas regras do app; o banco decide).
import * as api from "../api.js";
import { S } from "../state.js";
import { h, svg, toast, openSheet, confirmDialog, spinner } from "../lib/dom.js";
import { rankFor, analyzeBoard, seasonOf, canDuel, TIERS } from "../lib/rankedRules.js";
import { emblemSvg } from "../lib/emblem.js";
import { dayOf, weekStart, weekdayOf } from "../lib/dashboard.js";
import { initial } from "../lib/format.js";

const ZONE = { PROMOTE: ["Zona de subida", "var(--ok)"], SAFE: ["Zona segura", "var(--muted)"], DEMOTE: ["Zona de queda", "var(--danger)"] };

export default async function ranked(root, _p, { go }) {
  const reload = () => go("ranqueada");
  const player = await api.rankedPlayer(S.uid).catch((e) => { throw e; });
  root.append(h("h1", {}, "Ranqueada"));
  if (!player) {
    root.append(h("div", { class: "card stack center" }, h("h2", {}, "Entre na disputa"),
      h("p", { class: "muted" }, "Toda semana você é colocado numa turma do seu elo. Quem treina mais sobe; quem some, desce. Dá para desafiar alguém do seu elo pra um x1 de 7 dias."),
      h("button", { class: "btn primary big", onclick: async (e) => { e.target.disabled = true; try { await api.rankedJoin(); toast("Você entrou na ranqueada!", "ok"); reload(); } catch (err) { toast(err.message, "err"); e.target.disabled = false; } } }, "Entrar na ranqueada")));
    return;
  }
  const today = dayOf(new Date());
  const monday = weekStart(today);
  const iso = (d) => new Date(d * 86400000).toISOString().slice(0, 10);
  // fecha a semana passada e os x1 vencidos (idempotente)
  await api.rankedSettleWeek(iso(monday - 7)).catch(() => {});
  await api.rankedSettleDuels().catch(() => {});
  const [fresh, board, last, duels] = await Promise.all([
    api.rankedPlayer(S.uid).catch(() => player), api.rankedBoard().catch(() => []), api.rankedLastResult().catch(() => null), api.rankedDuels().catch(() => []),
  ]);
  const me = fresh || player;
  const r = rankFor(me.rp);
  const season = seasonOf(monday);
  const mine = board.find((b) => b.is_me);
  const a = mine ? analyzeBoard(board.map((b) => b.points), mine.points) : null;

  const em = h("div", { class: "emblem" }); em.append(svg(emblemSvg(r.tier, r.division)));
  root.append(h("div", { class: "card row", style: { gap: "16px" } }, em,
    h("div", { class: "grow stack", style: { gap: "6px" } },
      h("div", { class: "tiny muted" }, `Temporada ${season.number} · semana ${season.weekInSeason}/8${season.isLastWeek ? " (última!)" : ""}`),
      h("h2", {}, r.label), h("div", { class: "muted small" }, `${me.rp} RP · pico ${me.peak_rp}`),
      h("div", { class: "progress" }, h("i", { style: { width: Math.round(r.progress * 100) + "%" } })),
      h("div", { class: "small muted" }, r.isTop ? "Você é Lenda!" : `${r.rpToNext} RP para a próxima divisão`))));

  if (a) {
    const [zl, zc] = ZONE[a.zone];
    root.append(h("div", { class: "card stack" },
      h("div", { class: "row between" }, h("h3", {}, `Você é o ${a.position}º de ${a.size}`), h("span", { class: "pill", style: { color: zc } }, zl)),
      h("div", { class: "muted small" }, `${mine.points} pts esta semana · se fechasse agora: ${a.projectedDelta > 0 ? "+" : ""}${a.projectedDelta} RP`),
      a.toPromotion != null ? h("div", { class: "small" }, `Faltam ${a.toPromotion} pts para entrar na zona de subida.`) : null,
      a.toSafety != null ? h("div", { class: "small danger" }, `Faltam ${a.toSafety} pts para sair da zona de queda.`) : null));
  }

  if (last && last.delta != null) {
    root.append(h("div", { class: "card row between" }, h("div", {}, h("div", { class: "tiny muted" }, "Semana passada"), h("div", {}, `${last.pos}º de ${last.cohort_size} · ${last.points} pts`)),
      h("span", { class: "pill " + (last.delta >= 0 ? "ok" : "bad") }, `${last.delta > 0 ? "+" : ""}${last.delta} RP`)));
  }

  // x1
  const pending = duels.filter((d) => d.status === "pending" && d.opponent_id === S.uid);
  const activeD = duels.filter((d) => d.status === "active");
  if (pending.length) root.append(h("div", { class: "card stack" }, h("h3", {}, "Desafios para você"), ...pending.map((d) => h("div", { class: "row" },
    h("div", { class: "grow" }, h("div", { class: "bold" }, d.challenger_name), h("div", { class: "muted small" }, d.ranked ? "Valendo RP" : "Amistoso")),
    h("button", { class: "btn small primary", onclick: () => respond(d, true) }, "Aceitar"), h("button", { class: "btn small", onclick: () => respond(d, false) }, "Recusar")))));
  async function respond(d, ok) { try { await api.rankedDuelRespond(d.id, ok); toast(ok ? "x1 aceito: valem 7 dias!" : "x1 recusado", "ok"); reload(); } catch (e) { toast(e.message, "err"); } }
  if (activeD.length) root.append(h("div", { class: "card stack" }, h("h3", {}, "x1 em andamento"), ...activeD.map((d) => {
    const meChal = d.challenger_id === S.uid;
    const my = meChal ? d.pt_challenger : d.pt_opponent; const their = meChal ? d.pt_opponent : d.pt_challenger;
    return h("div", { class: "row between" }, h("div", {}, h("div", { class: "bold" }, `Você × ${meChal ? d.opponent_name : d.challenger_name}`), h("div", { class: "muted small" }, d.ranked ? "Valendo RP" : "Amistoso")), h("div", { class: "bold", style: { fontSize: "20px" } }, `${my ?? 0} × ${their ?? 0}`));
  })));

  root.append(h("div", { class: "row between" }, h("h2", {}, "Sua turma"), h("span", { class: "muted small" }, `${board.length} pessoas`)));
  root.append(h("div", { class: "card flat stack", style: { gap: "2px" } }, ...(board.length ? board.map((b) => {
    const zone = a ? analyzeBoard(board.map((x) => x.points), b.points).zone : "SAFE";
    const rk = rankFor(b.rp);
    const busy = duels.some((d) => ["pending", "active"].includes(d.status) && [d.challenger_id, d.opponent_id].includes(b.profile_id));
    return h("div", { class: "board-row" + (b.is_me ? " me" : "") },
      h("div", { class: "zone-bar", style: { background: ZONE[zone][1] } }),
      h("div", { class: "bold", style: { width: "22px", textAlign: "center" } }, b.pos),
      h("div", { class: "av" }, b.avatar_url ? h("img", { src: b.avatar_url, alt: "" }) : initial(b.username)),
      h("div", { class: "grow" }, h("div", { class: "trunc bold" }, b.username, b.is_me ? " (você)" : ""), h("div", { class: "muted small" }, rk.label)),
      h("div", { class: "bold" }, `${b.points} pts`),
      !b.is_me && !busy ? h("button", { class: "btn small", onclick: () => duelSheet(b) }, "x1") : null);
  }) : [h("div", { class: "empty" }, "A turma aparece quando a semana abrir.")])));

  function duelSheet(b) {
    const same = canDuel(me.rp, b.rp);
    openSheet((close) => h("div", { class: "stack" }, h("h2", {}, `x1 com ${b.username}`),
      h("p", { class: "muted" }, "Dura 7 dias a partir do aceite. Vence quem somar mais pontos."),
      h("button", { class: "btn primary block", disabled: !same, onclick: () => send(b, true, close) }, "Valendo RP"),
      !same ? h("p", { class: "small muted" }, "Valendo RP só entre pessoas do mesmo elo.") : null,
      h("button", { class: "btn block", onclick: () => send(b, false, close) }, "Amistoso"),
      h("button", { class: "btn ghost block", onclick: close }, "Cancelar")));
  }
  async function send(b, rankedDuel, close) {
    try { await api.rankedDuelCreate(b.profile_id, rankedDuel); close(); toast(rankedDuel ? "x1 valendo RP enviado" : "x1 amistoso enviado", "ok"); reload(); } catch (e) { toast(e.message, "err"); }
  }
}
