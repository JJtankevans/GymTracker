# Ranqueada (liga com elos) e x1 — desenho e base (31/10/2026)

Pedido do Thiago: "algum tipo de matchmaking ranqueado, criar elos como no LOL ou Valorant, uma porcentagem do topo do rank sobe e outra desce, e um cara poder fazer x1 com outro do rank dele". Esta é a **base** funcionando de ponta a ponta (banco, regras, app). **Nada foi visto no aparelho**; o SQL foi testado num Postgres local com dados sintéticos, e as regras têm testes que dão os mesmos números do SQL.

## Ideia em uma frase
Treinar de verdade dá **pontos da semana**; você disputa a semana numa **turma de até 30 pessoas do seu elo**; no domingo o topo sobe, o fundo desce, e o seu **RP** (pontos de rank) define o **elo**. Além disso, dá para marcar **x1** de 7 dias com alguém do mesmo elo.

## Elos
Ferro, Bronze, Prata, Ouro, Platina, Diamante: **3 divisões (III, II, I) de 100 RP** cada (300 RP por elo). **Lenda** (a partir de 1800 RP) é o topo, sem divisões, e o RP continua subindo. Todo mundo começa em Ferro III com 0 RP. O elo sai só do RP: `tier = min(RP / 300, 6)`.

## De onde vêm os pontos (anti-trapaça)
Os pontos são calculados **no banco** a partir de `workouts` e `workout_sets`; o app não manda número nenhum.
- Treino terminado com **20 a 240 minutos** vale **100** + esforço (**1 ponto a cada 2 min além de 20, até 80 min: no máximo +30**) + **2 por série com repetição (até 20 séries)**. Máximo por treino: 170.
- **Só o melhor treino de cada dia conta**, e **no máximo 6 dias** por semana.
- **+50** por treinar em **3 dias ou mais** (constância) e **+50** por bater a **meta semanal do perfil** (em dias com treino).
- Treino com menos de 20 min não vale; mais de 240 min também não (provavelmente esquecido aberto).

Limite conhecido: dá para inflar um treino com séries de 1 repetição ou com o cronômetro aberto por 20 minutos. O teto por dia (170) e o limite de 6 dias seguram o estrago, mas não é à prova de má-fé. Ideias para endurecer: exigir carga ou tempo mínimo por série, comparar com o histórico da pessoa, denúncia.

## A semana (segunda a domingo, fuso de Brasília)
1. Na primeira consulta da semana, **todo mundo que já era da ranqueada entra numa turma do seu elo** (até 30; monta outra quando enche; ordem fixa por elo e id). Faltar não protege de cair.
2. A aba mostra a turma com os pontos de agora, a **posição**, a **faixa** (subida, segura, descida), quantos **pontos faltam** para a faixa de subida e quantos **RP** você ganharia ou perderia se fechasse agora.
3. No fechamento (quem abrir a aba depois do domingo dispara; é idempotente e usa trava), cada pessoa recebe:

| Posição na turma | RP |
|---|---|
| 1º | +40 |
| 2º até 20% do topo | +30 |
| até a metade de cima | +10 |
| metade de baixo | -10 |
| 20% do fundo | -25 |
| 0 pontos (não treinou) | -35 (sempre) |

Empate divide a posição. Turmas com menos de 5 pessoas não têm percentis: quem fez 100+ pontos ganha +10, quem treinou menos perde 20, e quem não treinou perde 35.
Contas em inteiro (`(tamanho * 20 + 99) / 100` para o 20%), para o SQL e o Kotlin darem o mesmo resultado.

## x1
- Valendo RP: só entre pessoas do **mesmo elo** (divisões diferentes podem). Amistoso: qualquer um que esteja no app.
- O pedido fica pendente; quem foi desafiado aceita (começa na hora e dura **7 dias**) ou recusa; quem desafiou pode cancelar um pedido pendente.
- Vence quem somar **mais pontos de treino** na janela (mesma conta da semana). Empate vale meio ponto.
- RP por **Elo, K = 24**: `delta = floor(24 * (resultado - esperado) + 0,5)`, com `esperado = 1 / (1 + 10^((RP do rival - meu RP) / 400))`, limitado a ±35. Vencer alguém mais forte rende mais, perder para alguém mais fraco custa mais. Dois zerados ou amistoso: 0.
- Só um x1 em aberto por dupla.

## Temporadas
8 semanas (a primeira começou em 26/10/2026). Na última semana, depois do fechamento normal, o **RP encolhe para 60%** (`rp * 6 / 10`), então todo mundo recomeça um pouco mais embaixo e o topo precisa ser reconquistado. O app avisa na semana 8.

## O que falta (futuro)
- **Recompensas da temporada**: moldura/título no perfil por elo máximo, selos de fim de temporada.
- **Elo visível no perfil e no feed** (hoje só aparece na aba e no placar da turma).
- **Fila automática de x1** ("buscar adversário do meu elo") em vez de escolher na turma.
- **Proteção de promoção** (alguns jogos de imunidade ao subir de elo, como no LOL) e **série de promoção** para mudar de elo.
- **Placar entre amigos e por grupo** (liga ranqueada de grupo).
- **Anti-trapaça mais forte** (ver acima) e **notificação** de "sua semana fechou", "x1 aceito".
- **Matchmaking por MMR escondido** separado do RP visível, se o RP virar desigual demais.

## Onde está no código
- Banco: `docs/supabase/018_ranqueada.sql` (tabelas `ranked_*`, funções `ranked_points`, `ranked_delta`, `ranked_duel_delta`, `ranked_join`, `ranked_board`, `ranked_settle_week`, `ranked_duel_*`, `ranked_my_duels`). Testada no Postgres local: 12 jogadores (pontos 550 a 0, empate em último), 65 jogadores (turmas 30/30/5), fim de temporada, x1 com aceite, vitória e amistoso, e as permissões (anônimo não executa).
- Regras e testes: `presentation/ranked/RankedRules.kt` e `RankedRulesTest.kt` (mesmos números do SQL), `BoardAnalysis.kt` (posição, faixa, pontos que faltam), `RankedViewModel.kt`.
- Tela: `ui/ranked/RankedScreen.kt` (aba "Ranqueada" do Social) e `RankEmblem.kt` (emblema desenhado por elo).
