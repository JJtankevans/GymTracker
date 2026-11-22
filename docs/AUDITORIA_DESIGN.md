# Auditoria de design — GymTracker (21/11/2026)

Feita no emulador (Pixel 7, Android 35) com o APK debug, conta de teste, tema Carbon.
Legenda: ✅ feito nesta sessão · ⏳ a fazer · 💡 ideia de funcionalidade.

## Home
- ✅ Botão "Iniciar Treino" estava em verde-oliva apagado (cor `primaryContainer` padrão do Material). Agora usa a cor de marca (`primary`).
- ✅ Faltava um destaque claro do que fazer agora: novo cartão **"Próximo treino"** no topo com o grupo sugerido pela rotação e início em um toque.
- ⏳ Quarto inferior da tela vazio; o cartão de frequência poderia dar lugar a "Resumo da semana" com volume total.
- ⏳ Retomar treino em andamento: ao voltar do treino ativo não há aviso "Treino em andamento · Retomar".
- 💡 Cartão com o último recorde batido e o próximo "quase lá".

## Treino ativo
- ✅ Grupo sem exercícios mostrava só um texto. Agora tem o botão **"Adicionar exercícios"** que abre a tela de exercícios do grupo.
- ⏳ Botão "Finalizar" encosta na borda direita da barra superior.
- ⏳ Registrar série: mais rápido com valores da última sessão pré-preenchidos (a conferir) e RPE opcional (#350).
- 💡 Timer de descanso como "ilha" flutuante (Skiper UI) e tela de foco.

## Grupos musculares
- ✅ Três ícones por linha (compartilhar, editar, **lixeira vermelha sempre à vista**) viraram um menu ⋮. Risco de toque sem querer eliminado.
- ✅ Nomes normalizados só na exibição: "QUADRICEPS E PANTURRILHA" → "Quadriceps e Panturrilha".
- ⏳ Cada linha deveria mostrar "N exercícios · último treino há X dias".
- ⏳ Arrastar para reordenar a rotação.

## Histórico
- ✅ Durações absurdas (`17h 40min`, `12h 32min`) vinham de treinos deixados abertos. Acima de 4 h mostra "—".
- ✅ Mapa de calor só tinha "S Q S D"; agora S T Q Q S S D.
- ⏳ "Segure um treino para apagar" é invisível: trocar por deslizar com desfazer.
- ⏳ Cada treino sem volume/nº de séries; adicionar.
- 💡 Fechar treinos abertos automaticamente após X horas (e avisar).

## Recordes
- ✅ O selo "NOVO" ficava em recordes de semanas atrás. Agora só até 7 dias.
- ⏳ O troféu da Home abre "Recordes", mas "Conquistas" está escondida no Perfil. Unir numa tela com abas (Recordes · Conquistas).
- 💡 Componente OriginKit `holo-flip-badge` para os selos de conquista.

## Social
- ✅ Reação do fone de ouvido (sobra da música removida) saiu da lista.
- ⏳ Todo cartão do feed começa com "Treino finalizado ·"; trocar por título forte (grupo) e subtítulo.
- 💡 Componente OriginKit `like` para a animação de curtir.

## Perfil
- ✅ Lista longa de botões iguais virou duas seções ("Seus dados" e "Aplicativo") com ícone, título e descrição.
- ⏳ "Salvar meta" parece desabilitado mesmo quando muda; só ativar quando houver alteração e dar feedback.
- 💡 Slider da meta semanal com `hyaline-level` (OriginKit).

## Transversal
- ✅ FABs de Exercícios e Grupos agora na cor de marca (como a Home).
- ⏳ Tamanho mínimo de toque 48 dp nos ícones das barras superiores.
- ⏳ Esqueleto de carregamento em vez de spinner na Home.
- ⏳ Padronizar a gramática de rótulos: verbos no infinitivo nos botões ("Iniciar treino", "Adicionar exercícios").
