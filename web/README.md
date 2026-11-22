# GymTracker Web (PWA para iPhone)

Vite + React + TypeScript + supabase-js. Mesma conta e mesmos dados do app Android (projeto Supabase do GymTracker).
Plano e decisões: `../docs/WEB_APP.md`.

## Rodar
```powershell
cd web
npm install
copy .env.example .env      # preencha VITE_SUPABASE_URL e VITE_SUPABASE_KEY (chave sb_publishable_..., nunca a service role)
npm run dev                 # http://localhost:5173
npm test                    # regras portadas do :shared (1RM, próximo treino, duração, treino esquecido, conquistas, recordes)
npm run build               # tsc + vite build -> dist/
```
**Modo demonstração:** abra `http://localhost:5173/?mock` para ver a interface com dados falsos, sem login.

## Instalar no iPhone
Abra o site no Safari → Compartilhar → **Adicionar à Tela de Início**. Abre em tela cheia (standalone).

## Publicar (Vercel)
Projeto novo apontando para a pasta `web/` (Framework: Vite). Variáveis: `VITE_SUPABASE_URL` e `VITE_SUPABASE_KEY`. `vercel.json` já cuida das rotas e do service worker.
No Supabase → Authentication → URL Configuration: acrescente o endereço do site em Redirect URLs.

## O que tem
Login · Início (próximo treino, meta, sequência, frequência) · Treino (séries com −1/+1 e −2,5/+2,5, descanso, RPE local, finalizar, treino esquecido) ·
Histórico (mapa de calor, filtro, detalhe, apagar com Desfazer) · Grupos (criar, exercícios, remover) · Evolução (peso e 1RM) · Recordes · Conquistas (selo que vira) · catálogo de 70 exercícios e treinos prontos · níveis de força · mapa muscular da semana · retrospectiva do mês/ano e imagem para compartilhar (também no resumo do treino) · meta semanal · backup/importar · 9 temas (claro/escuro).

## Limites conhecidos
- Recordes e conquistas olham os últimos 300 treinos (sem a função `records_summary()` do servidor).
- Sem Social, push, medidas com fita e importação do histórico (cards #533, #534; medidas dependem da migração 013). Backup: exporta tudo, importa só grupos e exercícios.
- No iPhone a tela apaga e o JavaScript para: cronômetros usam horários, não contagem. RPE por série fica só neste aparelho (a coluna ainda não existe no banco).
- Nada disto foi testado com login real nem num iPhone de verdade.
