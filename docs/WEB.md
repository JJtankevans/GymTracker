# GymTracker web (PWA) — para quem usa iPhone

App web que fala com o **mesmo Supabase** do app de celular (mesmas tabelas e funções: treino, histórico, recordes, grupos, ranqueada com x1). Sem build: HTML + JS puro (módulos ES) em `web/`.

## O que tem
Entrar/criar conta · Início (meta semanal, sequência de dias/semanas, últimas 8 semanas, treino em andamento, chip da ranqueada) · Treino (chips de exercício com rolagem automática, carga/repetições com +/−, recorde com confete, descanso com bipe e tela acesa, exercício temporário "só neste treino", anotação, finalizar com resumo) · Histórico (filtro por grupo, detalhe, apagar) · Recordes · Ranqueada (entrar, elo, turma da semana com zonas, o que falta para subir, semana passada, x1 valendo RP/amistoso, aceitar/recusar) · Grupos e exercícios (catálogo de 93 exercícios em `web/catalog.json`) · Temas (Lima, Oceano, Sakura, Ouro, Papel) · Descanso padrão · Exportar CSV · Sair · Excluir conta (digitando EXCLUIR).

**Não tem (ainda):** grupos sociais/desafios, feed, medidas corporais, conquistas, foto de perfil, relógio. O iPhone não vibra pelo Safari: o aviso do fim do descanso é bipe + barra (e a tela fica acesa durante o treino, onde o iOS permite).

## Publicar (precisa de HTTPS)
1. Rode o workflow **Web** (Actions) ou faça push em `web/`. O artefato `gymtracker-web` é a pasta pronta; ele usa os secrets `SUPABASE_URL` e `SUPABASE_KEY` (chave publicável, a mesma do Android) para escrever o `config.js`.
2. Hospede a pasta em qualquer lugar estático com HTTPS: Netlify (arrastar a pasta em *Deploys*), Cloudflare Pages (upload direto), Vercel ou GitHub Pages.
3. No Supabase: *Authentication › URL Configuration* — acrescente o endereço do site em *Site URL / Redirect URLs* se for usar confirmação de e-mail.
4. No iPhone: abrir o endereço no **Safari** › Compartilhar › **Adicionar à Tela de Início**. Abre em tela cheia, como um app.
- Sem os secrets, o site pede o endereço do Supabase e a chave pública na primeira abertura (fica só no aparelho). Nunca cole a chave `service_role`: o app recusa.
- Rodar local: `cd web && python3 -m http.server 8080` e abrir `http://localhost:8080` (service worker só liga em HTTPS ou localhost).

## Testes
- `node --test web-tests/lib.test.mjs` — contas da ranqueada, início e formatação, com os mesmos valores dos testes em Kotlin.
- `node web-tests/e2e.mjs --shots pasta` — Chromium (Playwright) contra `web-tests/fake-supabase.mjs`, um Supabase de mentira (auth + PostgREST + rpc) com dados de exemplo. Cobre login, treino completo (recorde, descanso, temporário, finalizar), sair/voltar pelo banner, grupos, tema, ranqueada/x1, CSV e exclusão de conta. `node web-tests/fake-supabase.mjs` sobe o servidor na porta 8099 para olhar o app à mão (thiago@teste.com / senha123).
- **Não testado:** Safari/iOS de verdade, Supabase real, service worker em produção, bipe/wake lock em aparelho.

## Arquivos
`web/js/main.js` (rotas por hash, navegação) · `web/js/api.js` (todas as chamadas ao Supabase) · `web/js/views/*` (telas) · `web/js/lib/*` (regras, formatação, DOM) · `web/sw.js` (cache do "casco") · `tools/gerar_catalogo_web.py` (gera `web/catalog.json`).
