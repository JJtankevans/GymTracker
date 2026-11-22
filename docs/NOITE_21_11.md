# Noite de 21/11/2026 — plano e fila (o Thiago dorme; autorizou tudo, sem perguntar, até ~4–5h)

Regras desta noite: seguir `docs/PROCESSOS.md` e `docs/DESIGN.md`; commits pequenos + push no branch `claude/remover-leitura-notificacoes`; comentar e mover os cards no Runtime (projeto GymTracker); **migração de banco não se aplica** (projeto `ygbrrypyydxnuskrobeu`, outra conta): vai em `docs/MIGRACOES_PENDENTES.md`; sempre olhar a tela no emulador; nada de deploy de produção.

## Já feito (21/11, noite)
Auditoria completa (`AUDITORIA_DESIGN.md`) · Home com "Próximo treino" + aviso "Retomar" em todos os temas · FABs na cor de marca · Perfil em seções · menu ⋮ e **Desfazer** ao apagar treino · finalização automática de treino esquecido (>1 h sem série) · **seletor de temas em grade** com miniaturas · teste de contraste de todos os temas (Brutal e Arena corrigidos) · Home em paralelo · números que rolam, esqueleto de carregamento e **ilha do descanso** (escritos; ver CONTEXTO) · `DESIGN.md` com mapa de todas as referências · `PROCESSOS.md` · Vault 277 itens.

## Feito na noite (parte 2)
#523 ícone · #445 roteiro · #365 vibração · #364 backup (falta importar) · #355 imagem do treino · #353 mapa muscular · #354 retrospectiva do mês · #350 RPE local · atalhos do ícone · agenda no "Próximo treino" · 3 temas novos (Comando, Pergaminho, Industrial) · descanso automático + ilha · Retomar direto.

## Fila original (ordem; fáceis primeiro, sem depender de banco)
1. **#523 Ícone do app** — ícone adaptativo (primeiro plano vetorial + fundo + monocromático Android 13+), claro/escuro, conferir no launcher e no relógio. Traço único, sem emoji; acento lima do Carbon.
2. **#445 Roteiro único de validação no aparelho** — já existe em `PROCESSOS.md` §5; só comentar e mover.
3. **#365 Sons e vibração refinados** — háptico ao registrar série (confirmação curta), ao bater recorde (padrão duplo) e ao fim do descanso; chave "vibração/sons" em Aparência; respeitar o modo silencioso.
4. **#364 Backup completo em um arquivo** — exportar tudo (treinos, séries, grupos, exercícios, medidas, metas) em JSON pelo seletor de arquivos do sistema; importar de volta (só inserindo o que falta).
5. **#355 Compartilhar treino como imagem** — desenhar o resumo (tema atual, grupo, duração, séries, volume, recordes) num `Bitmap` e compartilhar.
6. **#353 Mapa muscular com volume da semana** — volume por músculo na semana (do catálogo de exercícios), em grade/figura simples com intensidade pela cor do tema.
7. **#354 Retrospectiva mensal/anual** — estatísticas do período + compartilhar como imagem (reaproveita o item 5).
8. **#350 Esforço por série (RPE)** — guardar **localmente** (preferências) enquanto a coluna `rpe` não existe no banco; migração em `MIGRACOES_PENDENTES.md`; slider no estilo `hyaline-level`/`wake-slider` (porte fiel do cache do OriginKit).
9. **#346/#348 Componentes e polimento** — abas do Social com direção (Cult UI `direction-aware-tabs`), filtros do Histórico por grupo com contador (Moxen `faceta`), `like` no feed, selos de Conquistas.
10. **#360 Gráficos de evolução** — família "Data-Dense Pro": números tabulares, gráfico como herói.
11. **Temas novos** (Refero): Comando (Raycast), Pergaminho (Superhuman), Industrial (teenage engineering) — com teste de contraste.

## Não fazer esta noite (precisam de você ou de infra)
#446 imagens de exercício (gerar), #367 Play Store, #358 Health Connect, #359 relógio, #361 IA, #362 ligas (migração), #363 notificações do Social (servidor), #345 offline (grande), #352 periodização, #349 tipos de série (migração), #344/#443 medidas na nuvem (migração 013 já escrita; falta aplicar), #444 testes sem SDK (já há SDK).

## Ao fechar cada item
Compilar pelo terminal real · testes do `:shared` · olhar no emulador (2 temas de layouts diferentes) · commit + push · comentar e mover o card · atualizar `CONTEXTO_SESSAO.md` · `node ../Vault/scripts/import-runtime.mjs` se escreveu conhecimento novo.
