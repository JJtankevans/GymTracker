# Auditoria de design — GymTracker (21/11/2026)

Feita no emulador (Pixel 7, Android 35) com o APK debug, conta de teste, tema Carbon.
Legenda: ✅ feito nesta sessão · ⏳ a fazer · 💡 ideia de funcionalidade.

## Home
- ✅ Botão "Iniciar Treino" estava em verde-oliva apagado (cor `primaryContainer` padrão do Material). Agora usa a cor de marca (`primary`).
- ✅ Faltava um destaque claro do que fazer agora: novo cartão **"Próximo treino"** no topo com o grupo sugerido pela rotação e início em um toque.
- ✅ Quarto inferior vazio: o cartão Próximo treino, o aviso Retomar e os números que rolam ocupam o espaço; sobra só o respiro do FAB.
- ✅ Aviso "Treino em andamento · Retomar" (entra direto, sem diálogo) em todos os layouts de Home.
- 💡 Cartão com o último recorde batido e o próximo "quase lá".

## Treino ativo
- ✅ Grupo sem exercícios mostrava só um texto. Agora tem o botão **"Adicionar exercícios"** que abre a tela de exercícios do grupo.
- ✅ Botão "Finalizar" com margem de 16 dp.
- ⏳ Registrar série: mais rápido com valores da última sessão pré-preenchidos (a conferir) e RPE opcional (#350).
- 💡 Timer de descanso como "ilha" flutuante (Skiper UI) e tela de foco.

## Grupos musculares
- ✅ Três ícones por linha (compartilhar, editar, **lixeira vermelha sempre à vista**) viraram um menu ⋮. Risco de toque sem querer eliminado.
- ✅ Nomes normalizados só na exibição: "QUADRICEPS E PANTURRILHA" → "Quadriceps e Panturrilha".
- ⏳ Cada linha deveria mostrar "N exercícios · último treino há X dias" (precisa contar exercícios por grupo no estado).
- ⏳ Arrastar para reordenar a rotação.

## Histórico
- ✅ Durações absurdas (`17h 40min`, `12h 32min`) vinham de treinos deixados abertos. Acima de 4 h mostra "—".
- ✅ Mapa de calor só tinha "S Q S D"; agora S T Q Q S S D.
- ✅ "Segure para apagar" virou menu ⋮ visível + Desfazer por 10 s (segurar continua funcionando).
- ⏳ Cada treino sem volume/nº de séries no Histórico; adicionar (o filtro por grupo com contadores já existe ✅).
- 💡 Fechar treinos abertos automaticamente após X horas (e avisar).

## Recordes
- ✅ O selo "NOVO" ficava em recordes de semanas atrás. Agora só até 7 dias.
- ✅ "Conquistas ›" dentro de Recordes; Conquistas com ícone por família. (Unir em abas continua opcional.)
- 💡 Componente OriginKit `holo-flip-badge` para os selos de conquista.

## Social
- ✅ Reação do fone de ouvido (sobra da música removida) saiu da lista.
- ✅ Feed: grupo como título forte, "Treino finalizado" como subtítulo; reação com o estalo do `like` (OriginKit); abas com direção (Cult UI).
- 💡 Componente OriginKit `like` para a animação de curtir.

## Perfil
- ✅ Lista longa de botões iguais virou duas seções ("Seus dados" e "Aplicativo") com ícone, título e descrição.
- ✅ Meta semanal com Wake Slider (OriginKit); "Salvar meta" habilita ao mudar o valor.
- 💡 Slider da meta semanal com `hyaline-level` (OriginKit).

## Transversal
- ✅ FABs de Exercícios e Grupos agora na cor de marca (como a Home).
- ✅ Tamanho mínimo de toque 48 dp: Circuito (−/+) e favorito do catálogo corrigidos; barras superiores já em 48 dp.
- ✅ Esqueleto de carregamento em vez de spinner (Home e as outras telas).
- ⏳ Padronizar a gramática de rótulos: verbos no infinitivo nos botões ("Iniciar treino", "Adicionar exercícios").
