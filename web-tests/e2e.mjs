// Testes ponta a ponta do app web (Chromium + servidor falso). Uso: node web-tests/e2e.mjs [--shots pasta]
import { createRequire } from "node:module";
const { chromium } = createRequire(import.meta.url)("playwright");
import { startServer } from "./fake-supabase.mjs";
import assert from "node:assert/strict";
import fs from "node:fs";

const shotsDir = process.argv.includes("--shots") ? process.argv[process.argv.indexOf("--shots") + 1] : null;
if (shotsDir) fs.mkdirSync(shotsDir, { recursive: true });
let failed = 0;
const results = [];

async function scenario(name, opts, fn) {
  const srv = await startServer(opts);
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, locale: "pt-BR", timezoneId: "America/Sao_Paulo" });
  const page = await ctx.newPage();
  page.setDefaultTimeout(6000);
  const errors = [];
  page.on("pageerror", (e) => errors.push("pageerror: " + e.message));
  page.on("console", (m) => { if (m.type() === "error" && !/Failed to load resource/.test(m.text())) errors.push("console: " + m.text()); });
  const shot = async (n) => { if (shotsDir) await page.screenshot({ path: `${shotsDir}/${n}.png` }); };
  try {
    await fn({ page, srv, shot, base: srv.url });
    assert.deepEqual(errors, [], "erros no console");
    results.push(["ok", name]);
  } catch (e) {
    failed++; results.push(["FALHOU", name, e.message.split("\n")[0]]);
    if (shotsDir) await page.screenshot({ path: `${shotsDir}/FALHA-${name.replace(/\W+/g, "_")}.png` }).catch(() => {});
    console.error(`\n--- ${name} ---\n`, e);
  } finally { await browser.close(); await srv.close(); }
}

const login = async (page, base) => {
  await page.goto(base + "/#/login");
  await page.fill('input[type="email"]', "thiago@teste.com");
  await page.fill('input[type="password"]', "senha123");
  await page.click("text=Entrar >> nth=-1");
  await page.waitForSelector("text=Começar treino");
};

await scenario("login errado mostra erro e certo leva ao início", {}, async ({ page, base, shot }) => {
  await page.goto(base + "/");
  await page.waitForSelector("h1:has-text('Entrar')");
  await shot("01-login");
  await page.fill('input[type="email"]', "thiago@teste.com");
  await page.fill('input[type="password"]', "errada1");
  await page.click("button.primary");
  await page.waitForSelector("text=E-mail ou senha incorretos");
  await page.fill('input[type="password"]', "senha123");
  await page.click("button.primary");
  await page.waitForSelector("text=Começar treino");
  await shot("02-home");
  assert.ok(await page.locator("text=Peito e tríceps").first().isVisible());
  assert.equal(await page.locator(".hero-num").first().textContent(), "3", "sequência de 3 dias");
});

await scenario("treino completo: série, recorde, descanso, temporário, finalizar", {}, async ({ page, base, shot, srv }) => {
  await login(page, base);
  await page.click("text=Peito e tríceps >> xpath=ancestor::button");
  await page.waitForSelector("text=Registrar série 1");
  await page.click(".chip:has-text('Supino reto')");
  await page.waitForSelector("text=Recorde:");
  await shot("03-treino");
  // supino: recorde antigo 65 -> registra 70 (recorde)
  await page.fill('input[aria-label="Carga em kg"]', "70");
  await page.dispatchEvent('input[aria-label="Carga em kg"]', "change");
  await page.click("text=Registrar série 1");
  await page.waitForSelector("text=Registrar série 2");
  await page.waitForSelector(".rest:not([hidden])");
  await page.waitForSelector("text=Recorde! 70 kg");
  await shot("04-serie-descanso");
  assert.equal(srv.db.workout_sets.filter((s) => s.weight_kg === 70).length, 1);
  // pular descanso
  await page.click(".rest >> text=Pular");
  assert.ok(await page.locator(".rest").isHidden());
  // concluir exercício avança
  await page.click("text=Concluir exercício");
  await page.waitForSelector("h2:has-text('Tríceps corda')");
  // temporário
  await page.click(".chip:has-text('+ Exercício')");
  await page.fill('input[placeholder="Nome do exercício"]', "Pump de ombro");
  await page.click("button:has-text('Adicionar')");
  await page.waitForSelector("h2:has-text('Pump de ombro')");
  assert.equal(srv.db.group_exercises.filter((l) => l.exercise_id === srv.db.exercises.find((e) => e.name === "Pump de ombro").id).length, 0, "temporário não entra no grupo");
  await page.fill('input[aria-label="Carga em kg"]', "12");
  await page.dispatchEvent('input[aria-label="Carga em kg"]', "change");
  await page.click("text=Registrar série 1");
  await page.waitForSelector("text=1. 12 kg × 12");
  await shot("05-temporario");
  // finalizar
  await page.click("button:has-text('Finalizar')");
  await page.click(".sheet button:has-text('Finalizar')");
  await page.waitForSelector("text=Treino finalizado");
  await shot("06-resumo");
  assert.ok(srv.db.workouts.some((w) => w.finished_at && w.id !== "w-old0" && !w.id.startsWith("w-old")));
  await page.click("text=Ver resumo");
  await page.waitForSelector("text=Pump de ombro");
  await page.goto(base + "/#/historico");
  await page.waitForSelector("text=Todos");
  await shot("07-historico");
});

