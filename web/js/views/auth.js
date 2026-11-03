// Entrar / criar conta.
import * as api from "../api.js";
import { S } from "../state.js";
import { h, toast } from "../lib/dom.js";

export default function auth(root, _p, { go }) {
  let signup = false;
  const email = h("input", { class: "input", type: "email", placeholder: "voce@email.com", autocomplete: "email", autocapitalize: "off", inputMode: "email" });
  const user = h("input", { class: "input", placeholder: "Seu nome de usuário", autocomplete: "username", autocapitalize: "off" });
  const pass = h("input", { class: "input", type: "password", placeholder: "Senha (mín. 6)", autocomplete: "current-password", onkeydown: (e) => { if (e.key === "Enter") submit(); } });
  const userField = h("div", { class: "field", hidden: true }, h("label", {}, "Nome de usuário"), user);
  const title = h("h1", {}, "Entrar");
  const sub = h("p", { class: "muted" }, "Seus treinos, recordes e a ranqueada, em qualquer aparelho.");
  const btn = h("button", { class: "btn primary block big", onclick: () => submit() }, "Entrar");
  const toggle = h("button", { class: "link", onclick: () => flip() }, "Não tem conta? Criar agora");
  const flip = () => {
    signup = !signup;
    userField.hidden = !signup; title.textContent = signup ? "Criar conta" : "Entrar";
    btn.textContent = signup ? "Criar conta" : "Entrar";
    toggle.textContent = signup ? "Já tem conta? Entrar" : "Não tem conta? Criar agora";
    pass.autocomplete = signup ? "new-password" : "current-password";
  };
  async function submit() {
    const e = email.value.trim(); const p = pass.value;
    if (!/^\S+@\S+\.\S+$/.test(e)) return toast("Digite um e-mail válido.", "err");
    if (p.length < 6) return toast("A senha precisa de pelo menos 6 caracteres.", "err");
    if (signup && user.value.trim().length < 2) return toast("Escolha um nome de usuário.", "err");
    btn.disabled = true;
    try {
      if (signup) {
        const inNow = await api.signUp(e, p, user.value.trim());
        if (!inNow) { toast("Conta criada! Confirme o e-mail e depois entre.", "ok"); flip(); btn.disabled = false; return; }
      } else await api.signIn(e, p);
      const ses = await api.session();
      S.uid = ses.user.id; S.email = ses.user.email; S.profile = null;
      go("home");
    } catch (err) { toast(err.message, "err"); btn.disabled = false; }
  }
  root.append(h("div", { class: "stack", style: { paddingTop: "40px" } },
    h("img", { src: "icons/icon-192.png", alt: "", width: 72, height: 72, style: { borderRadius: "18px" } }),
    title, sub,
    h("div", { class: "field" }, h("label", {}, "E-mail"), email), userField,
    h("div", { class: "field" }, h("label", {}, "Senha"), pass),
    btn, h("div", { class: "center" }, toggle),
    h("div", { class: "center small muted" }, h("a", { href: "#/setup" }, "Trocar o projeto Supabase"))));
}
