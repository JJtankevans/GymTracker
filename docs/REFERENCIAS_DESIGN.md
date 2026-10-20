# Referências de design — GymTracker

Lista dos sites/repos que você passou, com o que cada um é e como aproveitar num app Android (Compose).
A maioria é web (React/Tailwind), então o aproveitamento é de **ideias, paletas, tipografia e motion**, não de código.

> Pesquisado em 20/10/2026. Os itens marcados com ⚠️ têm nome ambíguo; confira se é o que você tinha em mente.

---

## Inspiração visual / design systems

### Refero Styles — https://styles.refero.design
Biblioteca com 2.000+ sistemas de design extraídos de produtos conhecidos. Cada estilo traz screenshots, paleta, tipografia, espaçamento, componentes e um **DESIGN.md** que ferramentas de IA (Claude Code, Cursor, v0) conseguem ler.
**Uso no app:** escolher 1–2 estilos, copiar paleta + tipografia pro `ui/theme` (Color.kt / Type.kt) e guardar o DESIGN.md escolhido no repo como guia pra eu e você seguirmos.

### Shader Gradient — https://shadergradient.co
Gradientes animados (Framer, Figma e React). Não achei suporte a mobile/React Native.
**Uso no app:** só como inspiração de fundo. Em Compose dá pra aproximar com `Brush` (linear/radial/sweep) animado ou `RuntimeShader` (AGSL, Android 13+) para Home/tela de treino ativo. Fallback com gradiente estático abaixo do Android 13.

### Skiper UI — https://skiper-ui.com
Componentes "incomuns" para Next.js em cima do shadcn/ui, com Motion.dev e Tailwind (24+ grátis, 54+ premium, pago a partir de US$129). Destaques: dynamic island, image reveal, tooltip estilo Vercel, listas com drag/scroll, tela de login animada.
**Uso no app:** inspirar o **timer de descanso como "ilha" flutuante**, o login animado e microinterações de lista.

### Cult UI — https://www.cult-ui.com
154 componentes animados open source (MIT) para shadcn/ui: Dynamic Island, mockups, cards, gráficos, Kanban.
**Uso no app:** mesma ideia do Skiper; bom pra ver como organizar cards de treino/estatísticas.

### OriginKit — https://www.originkit.dev/
50 componentes animados (MIT) para React/Next/Vite/Framer, inseridos por MCP. A chave grátis limita a 10 consultas/dia.
**Uso no app:** só referência de animação/estilo.

### ⚠️ "shadcn expanded"
Não encontrei nada com esse nome exato. Os mais próximos:
- **shadcnui-expansions** (hsuanyi-chou) — https://shadcn.io/template/hsuanyi-chou-shadcn-ui-expansions
- **shadcn-vue extended** — https://extended.shadcn-vue.com (registry comunitário, não é dependência: o código é copiado pro projeto)
- **shadcn extension** (BelkacemYerfa) — https://docs.21st.dev/@BelkacemYerfa/library/shadcn-extension

Se for outro, me manda o link.

---

## Skills / guias pra IA (úteis pra eu seguir ao desenhar telas)

### UI UX Pro Max — https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
Skill com 79 estilos visuais, 192 paletas, 74 combinações de fontes, 25 tipos de gráfico, 119 diretrizes de UX e 22 stacks, **incluindo Jetpack Compose, SwiftUI, React Native e Flutter**. Instalação: `npm i -g ui-ux-pro-max-cli` e `uipro init --ai claude`, ou `/plugin marketplace add nextlevelbuilder/ui-ux-pro-max-skill`. Os scripts de busca pedem Python 3.
**É o mais aplicável ao GymTracker** por cobrir Compose.

### Design Motion Principles — https://github.com/kylezantos/design-motion-principles
Skill para **criar ou auditar animações**, combinando três linhas: Emil Kowalski (contenção e velocidade), Jakub Krehel (acabamento de produção) e Jhey Tompkins (experimentação criativa). Instalação: `npx skills add kylezantos/design-motion-principles`.
**Uso no app:** critério para as animações de transição, do timer e de feedback ao registrar série.

### Awesome Claude Design — https://github.com/rohitg00/awesome-claude-design
Lista curada sobre o Claude Design: DESIGN.md por estilo visual, prompts, cookbooks, receitas de remix, skills e plugins.
**Uso no app:** fonte de DESIGN.md prontos pra partir de um estilo.

### ⚠️ "tasteskill"
Achei dois candidatos:
- **taste-skill** (Leonxlnx) — skill "anti-slop" pra frontend, evita visual genérico de IA: https://www.ui-skills.com/skills/leonxlnx/taste-skill
- **taste** (hmbown) — skill de julgamento/qualidade para código, UI, docs; manda consultar a interface existente em vez de improvisar: https://github.com/hmbown/taste

O primeiro parece mais o que você quis dizer.

---

## Como pretendo usar no GymTracker

1. Escolher uma direção (ex.: escura, alto contraste, acento forte) a partir do Refero + UI UX Pro Max.
2. Passar paleta e tipografia pra `ui/theme/`.
3. Aplicar os princípios de motion nas transições, no timer e no registro de série.
4. Registrar a direção escolhida em `docs/DESIGN.md` para as próximas sessões.
