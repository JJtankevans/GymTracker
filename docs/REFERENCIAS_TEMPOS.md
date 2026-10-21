# Tempos de referência (descanso e duração da série)

Usados no app só como **ponto de comparação**, não como prescrição. O app aprende o SEU ritmo; estes valores servem para você se comparar.
Pesquisado em 20/10/2026 (resumos de páginas, não li os artigos completos; confira antes de tratar como regra).

## Descanso entre séries (por objetivo)
| Objetivo | Faixa usada no app | Base |
|---|---|---|
| Força / potência | 120–300 s | Guias de programação (2–3 min, até 5 min) [open-exam-prep, sem citar fonte primária] |
| Hipertrofia | 60–180 s | Ver nota abaixo: a literatura recente favorece descansos mais longos |
| Resistência muscular | 30–60 s | Mesmo guia de programação |

**Nota sobre hipertrofia.** A ideia antiga de "hipertrofia pede descanso curto" não se sustenta bem:
- Henselmans & Schoenfeld (2014), *Sports Medicine* 44:1635–1643: nenhuma evidência de que hipertrofia exija descanso menor que força; nenhum estudo de longo prazo achou mais crescimento com descanso curto. https://link.springer.com/10.1007/s40279-014-0228-0
- Schoenfeld et al. (2016), *J Strength Cond Res* 30(7):1805–1812: 21 homens treinados, 8 semanas, 3 séries de 8–12RM; 3 min de descanso superou 1 min em força (agachamento ~15% vs ~8%) e em espessura do quadríceps (~13% vs ~7%). Custo: sessão quase dobra de duração. https://brookbushinstitute.com/article/longer-interset-rest-periods-enhance-muscle-strength-hypertrophy-resistance-trained-men
- McMahon et al. (JSCR, ~2023–24): estudo agudo pequeno (8 pessoas, isométrico de joelho); 5 min teve ~15% mais volume que 2 min. Os autores mesmos dizem que é especulativo. https://researchonline.ljmu.ac.uk/id/eprint/22797/1/McMahon_etal_InterSetRest_JSCR_2024_Accepted.pdf

Por isso o app mostra a faixa de hipertrofia como 60–180 s e deixa o usuário escolher, em vez de impor um número.

## Duração da série (tempo sob tensão)
- Schoenfeld et al. (2015): repetições de 0,5 a 8 s produziram hipertrofia semelhante; reps muito lentas (>10 s) tendem a ser menos eficazes (via muscleandmotion.com, resumo). https://www.muscleandmotion.com/tension-hypertrophy/
- Regra prática usada no app: **2–4 s por repetição** → 8–12 reps ≈ 25–50 s por série. É só o valor inicial, até o app aprender o seu ritmo real.

## Como o app usa isso
1. Antes de ter dados, usa o ritmo padrão (3 s/rep) e o descanso do objetivo escolhido.
2. Com dados, usa o **seu** ritmo (mediana dos últimos 10) e mostra o comparativo com a faixa de referência.
3. Autorregulação: a literatura não mostra vantagem em descanso fixo versus ajustado pelo usuário (Henselmans & Schoenfeld 2014), então ajustar pelo seu ritmo é coerente.

Os valores usados no app estão em `shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/presentation/circuit/ReferenceTimes.kt`.