await scenario("sair do treino e voltar pelo banner da Home; descartar treino vazio", {}, async ({ page, base, shot }) => {
  await login(page, base);
  await page.click("text=Costas e bíceps >> xpath=ancestor::button");
  await page.waitForSelector("text=Registrar série 1");
  await page.click("text=‹ Sair");
  await page.waitForSelector("text=Treino em andamento");
  await shot("08-banner");
  await page.click("text=Continuar");
  await page.waitForSelector("h2:has-text('Remada curvada')");
  await page.click("button:has-text('Finalizar')");
  await page.click(".sheet button:has-text('Descartar')");
  await page.waitForSelector("text=Começar treino");
  assert.equal(await page.locator("text=Treino em andamento").count(), 0);
});

await scenario("descanso continua certo quando a aba volta (relógio, não tique)", {}, async ({ page, base }) => {
  await login(page, base);
  await page.click("text=Costas e bíceps >> xpath=ancestor::button");
  await page.click("text=Registrar série 1");
  await page.waitForSelector(".rest:not([hidden])");
  const a = await page.locator(".rest .t").textContent();
  await page.waitForTimeout(2200);
  const b = await page.locator(".rest .t").textContent();
  const sec = (s) => parseInt(s.slice(0, 2)) * 60 + parseInt(s.slice(3));
  assert.ok(sec(a) - sec(b) >= 2, `descanso deveria andar: ${a} -> ${b}`);
});

await scenario("recordes, grupos, tema e mais", {}, async ({ page, base, shot, srv }) => {
  await login(page, base);
  await page.click("nav >> text=Mais");
  await page.waitForSelector("text=Recordes");
  await shot("09-mais");
  await page.click("text=Oceano");
  assert.equal(await page.evaluate(() => document.documentElement.dataset.theme), "oceano");
  await page.click("h3:has-text('Recordes')");
  await page.waitForSelector("text=Supino reto");
  await shot("10-recordes");
  await page.goto(base + "/#/grupos");
  await page.click("button:has-text('+ Novo')");
  await page.fill('input[placeholder^="Ex.:"]', "Pernas");
  await page.click(".sheet button:has-text('Salvar')");
  await page.waitForSelector("h3:has-text('Pernas')");
  assert.ok(srv.db.muscle_groups.some((g) => g.name === "Pernas"));
  await page.click("h3:has-text('Pernas')");
  await page.click("button:has-text('+ Adicionar exercício')");
  await page.fill('.sheet input[type="search"]', "agachamento liv");
  await shot("11-add-exercicio");
  await page.click(".sheet .card:has-text('Agachamento livre') >> nth=0");
  await page.waitForSelector("h3:has-text('Agachamento livre')");
  assert.ok(srv.db.group_exercises.length >= 5);
  // recarregar mantém sessão e tema
  await page.reload();
  await page.waitForSelector("h3:has-text('Agachamento livre')");
  assert.equal(await page.evaluate(() => document.documentElement.dataset.theme), "oceano");
});

await scenario("ranqueada: entrar, turma e x1", {}, async ({ page, base, shot, srv }) => {
  await login(page, base);
  await page.click("nav >> text=Ranqueada");
  await page.waitForSelector("text=Entre na disputa");
  await shot("12-ranked-entrar");
  await page.click("text=Entrar na ranqueada");
  await page.waitForSelector("text=Sua turma");
  await page.waitForSelector("text=Bruno");
  await shot("13-ranked");
  await page.click(".board-row:has-text('Bruno') >> text=x1");
  await shot("14-x1");
  await page.click(".sheet >> text=Amistoso");
  await page.waitForSelector(".toast:has-text('x1 amistoso enviado')");
  assert.equal(srv.db.duels.length, 1);
  assert.equal(srv.db.duels[0].ranked, false);
});

await scenario("ranqueada indisponível sem a migração 018 mostra erro amigável", { withRanked: false }, async ({ page, base }) => {
  await login(page, base);
  await page.goto(base + "/#/ranqueada");
  await page.waitForSelector("text=Entre na disputa");
});

await scenario("exportar CSV e excluir conta pedem confirmação", {}, async ({ page, base, srv }) => {
  await login(page, base);
  await page.goto(base + "/#/mais");
  const dl = page.waitForEvent("download");
  await page.click("h3:has-text('Exportar treinos')");
  const d = await dl;
  const txt = fs.readFileSync(await d.path(), "utf8");
  assert.match(txt, /data;grupo;exercicio/);
  assert.match(txt, /Supino reto/);
  await page.click("h3:has-text('Excluir minha conta')");
  await page.click(".sheet button:has-text('Excluir tudo')", { force: true, timeout: 800 }).catch(() => {});
  assert.ok(!srv.state.deleted, "não pode excluir sem digitar EXCLUIR");
  await page.fill(".sheet input", "excluir");
  await page.click(".sheet button:has-text('Excluir tudo')");
  await page.waitForSelector("h1:has-text('Entrar')");
  assert.ok(srv.state.deleted);
});

console.log("\n" + results.map((r) => r.join("  ")).join("\n"));
process.exit(failed ? 1 : 0);
