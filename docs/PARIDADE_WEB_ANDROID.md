# Paridade web → Android

**Regra (Thiago, 22/11/2026): tudo que for criado na web precisa ser levado para o Android.** Este arquivo é a fila. Quem cria algo na web **escreve a linha aqui na mesma sessão**; quem trabalha no Android **lê este arquivo primeiro** e leva o que estiver como ❌. Quando levar, troque o status e anote o commit.

Legenda: ✅ já tem no Android · ❌ falta levar · 🔎 conferir se já existe (dar grep antes de criar, ver `docs/PROCESSOS.md`) · 🚫 não se aplica ao Android.

**Antes de criar função no `:shared`, procure se já existe** (quase dupliquei `PlateCalculator`/`warmupSets`, e depois `nextProgression`, que o `LoadSuggester` já cobria).

Onde ficam as regras: web em `web/src/insights.ts`, `superset.ts`, `tempo.ts`, `wellness.ts`, `recap.ts`; Android em `shared/.../presentation/body/Insights.kt` (+ testes em `commonTest`). Compilar/testar pelo terminal real: `tools/run-shared-tests.ps1` e `tools/build-install.ps1`.

## Treino
| Funcionalidade (web) | Android | Notas |
|---|---|---|
| Supersérie / bi-set / tri-set / circuito (sem descanso até o fim da rodada, "Agora: próximo") — #575 | ⚠️ ver `CONTEXTO_SESSAO.md` (porte em andamento em 22/11) | Web guarda a ligação no aparelho (`gt-ss-<treino>`); migração 017 prepara o banco |
| Cadência `3-1-2-0` opcional e TUT por série — #581 | ⚠️ ver `CONTEXTO_SESSAO.md` (porte em andamento em 22/11) | Web: `tempo.ts`; migração 017 |
| Sugestão de carga (+2,5/+5 kg ao bater reps) — #559 | ✅ `LoadSuggester` (mais completo) | Não criar de novo |
| Aquecimento sugerido por exercício — #579 | 🔎 | Android tinha `warmupSets`; conferir se aparece no treino |
| Nota por exercício (banco, pegada) — #582 | ❌ | Web: só no aparelho (`gt-note-<id>`) |
| Descanso próprio por exercício — #576 | ❌ | Android lembra um descanso único; falta por exercício |
| Modo foco (um exercício por vez) — #584 | ❌ | |

## Home
| Funcionalidade (web) | Android | Notas |
|---|---|---|
| Painel "Hoje" (carga da semana, descanso, aviso de descarga) — #561/#560 | ✅ `500e220` | |
| Resumo da semana (sex–dom) — #585 | ❌ | Só cartão; notificação depende de push |
| Atalhos de grupos favoritos — #583 | ❌ | O Android tem linha "Atalhos" fixa (Grupos/Histórico), não favoritos |
| Sono e energia do dia — #587 | ❌ | Só no aparelho |

## Evolução / Análises
| Funcionalidade (web) | Android | Notas |
|---|---|---|
| Equilíbrio empurrar×puxar e quadríceps×posterior — #557 | ✅ | `Insights.kt` |
| Platôs — #558 | ✅ | Falta "variações sugeridas" (web: `variationsFor`) ❌ |
| Recordes por faixa 1/5/10RM — #555 | ✅ | |
| Comparar dois treinos — #573 | ✅ | |
| Pesquisa no histórico — #577 | ✅ | |
| Exportar CSV — #565 | ✅ | Já existia (`CsvExport`) |
| Mapa de calor — #574 | ✅ | Já existia (`TrainingHeatmap`) |
| Comparativo semana atual × anterior (treinos, séries, volume) | ❌ | Web: `compareWeeks` |
| Calorias do mês (estimativa, MET 5) — #556 | ❌ | Web: `estimateKcal` (já está em `Insights.kt`, falta a tela) |
| Volume por exercício (8 semanas) — #580 | ❌ | |
| Séries por equipamento — #590 | ❌ | |
| Intensidade (RPE médio) e fadiga — #591 | ❌ | Android tem `RpeStore`; ligar |
| Energia × desempenho — #587 | ❌ | Depende do registro de energia |
| Meta de peso com projeção de data — #586 | ❌ | |

## Conquistas
| Funcionalidade (web) | Android | Notas |
|---|---|---|
| Selos do mês (treinos, dias, volume) — #564 | ❌ | |
| Desafio pessoal de 30 dias — #592 | ❌ | |

## Social
| Funcionalidade (web) | Android | Notas |
|---|---|---|
| Feed com reações e legenda — #533 (parte) | ✅ | O app já tinha tudo isso |

## Como atualizar
1. Criou algo na web? Acrescente a linha na tabela certa com ❌.
2. Levou para o Android? Troque por ✅ e ponha o hash do commit.
3. Mencione no card do Runtime ("Android: pendente/feito").
