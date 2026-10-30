# Roteiro de teste no aparelho (PR #12, `claude/noite-melhorias`)

Marque cada item e anote o que estranhar (print ou descrição). Nada daqui foi visto em aparelho: tudo compila e passa nos testes do CI, só isso.
APK certo: os dois artefatos (`gymtracker-debug-apk` e `gymtracker-wear-apk`) da **mesma** execução verde do branch `claude/noite-melhorias`. Instalação: `docs/RELOGIO.md` ou `tools/instalar_apks.ps1`.

## 0. Antes de tudo
- [ ] Instalou o APK do celular. Se instalou pelo WhatsApp: o Play Protect bloqueou? (a leitura de notificações foi removida; se bloquear de novo, a causa é outra)
- [ ] Rodou as migrações **013**, **014** e **015** no Supabase (`docs/supabase/`). Sem elas o app funciona, mas medidas não vão para a nuvem e a liga da semana fica escondida.
- [ ] Entrou na conta e a Home carregou.

## 1. Home e Histórico
- [ ] Cartão **Frequência** mostra "N semanas seguidas batendo a meta" (só aparece com 1+ semana batida; a semana em andamento não zera a conta).
- [ ] Atalho **"2º de 5 na liga da semana"** abre o Social (precisa da 015 e de gente seguida).
- [ ] **Histórico**: resumo do mês no topo, mapa de calor e, com 2+ grupos treinados, chips **Todos / Peito / Costas...** acima da lista.
  - [ ] Tocar num chip filtra a lista; "Todos" volta.
  - [ ] Apagar (toque longo) o último treino de um grupo filtrado: a lista não fica vazia, volta a mostrar tudo.

## 2. Evolução e Recordes
- [ ] **Evolução → Equilíbrio do treino (7 dias)**: barras de Empurrar / Puxar / Pernas / Core e uma frase (só com 8+ séries na semana). Confira se a frase bate com o que você treinou.
- [ ] Registrar uma medida (cintura), sair da conta, entrar de novo: a medida continua (depende da 014).
- [ ] **Recordes**: cartão "Estagnados" (só aparece se houver exercício parado há 6+ semanas), tocar num recorde abre "Meta" e a barra de progresso aparece; sair da conta apaga as metas.

## 3. Avisos
- [ ] Permitir notificações. Pedido de seguir e fim de desafio avisam em até ~6 h (o app confere de tempos em tempos, não é instantâneo).
- [ ] Fechamento da semana: domingo depois das 18h ou segunda.
- [ ] Negou a permissão antes, depois permitiu: os avisos pendentes devem aparecer (corrigido nesta sessão).

## 4. Temas (Perfil → Aparência)
- [ ] 52 temas, 31 Loucos, selo **NOVO** nos 6 últimos; botão de tema aleatório.
- [ ] Fluidez: Estática (~420 retângulos por quadro), Discoteca, Matrix. Se travar, anote o nome.
- [ ] Legibilidade do **Holográfico**, **Sakura** e **Origami** (claros).

## 5. Exportar e compartilhar
- [ ] Exportar CSV e abrir na planilha: colunas separadas por `;`, vírgula decimal. Um exercício chamado `=1+1` deve aparecer como texto `'=1+1`, não calcular.
- [ ] Importar um treino por código de outra conta: cria o grupo e os exercícios (no máximo 60).

## 6. Relógio
- [ ] Início: anel da semana, "Sugerido hoje", últimos 5 treinos.
- [ ] Sincronizar: Perfil → cartão **Relógio** → "Sincronizar com o relógio".
- [ ] Treino completo: série, coroa muda carga, descanso vibra **com a tela apagada**, "RECORDE!", resumo com melhor série (1RM) e zonas de batimento.
- [ ] Ao terminar: o celular avisa "Treino do relógio salvo" e o treino aparece no Histórico (uma vez só).
- [ ] **Intervalos** (Tabata, HIIT, EMOM, Meu timer): vibra nas trocas, 3-2-1, tela acesa, pausar/seguir. Fora da tela do timer continua vibrando; **com o app fechado não vibra** (limitação conhecida).
- [ ] Tile "GymTracker" (deslizar do mostrador) e complicação "Treinos na semana".
- [ ] Anilhas (toque em "anilhas" com a carga selecionada) e adicionar exercício fora do grupo.
- [ ] Zonas: preencher o ano de nascimento em Perfil → Meus dados e sincronizar; a zona aparece na tela de números.

## 7. iPhone (só se tiver Mac/Xcode)
- [ ] O CI só prova que a tela de login abre no simulador. Login, treino e telas internas nunca foram exercitados.
