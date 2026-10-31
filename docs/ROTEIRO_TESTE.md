# Roteiro de teste no aparelho (PR #12, `claude/noite-melhorias`)

Marque cada item e anote o que estranhar (print ou descrição). Nada daqui foi visto em aparelho: tudo compila e passa nos testes do CI, só isso.
APK certo: os dois artefatos (`gymtracker-debug-apk` e `gymtracker-wear-apk`) da **mesma** execução verde do branch `claude/noite-melhorias`. Instalação: `docs/RELOGIO.md` ou `tools/instalar_apks.ps1`.

## 0. Antes de tudo
- [ ] Instalou o APK do celular. Se instalou pelo WhatsApp: o Play Protect bloqueou? (a leitura de notificações foi removida; se bloquear de novo, a causa é outra)
- [ ] Rodou as migrações **013**, **014**, **015** e **016** no Supabase (`docs/supabase/`). Sem elas o app funciona, mas medidas não vão para a nuvem, a liga da semana fica escondida e "Excluir minha conta" avisa que falta a 016.
- [ ] Entrou na conta e a Home carregou.

## 1. Home e Histórico
- [ ] Cartão **Frequência** mostra "N semanas seguidas batendo a meta" (só aparece com 1+ semana batida; a semana em andamento não zera a conta).
- [ ] Atalho **"2º de 5 na liga da semana"** abre o Social (precisa da 015 e de gente seguida).
- [ ] **Histórico**: resumo do mês no topo, mapa de calor e, com 2+ grupos treinados, chips **Todos / Peito / Costas...** acima da lista.
  - [ ] Tocar num chip filtra a lista; "Todos" volta.
  - [ ] Apagar (toque longo) o último treino de um grupo filtrado: a lista não fica vazia, volta a mostrar tudo.

## 1b. Treino ativo
- [ ] Deslizar para o lado entre exercícios: o chip do exercício atual rola sozinho para o meio da faixa do topo (com 8+ exercícios, ida e volta). Tocar num chip também centraliza o chip.

- [ ] **Exercício só desta sessão**: botão "+" na barra do treino. Escolher do catálogo (ou "Personalizado" com nome livre): o exercício aparece no fim da lista, o Foco vai direto para ele, registra séries normalmente e entra no resumo. Terminar o treino e abrir o grupo: o exercício NÃO está no grupo.
- [ ] Digitar o nome de um exercício que você já tem (outra caixa/acento): reaproveita, com última carga e recorde. Fechar o app no meio e retomar o treino: o temporário volta (se já tinha série).

- [ ] **Estilo do treino** (Perfil → Aparência → "Estilo do treino"): Automático / Lista / Foco / Arena / Anel. Veja `docs/DESIGN_TREINO_ATIVO.md`.
  - [ ] **Arena**: cabe tudo sem rolar (teste também com o teclado fechado e em tela pequena)? Arrastar o número para cima/baixo muda o valor com vibração? A comparação ("+2,5 kg vs. última") muda ao mexer na carga? REGISTRAR vira a barra de descanso e tocar nela pula?
  - [ ] **Anel**: os segmentos acendem a cada série e o anel de dentro conta o descanso? Tocar no meio registra (e, no descanso, pula)? O tamanho do anel está bom?
  - [ ] **Foco**: a comparação com a última vez e o recorde aparece acima do campo de reps/kg.
  - [ ] No **Insano**, o "soco" nos números ao registrar e o pulso das bolinhas/anel estão bons ou exagerados?
  - [ ] Os estilos funcionam com os exercícios extras ("+") e com a faixa de chips que acompanha o exercício.

- [ ] **Treino em andamento na Home**: iniciar um treino, registrar uma série, voltar para a Home pela seta. Aparece o cartão "Treino em andamento" com o grupo e o tempo. "Continuar treino" entra direto (sem a pergunta, com a série que você já fez). "Cancelar" pede confirmação e apaga o treino. Terminar o treino faz o cartão sumir.
- [ ] **Alarme do fim do descanso**: registrar uma série (descanso de 60 s), **bloquear o celular e deixar parado**. No fim do descanso o celular deve **vibrar** (padrão longo) e mostrar "Descanso acabou" na tela de bloqueio, mesmo se estiver no silencioso. Repetir com o app fechado pelo recentes. Com o treino aberto na tela, deve avisar uma vez só (sem vibrar em dobro). Pular o descanso (ou pausar) antes do fim: o alarme não pode tocar depois.
- [ ] **Descanso depois do bloqueio**: registrar uma série (descanso rodando), bloquear o celular por ~20 s, desbloquear e voltar ao app: o tempo deve estar certo na hora e seguir contando sem precisar tocar. Repetir com o descanso acabando enquanto a tela está bloqueada (deve mostrar "BORA!").

## 2. Evolução e Recordes
- [ ] **Evolução → Equilíbrio do treino (7 dias)**: barras de Empurrar / Puxar / Pernas / Core e uma frase (só com 8+ séries na semana). Confira se a frase bate com o que você treinou.
- [ ] Registrar uma medida (cintura), sair da conta, entrar de novo: a medida continua (depende da 014).
- [ ] **Recordes**: cartão "Estagnados" (só aparece se houver exercício parado há 6+ semanas), tocar num recorde abre "Meta" e a barra de progresso aparece; sair da conta apaga as metas.

