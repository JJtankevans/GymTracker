// Mini biblioteca de DOM: h("div", {class: "x", onclick}, filhos...) e os componentes de tela (toast, folha, confirmação).

export function h(tag, props, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props || {})) {
    if (v == null || v === false) continue;
    if (k === "class") el.className = v;
    else if (k === "style" && typeof v === "object") Object.assign(el.style, v);
    else if (k.startsWith("on") && typeof v === "function") el.addEventListener(k.slice(2).toLowerCase(), v);
    else if (k === "for") el.htmlFor = v;
    else if (k in el && k !== "list" && !k.startsWith("aria")) { try { el[k] = v; } catch { el.setAttribute(k, v); } }
    else el.setAttribute(k, v === true ? "" : v);
  }
  append(el, kids);
  return el;
}

export function append(el, kids) {
  for (const k of kids.flat(Infinity)) {
    if (k == null || k === false) continue;
    el.append(k instanceof Node ? k : document.createTextNode(String(k)));
  }
  return el;
}

/** Transforma um texto de SVG estático (escrito por nós) em elemento. */
export function svg(markup) {
  const t = document.createElement("template");
  t.innerHTML = markup.trim();
  return t.content.firstChild;
}

export function mount(root, ...nodes) {
  root.replaceChildren();
  append(root, nodes);
}

export function toast(msg, kind = "") {
  const box = document.getElementById("toasts");
  if (!box) return;
  const t = h("div", { class: "toast " + kind }, msg);
  box.append(t);
  setTimeout(() => t.remove(), kind === "err" ? 5200 : 2800);
}

/** Folha que sobe de baixo. build(close) devolve os filhos. Fecha tocando fora. */
export function openSheet(build) {
  const wrap = h("div", { class: "sheet-wrap", onclick: (e) => { if (e.target === wrap) close(); } });
  const box = h("div", { class: "sheet" }, h("div", { class: "grab" }));
  const close = () => wrap.remove();
  append(box, [build(close)]);
  wrap.append(box);
  document.body.append(wrap);
  return close;
}

/** Confirmação em folha. `typed` exige digitar uma palavra (ações que apagam de verdade). */
export function confirmDialog({ title, text, ok = "Confirmar", danger = false, typed = null }) {
  return new Promise((resolve) => {
    let done = false;
    const finish = (v, close) => { if (!done) { done = true; close(); resolve(v); } };
    openSheet((close) => {
      const input = typed ? h("input", { class: "input", placeholder: `Digite ${typed}`, autocapitalize: "characters", oninput: () => { okBtn.disabled = input.value.trim().toUpperCase() !== typed; } }) : null;
      const okBtn = h("button", { class: "btn block " + (danger ? "danger" : "primary"), disabled: !!typed, onclick: () => finish(true, close) }, ok);
      const cancel = h("button", { class: "btn ghost block", onclick: () => finish(false, close) }, "Cancelar");
      return h("div", { class: "stack" }, h("h2", {}, title), text ? h("p", { class: "muted" }, text) : null, input, okBtn, cancel);
    });
    // fechar tocando fora cancela
    const obs = new MutationObserver(() => { if (!document.querySelector(".sheet-wrap")) { obs.disconnect(); if (!done) { done = true; resolve(false); } } });
    obs.observe(document.body, { childList: true });
  });
}

/** Pede um texto ou número numa folha. Devolve null se cancelou. */
export function promptDialog({ title, label, value = "", ok = "Salvar", inputMode = "text", type = "text" }) {
  return new Promise((resolve) => {
    let done = false;
    openSheet((close) => {
      const input = h("input", { class: "input", value, inputMode, type, onkeydown: (e) => { if (e.key === "Enter") go(); } });
      const go = () => { if (!done) { done = true; close(); resolve(input.value); } };
      setTimeout(() => input.focus(), 50);
      return h("div", { class: "stack" }, h("h2", {}, title), h("div", { class: "field" }, label ? h("label", {}, label) : null, input),
        h("button", { class: "btn primary block", onclick: go }, ok),
        h("button", { class: "btn ghost block", onclick: () => { if (!done) { done = true; close(); resolve(null); } } }, "Cancelar"));
    });
  });
}

export function spinner() { return h("div", { class: "spin", role: "progressbar", "aria-label": "Carregando" }); }

/** Chuva de confete (recorde batido). */
export function confetti(colors = ["#C6F432", "#FFC53D", "#FF5A5F", "#38BDF8", "#FF8FB8"]) {
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const box = h("div", { class: "confetti" });
  for (let i = 0; i < 36; i++) {
    box.append(h("i", { style: { left: Math.random() * 100 + "%", background: colors[i % colors.length], animationDelay: Math.random() * 0.5 + "s", animationDuration: 1.1 + Math.random() * 0.9 + "s" } }));
  }
  document.body.append(box);
  setTimeout(() => box.remove(), 2600);
}
