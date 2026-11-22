# DESIGN.md — GymTracker

Guia de design do app Android. Adaptado do `Runtime/docs/DESIGN.md` (Kanban) ao Compose, mais o que aprendemos na
auditoria de 21/11/2026 (`AUDITORIA_DESIGN.md`) e na pesquisa de UX de treino. Referências: `REFERENCIAS_DESIGN.md`.

## 1. Princípios
1. **Treinar primeiro.** Em treino a pessoa está suada, com uma mão e pouco tempo. Nada decorativo atrapalha ler ou registrar uma série. Efeitos vivem no *fundo*, nunca no conteúdo.
2. **Tudo é token.** Componentes só leem `MaterialTheme.colorScheme`, `typography`, `shapes` e `LocalThemeExtras`. Trocar de tema não exige tocar em tela.
3. **Toda ação principal tem a cor de marca** (`primary`). Nada de `primaryContainer` apagado no botão que inicia o treino.
4. **Uma pergunta por tela, em 3 segundos:** dá para dizer para que serve a tela? A próxima ação está visível?
5. **O app lembra por você:** pré-preenche carga/reps da última sessão, sugere o próximo treino, retoma o que ficou aberto.

## 2. Tokens por tema
`ThemeSpec` = `colors` (ColorScheme) + `typography` + `shapes` + `extras` (decor, gradiente, borda, sombra dura, brilho, `success`, `homeLayout`, `workoutLayout`).
- **Contraste, conferido por teste** (`ThemeContrastTest`): texto sobre fundo ≥ 4,5:1 (alvo 7:1), texto do botão de destaque ≥ 4,5:1, cor de apoio ≥ 3:1. Tema que reprova não entra no seletor até ser corrigido.
- Cada tema tem **o seu acento**. Sem verde-limão/ciano "padrão" repetido em vizinhos.
- Cada tema escolhe as **fontes** quando combina com o caráter (não confiar só em Inter/Roboto).
- Escuro e claro: cada tema declara `isDark`; a barra do sistema acompanha.

## 3. Movimento
- Estado/toque: 150–200 ms. Entrada de diálogo/sheet ≤ 200 ms; **saída mais sutil** (≈ 120 ms).
- Curva `FastOutSlowIn`/`cubic-bezier(.23,1,.32,1)`. Sem bounce em ação utilitária. Nunca partir de escala 0.
- Press: escala 0,97. Animar só `translate/alpha/scale`.
- Mudança de altura de cartão: `animateContentSize()` (a sugestão de carga aparecendo não pode empurrar o botão de repente).
- Um único relógio para decoração animada; **só o tema/elemento visível anima**. Respeitar "reduzir animações" do sistema.
- **Nunca:** indicador pulsando sem motivo, stagger em lista utilitária, animar conteúdo estático.

## 4. Anti-genérico (regras do projeto)
- **Nada de faixa colorida à esquerda do cartão.**
- No máximo 2 níveis de contêiner (tela → cartão). Chips e tags não viram caixas dentro de caixas.
- Ícones de traço único (Material Symbols/Icons filled ou outlined, sem misturar), sem emoji como ícone.
- Botão destrutivo (apagar/remover) **nunca fica sempre à vista**: vai em menu ⋮ ou exige confirmação em dois passos.
- Nomes de grupo/exercício mostram em caixa de título (`prettyGroupName`), mesmo que o dado venha em CAIXA ALTA.

## 5. Estrutura e estados
- **Home:** (1) aviso "Treino em andamento · Retomar" se houver; (2) cartão **Próximo treino** (rotação); (3) meta/streak/último; (4) frequência; (5) atalhos.
- **Treino ativo:** alvo de toque grande (≥ 48 dp), ações principais ao alcance do polegar (parte de baixo), −1/+1 e −2,5/+2,5 para ajuste rápido, sugestão de carga, timer de descanso que inicia ao registrar.
- **Estados sempre desenhados:** carregando (esqueleto, não spinner solto), vazio (com a **próxima ação**: "Adicionar exercícios"), erro (com "tentar de novo"), sem conexão (faixa).
- **Treino esquecido:** >1 h sem série ⇒ encerra no horário da última série (ou apaga se não houve série). Diálogo de retomada diz há quanto tempo começou.
- **Aparência:** grade com miniatura da tela em cada tema, filtro por categoria, aplicação na hora; só o selecionado anima.