## 3. Avisos
- [ ] Permitir notificações. Pedido de seguir e fim de desafio avisam em até ~6 h (o app confere de tempos em tempos, não é instantâneo).
- [ ] Fechamento da semana: domingo depois das 18h ou segunda.
- [ ] **Sequência em risco**: com 3+ dias seguidos terminando ontem e nenhum treino hoje, depois das 18h chega "Sua sequência está em risco" (uma vez por dia; canal "Lembretes de treino" nas configurações de notificação do Android). Treinou hoje: não avisa.
- [ ] Negou a permissão antes, depois permitiu: os avisos pendentes devem aparecer (corrigido nesta sessão).

## 3b. Excluir conta (APAGA DE VERDADE)
- [ ] **Só com uma conta de teste criada para isso, nunca a sua de verdade.** Perfil → "Excluir minha conta": o botão "Excluir tudo" só habilita depois de digitar EXCLUIR.
- [ ] Com a 016 rodada: volta para a tela de login, e entrar de novo com o mesmo e-mail/senha falha. A foto de perfil some do Storage.
- [ ] Sem internet (modo avião): mostra o erro de conexão e continua logado, com os dados intactos.

## 3c. Grupos e desafios personalizados (precisa da migração 017)
- [ ] Social → Grupos → Criar grupo: escolher capa (20 opções com o fundo animado de um tema), cor, ícone, meta semanal. O cartão do grupo mostra a faixa de capa com o ícone e o nome.
- [ ] Tocar no grupo abre a folha: capa animada, descrição, aviso fixado, **meta da semana** com barra (treinos de todos os membros, segunda a domingo) e, se for o dono, o código de convite e "Personalizar grupo".
- [ ] Dono: Personalizar grupo → trocar capa/cor/ícone, escrever o aviso fixado e a meta. Outro membro vê o aviso e a barra.
- [ ] Desafio novo com capa, cor, ícone e **prêmio**: o cartão e o placar mostram a faixa e "Prêmio: ...".
- [ ] Sem rodar a 017: criar grupo/desafio **sem** personalizar continua funcionando; personalizar mostra o aviso de que falta a 017.

## 3c-2. Grupos vivos (precisa da migração 019)
- [ ] O **aviso fixado** mostra o texto inteiro (até 1000 caracteres; "Ver mais" quando passa de 4 linhas) num cartão limpo com barra na cor do grupo.
- [ ] Abas **Resumo / Ranking / Mural** no grupo. Resumo: meta da semana grande com barra, "N semanas seguidas batendo a meta", desafios do grupo e "Novo desafio neste grupo" (o grupo já vem escolhido), convite com botão de compartilhar.
- [ ] **Ranking**: membros ordenados por treinos da semana, medalhas ouro/prata/bronze; o dono vê o menu (três pontos) para promover a admin ou remover; um admin só remove membros comuns.
- [ ] **Mural**: escrever (até 280), enviar, a mensagem aparece com nome e "agora"; apagar a própria; o admin apaga qualquer uma.
- [ ] Sem a 019: o aviso fixado continua cortando em 300 e o ranking e o mural ficam vazios, sem quebrar o resto.

## 3d. Ranqueada (precisa da migração 018; veja `docs/RANQUEADA_DESIGN.md`)
- [ ] Social → aba **Ranqueada** → "Entrar na ranqueada": aparece o cartão do elo (Ferro III, emblema desenhado, barra de RP) e a **rodada da semana** com seus pontos.
- [ ] Treinar (20 min ou mais) e voltar: os pontos sobem (100 + esforço + séries; só o melhor treino do dia). 3 dias de treino dão +50.
- [ ] A **turma** aparece com posição, elo e pontos de cada um; faixas verde (sobe) e vermelha (desce); "Faltam N pontos para a zona de subida".
- [ ] **x1**: com 2 contas no mesmo elo, o botão "x1" aparece ao lado da pessoa na turma. Desafiar (valendo RP ou amistoso), aceitar na outra conta, acompanhar o placar ao vivo e, depois de 7 dias (ou ajustando as datas no banco), ver o resultado e o RP.
- [ ] Sem a 018: a aba mostra "falta rodar a migração 018" com "Tentar de novo".
- [ ] Fechamento da semana: na segunda, abrir a aba mostra "Semana passada: Nº de X · pts · ±RP" (só funciona depois de uma segunda-feira real ou de forçar `ranked_settle_week` no SQL).

## 4. Temas (Perfil → Aparência)
- [ ] 52 temas, 31 Loucos, selo **NOVO** nos 6 últimos; botão de tema aleatório.
- [ ] **Temas antigos renovados** (Carbon, Oceano, Studio, Tinta, Papel, Blueprint, Brutal, Neon, Terminal, Arena, Ouro, Aurora, Synthwave, Confete, Magma, Nórdico, Obsidiana, Cobalto, Floresta, Ártico, Sistema): cada um tem um cenário animado só dele. Conferir em **Calmo / Normal / Insano** se a riqueza muda (Calmo discreto, Insano cheio) e se o texto continua legível. Atenção nos claros (Tinta, Papel, Brutal, Nórdico, Confete, Studio).
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
