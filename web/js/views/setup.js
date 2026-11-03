// Primeira configuração: endereço e chave pública (anon) do Supabase. Só aparece se o deploy não trouxe o config.js preenchido.
import { h, toast } from "../lib/dom.js";
import { ls } from "../state.js";

export default function setup(root, _p, { go }) {
  const url = h("input", { class: "input", placeholder: "https://xxxx.supabase.co", value: ls.get("gt.url", ""), inputMode: "url", autocapitalize: "off", autocomplete: "off" });
  const key = h("textarea", { class: "textarea", placeholder: "Chave pública (anon / publishable)", autocapitalize: "off", spellcheck: false }, ls.get("gt.key", ""));
  root.append(
    h("h1", {}, "Configurar"),
    h("p", { class: "muted" }, "Cole o endereço e a chave pública do projeto Supabase do GymTracker (as mesmas do app de celular). Fica só neste aparelho."),
    h("div", { class: "field" }, h("label", {}, "Endereço do projeto"), url),
    h("div", { class: "field" }, h("label", {}, "Chave pública"), key),
    h("button", { class: "btn primary block", onclick: () => {
      const u = url.value.trim().replace(/\/+$/, ""); const k = key.value.trim();
      if (!/^https:\/\/[^\s/]+\.[^\s/]+$/.test(u)) return toast("O endereço deve começar com https://", "err");
      if (k.length < 20) return toast("Cole a chave pública completa.", "err");
      if (/service_role/i.test(k) || (k.split(".").length === 3 && atob(k.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")).includes("service_role"))) return toast("Essa é a chave secreta! Use só a pública (anon).", "err");
      ls.set("gt.url", u); ls.set("gt.key", k);
      location.hash = "#/login"; location.reload();
    } }, "Salvar e continuar"),
  );
}
