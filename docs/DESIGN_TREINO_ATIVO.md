# Estudo de design: tela de treino ativo (30/10/2026)

Pedido do Thiago: "no modo louco está legal, mas pode melhorar ainda; estude novos modos de design para a tela de treino ativo". Este arquivo guarda o raciocínio. **Nada foi visto no aparelho**: é código novo, compilado e testado só no CI.

## O que existia
- **Lista** (`LIST`): todos os exercícios numa lista rolável, com campos de texto.
- **Foco** (`FOCUS`, o que quase todos os temas usam): um exercício por vez (pager), dois cartões de número (reps e kg) com + e -, botão REGISTRAR, chips de descanso, anel do descanso e as séries feitas. No **Insano** ganha o halter levantando, confete mais forte e glitch nos temas Neon/Terminal.
- O estilo vinha **preso ao tema** (campo `workoutLayout`): quem gosta de um tema não escolhia o desenho da tela.

## O que atrapalha na academia (lendo o código do Foco)
1. **O botão principal fica no meio da tela**, e o resto (descanso, séries feitas) empilha embaixo: é preciso rolar com a mão suada. A zona do polegar é o terço de baixo.
2. **O descanso aparece abaixo do botão**, só depois de registrar, empurrando o layout. Tem que rolar para ver o tempo.
3. **O progresso das séries é só texto** ("Série 2 de 4"). Não dá para bater o olho.
4. **Nada compara a série que você está montando com a última vez ou com o recorde.** O app sabe os dois números e não mostra.
5. **Só mais e menos** para mudar valor: muitos toques para saltar 10 kg.
6. **O Insano é decoração**: o halter sobe e desce igual, aconteça o que acontecer. Nada reage ao que você faz.

## Princípios adotados
- Tudo que importa **numa tela só, sem rolar**, e o botão principal **embaixo**.
- **A tela responde ao que você faz**: registrar dá um "soco" nos números (maior no Insano), a série atual pulsa, a comparação muda ao mexer na carga.
- **Gestos como atalho, botões como garantia**: arrastar o número é rápido, mas os botões continuam lá.
- O estilo é **escolha da pessoa** (Aparência → Estilo do treino), separado do tema; "Automático" mantém o comportamento antigo.

## Os estilos novos
### Arena (`ARENA`)
Layout de painel de jogo, sem rolagem.
- Em cima: nome do exercício, **bolinhas de série** (cheias = feitas, a atual pulsa, vazias = faltam) e "Última: 40 kg × 12 · Recorde: 50 kg".
- No meio: **REPS e KG gigantes**. Arrastar o número para cima ou para baixo muda (1 rep ou 2,5 kg por passo, com vibração); + e - continuam. Embaixo, a **comparação ao vivo**: "+2,5 kg vs. última", "Igual à última vez" ou "Recorde! +2,5 kg sobre 50 kg".
- Embaixo (zona do polegar): **REGISTRAR SÉRIE**, que ao registrar **vira a barra de descanso** (enche/esvazia com o tempo, tocar pula). Depois, os chips de descanso e as séries feitas numa faixa só.
- Insano: o soco nos números é maior e as bolinhas pulsam mais rápido.

### Anel (`RING`)
Um instrumento no centro da tela.
- **Anel segmentado**: um segmento por série (feitas acesas, a atual pulsando). No descanso, um **anel interno** faz a contagem regressiva e o centro mostra o tempo.
- **Tocar no meio registra** a série (ou pula o descanso): alvo enorme, uma mão só.
- Abaixo: reps e kg em mini-controles, comparação ao vivo, chips de descanso e séries feitas.
- Insano: o anel pulsa mais rápido e o "soco" é maior.

### Foco, melhorado
O Foco ganhou a mesma comparação ao vivo (última vez / recorde). O resto ficou igual.

## Ideias que ficaram de fora (e por quê)
- **Diário** (uma linha por série, com a última vez ao lado): só guardamos a última série de cada exercício, não a de cada número da série. Precisaria de uma consulta nova.
- **Pilha de cartões** e **modo espelho do relógio**: dependem de decisões de produto (o treino do celular e do relógio ainda são independentes).
- **Insano reagindo ao esforço** (halter mais rápido quanto mais perto do recorde): boa ideia, depende de ver o Arena/Anel no aparelho antes.

## Onde está no código
- Escolha: `ThemePreferences.workoutStyle`, `LocalWorkoutStyle` (`ThemeSpec.kt`), cartão em `ThemePickerScreen.kt`.
- Telas: `ui/workout/WorkoutStylePages.kt` (`ArenaPage`, `RingPage`, peças compartilhadas), escolhidas em `FocusWorkout.kt` (o pager, a faixa de chips e o "+" de exercício extra são os mesmos).
- Regra da comparação: `presentation/workout/SetComparison.kt` (testada).

## Para avaliar no aparelho
Ver `docs/ROTEIRO_TESTE.md`, seção "Treino ativo". O que mais pode precisar de ajuste: altura em telas pequenas (o Arena não rola), sensibilidade do arrastar e o tamanho do anel.
