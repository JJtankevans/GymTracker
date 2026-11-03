// Inicialização: configuração do Supabase, sessão, rotas por hash (#/home, #/treino/ID...) e barra de baixo.
import * as api from "./api.js";
import { S, loadBase, applyTheme, savedTheme, ls } from "./state.js";
import { h, mount, toast, spinner } from "./lib/dom.js";
import { ICON } from "./lib/icons.js";
import { svg } from "./lib/dom.js";

const ROUTES = {
  login: () => import("./views/auth.js"),
  setup: () => import("./views/setup.js"),
  home: () => import("./views/home.js"),
  treino: () => import("./views/workout.js"),
  historico: () => import("./views/history.js"),
  recordes: () => import("./views/records.js"),
  ranqueada: () => import("./views/ranked.js"),
  mais: () => import("./views/more.js"),
  grupos: () => import("./views/groups.js"),
};
const PUBLIC = new Set(["login", "setup"]);
const TABS = [
  { key: "home", label: "Início", icon: "home" },
  { key: "historico", label: "Histórico", icon: "history" },
  { key: "treino", label: "Treinar", icon: "dumbbell", go: true },
  { key: "ranqueada", label: "Ranqueada", icon: "trophy" },
  { key: "mais", label: "Mais", icon: "more" },
];

const app = document.getElementById("app");
let cleanup = null;
let navEl = null;
let token = 0;

export function go(path) {
  if (location.hash === "#/" + path) route(); else location.hash = "#/" + path;
}

function parse() {
  const parts = location.hash.replace(/^#\/?/, "").split("/").filter(Boolean);
  return { name: parts[0] || "home", params: parts.slice(1) };
}

function config() {
  const c = window.GYM_CONFIG || {};
  if (c.url && c.key) return { url: c.url, key: c.key };
  const url = ls.get("gt.url"); const key = ls.get("gt.key");
  return url && key ? { url, key } : null;
}

function drawNav(active) {
  navEl?.remove(); navEl = null;
  if (!active) return;
  navEl = h("nav", { class: "nav", "aria-label": "Principal" }, h("div", { class: "nav-in" }, TABS.map((t) => {
    const on = active === t.key || (t.key === "mais" && ["grupos", "recordes"].includes(active));
    const a = h("a", { href: "#/" + t.key, class: (on ? "on " : "") + (t.go ? "go" : ""), "aria-label": t.label, "aria-current": on ? "page" : null });
    const ic = svg(ICON[t.icon]);
    a.append(t.go ? h("span", { class: "fab" }, ic) : ic, h("span", {}, t.label));
    return a;
  })));
  document.body.append(navEl);
}

async function route() {
  const my = ++token;
  const { name: rawName, params } = parse();
  const name = ROUTES[rawName] ? rawName : "home";
  try { cleanup?.(); } catch { /* ignora */ }
  cleanup = null;
  document.querySelector(".rest")?.remove();

  if (!config()) { if (name !== "setup") return go("setup"); }
  else if (!api.client()) { const c = config(); api.init(c.url, c.key); }

  if (name !== "setup") {
    if (!S.uid) {
      const ses = await api.session().catch(() => null);
      if (my !== token) return;
      if (ses) { S.uid = ses.user.id; S.email = ses.user.email; }
    }
    if (!S.uid && !PUBLIC.has(name)) return go("login");
    if (S.uid && name === "login") return go("home");
    if (S.uid && !S.profile) {
      mount(app, spinner());
      try { await loadBase(); } catch (e) { if (my !== token) return; return fatal(e); }
      if (my !== token) return;
    }
  }

  drawNav(PUBLIC.has(name) ? null : (name === "treino" && params[0] ? null : name));
  mount(app, spinner());
  window.scrollTo(0, 0);
  try {
    const mod = await ROUTES[name]();
    if (my !== token) return;
    const root = h("div", { class: "screen" });
    mount(app, root);
    const out = await mod.default(root, params, { go, name });
    if (my !== token) { try { out?.(); } catch { /* ignora */ } return; }
    cleanup = typeof out === "function" ? out : null;
  } catch (e) {
    if (my !== token) return;
    fatal(e);
  }
}

function fatal(e) {
  console.error(e);
  mount(app, h("div", { class: "card stack center" }, h("h2", {}, "Algo deu errado"), h("p", { class: "muted" }, e?.message || "Erro inesperado."),
    h("button", { class: "btn primary", onclick: () => route() }, "Tentar de novo"),
    S.uid ? h("button", { class: "btn ghost", onclick: async () => { await api.signOut().catch(() => {}); S.uid = null; S.profile = null; go("login"); } }, "Sair") : null));
}

/** Para as telas: sai da conta e limpa o estado. */
export async function logout() {
  await api.signOut().catch(() => {});
  S.uid = null; S.email = null; S.profile = null; S.groups = []; S.exercises = [];
  go("login");
}

applyTheme(savedTheme());
window.addEventListener("hashchange", route);
if ("serviceWorker" in navigator && (location.protocol === "https:" || location.hostname === "localhost")) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}
route();
