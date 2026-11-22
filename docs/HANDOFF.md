# HANDOFF — GymTracker
status: PENDENTE
atualizado: 2026-11-22 (fim da noite)

> Como funciona: o Thiago digita `/handoff` → a sessão atualiza este arquivo, commita, marca `status: PENDENTE` e limpa a conversa.
> Na sessão nova, **leia este arquivo primeiro**, troque o status para `RETOMADO` (com data) e continue o trabalho sem pedir confirmação.

## Autorizações (valem sem pedir de novo)
- **Pode:** commit e push no branch `claude/remover-leitura-notificacoes`; usar o emulador (AVD gym35, já aberto); criar funcionalidades e melhorias por conta própria; comentar e mover cards no Runtime (até "Revisão").
- **Não pode:** aplicar migração no banco (só escrever o SQL e listar em `docs/MIGRACOES_PENDENTES.md`); deploy na Vercel (o Thiago decide); digitar senha; mover card para "Concluído" antes de conferido.
- Português do Brasil, relatório curto e honesto (dizer o que NÃO foi testado).

## Leia, nesta ordem
1. `GymTracker/CLAUDE.md`
2. `GymTracker/docs/CONTEXTO_SESSAO.md` (final do arquivo: o que foi feito e o que falta)
3. `GymTracker/docs/PROCESSOS.md` (compilar, verificar, regras que custaram caro, Vault)
4. Se mexer em interface: `GymTracker/docs/DESIGN.md` (e o Vault/OriginKit para componentes: porte fiel)
5. Web: `GymTracker/docs/WEB_APP.md` e `GymTracker/web/README.md`
6. Social e banco: `GymTracker/docs/SOCIAL_DESIGN.md`, `GymTracker/docs/MIGRACOES_PENDENTES.md`
7. Memória do projeto: `projeto-gymtracker-22-11` (carregada pelo índice MEMORY.md)

## Como trabalhar
- **Fila:** board Runtime, projeto GymTracker (API `https://obmbjszdfvwhajgvkfpi.supabase.co/functions/v1/api` com `$RUNTIME_TOKEN`; ações `list_tasks`, `create_task`, `comment`, `move_task`; coluna com acento: "Revisão"). Comentar o que fez e o que NÃO foi testado.
- **Antes de criar função no `:shared`, dar grep** (já existia `GymMath`/`PlateCalculator`; quase dupliquei).
- **Android:** compilar pelo terminal real (`mcp__terminal__run_in_terminal` + script `.ps1` que roda `.\gradlew.bat :app:assembleDebug`, instala com `adb install -r` e abre o app), conferir a tela no emulador (`adb exec-out screencap -p`). O Bash NÃO roda Gradle; este PC NÃO tem `gh` (ver o CI na aba Actions do GitHub). Toque em tela: coordenadas do original 1080x2400 (a imagem exibida é 900x2000, multiplicar por 1,2). Evitar `KEYCODE_BACK` com teclado aberto (sai da tela).
- **Web (`GymTracker/web`):** `npm test`, `npm run build`, conferir no navegador com `http://localhost:5173/?mock` (preview `gym-web`, em `Claude_Code/.claude/launch.json`; se o servidor cair, `preview_start`).
- Heredoc/`node -e` com aspas quebra no Bash: para edições grandes use a ferramenta Write/Edit.
- Commits pequenos, em português, com a linha de coautoria; push a cada bloco; atualizar `docs/CONTEXTO_SESSAO.md` no fim de cada bloco.
- Treinos de teste criados no emulador: apagar pelo Histórico (⋮ → Apagar treino).

## Onde paramos (22/11/2026)
**Web PWA** (`GymTracker/web/`, 135 testes): login, Início, Treino (aviso de descanso com bip, aviso de novo recorde, RPE local), Histórico (filtro multi-seleção, anotação do treino), Grupos/catálogo, Evolução (mapa muscular, retrospectiva mês/ano e "Meu ano", níveis de força), Recordes, Conquistas, Mais (backup/importar catálogo, meta semanal, ferramentas: anilhas/aquecimento/tabela de repetições, 9 temas), compartilhar imagem e **foto com estatísticas**.
**Android:** SeleniteChips no Histórico (#536), widget em 3 tamanhos (#540), tela "Meu ano" (#539), "Foto com estatísticas" no resumo (`ui/workout/PhotoShare.kt`), CI com testes do `:app` (continue-on-error) e job da web (#541).
**Social:** migração `docs/supabase/016_foto_no_post.sql` escrita (bucket privado `post_photos`, `feed_posts.photo_path`), **NÃO aplicada**; o app ainda não envia a foto ao feed.

## Aberto / próximas ideias
- Conferir o resultado do CI na aba Actions do GitHub; se os testes do `:app` estiverem verdes, tirar o `continue-on-error` do passo.
- Quando o Thiago aplicar a 016: código do app para enviar o JPEG ao bucket, gravar `photo_path` e mostrar a foto no feed por URL assinada (card "Social: foto do treino no post do feed", em "A fazer").
- #538 importar histórico do backup (precisa de função no servidor, esboço 015); Social e push na web (#533, #534); medidas na web (depende da migração 013); #446 imagens de exercício.
- Ideias novas para implementar sem pedir: Social (compartilhar treino com foto no feed, reações, desafios), gamificação (metas, selos), evolução (gráficos, comparativos), polimento de design e acessibilidade, testes que faltam.
- Não testado ainda: foto real no celular, Instagram/WhatsApp de verdade, iPhone/Safari (PWA), dados reais do Supabase na web, Web Share, widget compacto/médio e com cronômetro.