## 6. Componentes (OriginKit → Compose)
Os do OriginKit são web; portamos a **ideia** com código próprio (cache em `Runtime/docs/originkit-cache/`).
| OriginKit | Uso no GymTracker | Estado |
|---|---|---|
| `rolling-counter` / Cult UI `rolling-number` | números da Home (meta/sequência) com a mola do original | ✅ |
| `gooey-delete-confirm` | apagar treino/grupo: lixeira que abre "Confirmar / Cancelar" | ⏳ |
| `direction-aware-tabs` (Cult UI) | abas do Social com direção | ✅ |
| `selenite-chips` | filtros multi-seleção (histórico por grupo) | ⏳ |
| `like` | reação do feed (estalo, anel e 12 partículas) | ✅ |
| `holo-flip-badge` | selos de Conquistas | ⏳ |
| `hyaline-level` / `wake-slider` | meta semanal e RPE da série (#350) | ⏳ |
| `stretch-pill-toggle` | interruptores (modo circuito, privacidade) | ⏳ |

## 7. Checklist de entrega
- [ ] Olhei a **tela real no emulador** (não só o código), em pelo menos 2 temas de layouts diferentes?
- [ ] Contraste ok (teste de tema)? Texto legível sobre a decoração?
- [ ] Alvos de toque ≥ 48 dp? Dá para usar com uma mão?
- [ ] Tem estado de carregando, vazio e erro?
- [ ] A ação principal está na cor de marca e visível sem rolar?
- [ ] Nada destrutivo exposto sem confirmação?
- [ ] O que adicionei é o que a ocasião pede? (cortar o resto)

## 8. Fontes de pesquisa (21/11/2026)
- Seletor de tema com miniaturas da tela: <https://digitalbiztalk.com/article/building-a-theme-picker-for-app-onboarding-a-2026-developer-guide>
- Alvos de toque: <https://www.nngroup.com/articles/touch-target-size/>
- Uso com uma mão: <https://smashingmagazine.com/2020/02/design-mobile-apps-one-hand-usage/>
- Android — temas: <https://developer.android.com/design/ui/mobile/guides/styles/themes>

## 9. Regras herdadas do Vault (notas `nt-design-*`, `nt-processo-*`, `nt-ref-*`)
Fonte: `Vault/seed-out/seed.json` (46 notas, 254 itens). Resumo do que vale aqui:
- **Portar fiel, não reinventar.** Para cada componente do OriginKit: ler o original em `Runtime/docs/originkit-cache/`, extrair técnica/estrutura/parâmetros/entradas, e manter o **miolo** (física, constantes, curvas) idêntico; trocar só a casca (React → Compose) e as cores (tokens do tema). Comparar lado a lado antes de dar por pronto. Sem acesso ao original, não fazer.
- **Cache primeiro:** ler `originkit-cache/` antes de gastar cota (Free = 3 `get`/dia). Quem baixar novo componente salva no cache na hora.
- **Poucos controles** para o usuário; o resto é decisão do componente.
- **Claro e escuro sempre.** No escuro a luz é aditiva; no claro mistura sobre o fundo.
- **Reduzir movimento:** respeitar a configuração do sistema; quadro parado já "desenvolvido".
- **Movimento (valores):** UI 150–200 ms, entrada/saída < 300 ms; curva `cubic-bezier(.23,1,.32,1)` (entrada), `(.77,0,.175,1)` (reveal); spring padrão duração .45 sem bounce (`bounce .1` só em contexto lúdico); press `scale(.97)`; nunca partir de escala 0 (usar .9 + alpha); entrada = alpha 0→1 + deslocamento ≈ 8 dp; **saída mais sutil**; popover cresce do gatilho; sombra de hover em camadas, sem borda dura.
- **Não animar:** ação por teclado, interação de altíssima frequência, conteúdo estático.
- **Anti-genérico:** indicador estático (não pulsa); blur só em hero/diálogo; `scale(1.05)` só no CTA principal; sem stagger em lista utilitária; sem bounce em menu/toggle/diálogo; variar tempo/curva por tipo; no máx. 2 níveis de contêiner; fonte escolhida para a marca; acento definido **antes** de gerar.
- **Verificar de verdade:** olhar as capturas (claro+escuro) e conferir o contraste por teste; depois de publicar, conferir o artefato real.
- **Levar para outro projeto:** copiar este arquivo, `REFERENCIAS_DESIGN.md` e o cache do OriginKit.

## 10. Todas as referências → o que serve ao GymTracker (revisado em 21/11/2026)
Lido: `Runtime/docs/REFERENCIAS_DESIGN.md`, notas do Vault, caches `originkit-cache`, `cultui-cache`, `skiper-cache`, `moxen-cache` (880 itens), dados do repositório UI UX Pro Max. Prioridade: 🔴 alta · 🟡 média · ⚪ baixa/não se aplica.

| Referência | O que tem de útil para um app de treino | Ação |
|---|---|---|
| **UI UX Pro Max** | Para *Fitness/Gym App*: estilo **Vibrant & Block-based + Dark Mode (OLED)**, laranja energético (`#F97316`/`#FF6B35`) + verde de sucesso (`#22C55E`) em fundo escuro, efeitos **anéis de progresso animados + desbloqueio de conquistas**, anti-padrão "design estático + sem gamificação", obrigatório: acompanhar progresso e planos de treino. Guia **Jetpack Compose** (estado elevado, `key` em Lazy, sem `fillMaxSize` por padrão, `semantics`/`contentDescription`) e regras de interface de app (alvo 48 dp, 8 dp entre toques, **sem ação só por gesto**, esqueleto >300 ms, respeitar reduzir movimento). 🔴 | Reforçar gamificação (anel da meta animado, desbloqueio de selo); ação de apagar sempre com botão visível; esqueletos |
| **Design Motion Principles** | Valores concretos (§9), checklist anti-genérico, auditoria de movimento 🟡 | Auditar durações/curvas do app contra a tabela |
| **Skiper UI** (dynamic island, 12 grátis em `skiper-cache`) | **Ilha dinâmica** = ótimo para o **timer de descanso** flutuante que não some ao rolar 🔴 | Portar o conceito (estados por tamanho, mola 400/30) como ilha do descanso |
| **Cult UI** (`cultui-cache`) | `rolling-number` (mola massa .8/rigidez 75/amort. 15, 43 linhas): números da Home 🔴 · `direction-aware-tabs`: abas do Social/filtros 🟡 · `expandable`: cartão de exercício que expande 🟡 · `popover-form`: adicionar exercício crescendo do botão 🟡 · `sortable-list`: reordenar grupos (com botões Mover, não só arrastar) 🟡 · `halo-progress`/`halo-segmented` 🟡 · `timer`: mono tabular + `role=timer` 🟡 · `border-beam`: feixe só ao bater recorde (2 s), não contínuo ⚪ · `kanban-board`, `fluted-glass` ⚪ | Portar fiel o miolo: rolling-number, direction-aware-tabs, sortable-list |
| **Moxen** (`moxen-cache/INDICE.md`) | **Deshacer** (toast "Desfazer" com contagem) para apagar treino/série 🔴 · **Esqueleto/Demora/Reintento** (carregando, sem piscar, erro com tentar de novo) 🔴 · **Calibre** (campo numérico arrastável como régua) para carga/reps 🟡 · **Esfera** (dial de temporizador) p/ descanso 🟡 · **Segmentos** (controle segmentado com números que rolam) 🟡 · **Confort** (acessibilidade com prévia ao vivo) 🟡 · **Duermevela** (claro/escuro/sistema) 🟡 · **Sintonía** (troca de tema revelada em círculo) p/ o seletor 🟡 · **Insignia** (selo que salta com mola) p/ contadores 🟡 · **Faceta** (filtros com contadores) p/ Histórico 🟡 · **Refrendo** (digitar o nome para apagar) p/ grupo com exercícios 🟡 · **Pila** (toasts empilhados) ⚪ | Começar por Deshacer + Esqueleto/Reintento |
| **OriginKit** (`originkit-cache`) | `rolling-counter`, `gooey-delete-confirm`, `bouncing-dot-tabs`, `selenite-chips`, `like`, `holo-flip-badge`, `hyaline-level`, `wake-slider`, `stretch-pill-toggle` (tabela §6) | Ver §6 |
| **Refero Styles** | Tokens reais de produtos: Raycast, Superhuman, Slush, teenage engineering, Reflect, Monopo; "regras" além de tokens; coleções de telas de dashboard/settings 🟡 | Criar 2–3 temas profissionais novos (Comando, Pergaminho, Industrial) com tokens lidos nas páginas; ler estilos de apps de fitness |
| **Shader Gradient** (`shadergradient-cache`) | Gradientes animados em shader: `RuntimeShader` AGSL (Android 13+) com fallback `Brush` estático ⚪ | Opcional, só como fundo de Aurora/Ártico; baixa prioridade (custo de bateria) |
| **Awesome Claude Design** | Famílias estéticas: **Data-Dense Pro** (números tabulares, gráficos como herói) para Evolução 🟡; **Playful Color** (Duolingo: streak, comemorações) para gamificação 🟡 | Aplicar nas telas Evolução e Conquistas |
| **taste (Hmbown / Leonxlnx)** | Processo: aterrar no artefato real, checagem pré-entrega, anti-genérico 🟡 | Já no checklist §7 |
| **shadcn** | Acessibilidade e teclado de componentes base ⚪ (app é toque) | Só `semantics`/rótulos de acessibilidade |
| **Cult UI / Skiper (web-only)**: hover, cursor, scroll-reveal | ⚪ não se aplicam a toque | — |

### Já portado/aplicado (21/11, noite)
rolling-number (Cult UI) · ilha do descanso (Dynamic Island, Skiper/Cult UI) · Deshacer/Esqueleto (Moxen) · retomar/descanso automático (UX de treino) · mapa muscular e retrospectiva (UI UX Pro Max: gamificação) · 3 temas do Refero (Raycast, Superhuman, teenage engineering).

### Fila de implementação (ordem)
1. Seletor de temas em grade + teste de contraste (feito/em andamento).
2. Finalização automática de treino esquecido (feito).
3. **Deshacer** ao apagar treino + **esqueletos/erro com "tentar de novo"** na Home/Histórico.
4. **Rolling number** na Home e no resumo do treino; anel da meta animado.
5. **Ilha do descanso** (timer flutuante) no treino ativo.
6. Abas do Social com direção; filtros do Histórico com contadores.
7. Reordenar grupos (alça + Mover cima/baixo).
8. 2–3 temas novos baseados no Refero.
