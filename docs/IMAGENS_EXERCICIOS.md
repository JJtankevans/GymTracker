# Imagens dos exercícios

O app mostra uma ilustração por exercício no catálogo, na lista de exercícios e em Recordes.
Se o arquivo não existir, cai no ícone desenhado do equipamento (nada quebra), então dá para
mandar aos poucos.

## Como entregar

- Um arquivo **PNG por exercício**, fundo **transparente**, **quadrado** (512×512 já serve; 1024×1024 no máximo).
- Personagem centralizado, com ~8% de margem. O app põe um fundo escuro com a cor do músculo atrás.
- Estilo de referência: silhueta roxa/azul-escura com o músculo trabalhado em vermelho/coral (as duas folhas que o Thiago mandou).
- Nome do arquivo = **chave** da tabela abaixo (ex.: `ex_supino_reto_com_barra.png`), em letras minúsculas, sem acento.
- Pasta: `app/src/main/res/drawable-nodpi/` (se vier uma folha com vários desenhos, dá para cortar em arquivos).
- Rodar depois: `./gradlew.bat :app:assembleDebug`.

A chave vem do nome (`CatalogExercise.imageKey`), e um teste garante que ela é única e válida.

## Situação (21/10/2026): 41 de 56 prontas
Faltam estas 15 (caem no ícone do equipamento): Supino na Máquina, Remada na Máquina, Desenvolvimento na Máquina, Rosca Martelo, Rosca na Polia, Tríceps Testa, Supino Fechado, Agachamento no Smith, Hack Squat, Elevação Pélvica, Cadeira Abdutora, Glúteo na Polia (Coice), Panturrilha Sentado, Panturrilha no Leg Press, Abdominal na Polia.
As duas folhas originais (`docs/folhas/`) têm estilos diferentes (1 realista/sombreada, 2 plana); o ideal é regerar tudo no mesmo estilo. O script de corte usado nas folhas era temporário e não foi versionado (flood fill do fundo, tapa bolsões, agrupa peças por vizinhança, centraliza em 384×384).

Para descobrir quais exercícios criados pelos usuários ainda não estão no catálogo (e portanto não têm imagem), rodar `docs/supabase/consulta_exercicios_de_todos_os_usuarios.sql` no SQL Editor do Supabase: as linhas com `no_catalogo = true` são as candidatas.

## Lista

| # | Exercício | Músculo · equipamento | Arquivo |
|---|-----------|-----------------------|---------|
| 1 | Supino Reto com Barra | Peito · Barra | `ex_supino_reto_com_barra.png` |
| 2 | Supino Inclinado com Barra | Peito · Barra | `ex_supino_inclinado_com_barra.png` |
| 3 | Supino Reto com Halteres | Peito · Halteres | `ex_supino_reto_com_halteres.png` |
| 4 | Supino Inclinado com Halteres | Peito · Halteres | `ex_supino_inclinado_com_halteres.png` |
| 5 | Crucifixo com Halteres | Peito · Halteres | `ex_crucifixo_com_halteres.png` |
| 6 | Supino na Máquina | Peito · Máquina | `ex_supino_na_maquina.png` |
| 7 | Crucifixo na Máquina (Peck Deck) | Peito · Máquina | `ex_crucifixo_na_maquina_peck_deck.png` |
| 8 | Crossover na Polia | Peito · Polia | `ex_crossover_na_polia.png` |
| 9 | Flexão de Braço | Peito · Peso do corpo | `ex_flexao_de_braco.png` |
| 10 | Barra Fixa | Costas · Peso do corpo | `ex_barra_fixa.png` |
| 11 | Puxada Frontal na Polia | Costas · Polia | `ex_puxada_frontal_na_polia.png` |
| 12 | Remada Baixa na Polia | Costas · Polia | `ex_remada_baixa_na_polia.png` |
| 13 | Remada Curvada com Barra | Costas · Barra | `ex_remada_curvada_com_barra.png` |
| 14 | Levantamento Terra | Costas · Barra | `ex_levantamento_terra.png` |
| 15 | Remada Unilateral com Halter | Costas · Halteres | `ex_remada_unilateral_com_halter.png` |
| 16 | Remada na Máquina | Costas · Máquina | `ex_remada_na_maquina.png` |
| 17 | Pulldown na Polia | Costas · Polia | `ex_pulldown_na_polia.png` |
| 18 | Desenvolvimento com Barra | Ombros · Barra | `ex_desenvolvimento_com_barra.png` |
| 19 | Desenvolvimento com Halteres | Ombros · Halteres | `ex_desenvolvimento_com_halteres.png` |
| 20 | Desenvolvimento na Máquina | Ombros · Máquina | `ex_desenvolvimento_na_maquina.png` |
| 21 | Elevação Lateral | Ombros · Halteres | `ex_elevacao_lateral.png` |
| 22 | Elevação Frontal | Ombros · Halteres | `ex_elevacao_frontal.png` |
| 23 | Crucifixo Inverso | Ombros · Máquina | `ex_crucifixo_inverso.png` |
| 24 | Encolhimento com Halteres | Ombros · Halteres | `ex_encolhimento_com_halteres.png` |
| 25 | Rosca Direta com Barra | Bíceps · Barra | `ex_rosca_direta_com_barra.png` |
| 26 | Rosca Alternada com Halteres | Bíceps · Halteres | `ex_rosca_alternada_com_halteres.png` |
| 27 | Rosca Martelo | Bíceps · Halteres | `ex_rosca_martelo.png` |
| 28 | Rosca Scott | Bíceps · Máquina | `ex_rosca_scott.png` |
| 29 | Rosca na Polia | Bíceps · Polia | `ex_rosca_na_polia.png` |
| 30 | Tríceps Testa | Tríceps · Barra | `ex_triceps_testa.png` |
| 31 | Tríceps Pulley | Tríceps · Polia | `ex_triceps_pulley.png` |
| 32 | Tríceps Corda | Tríceps · Polia | `ex_triceps_corda.png` |
| 33 | Tríceps Francês | Tríceps · Halteres | `ex_triceps_frances.png` |
| 34 | Mergulho nas Paralelas | Tríceps · Peso do corpo | `ex_mergulho_nas_paralelas.png` |
| 35 | Supino Fechado | Tríceps · Barra | `ex_supino_fechado.png` |
| 36 | Agachamento Livre | Quadríceps · Barra | `ex_agachamento_livre.png` |
| 37 | Agachamento no Smith | Quadríceps · Smith | `ex_agachamento_no_smith.png` |
| 38 | Leg Press 45° | Quadríceps · Máquina | `ex_leg_press_45.png` |
| 39 | Hack Squat | Quadríceps · Máquina | `ex_hack_squat.png` |
| 40 | Cadeira Extensora | Quadríceps · Máquina | `ex_cadeira_extensora.png` |
| 41 | Agachamento Búlgaro | Quadríceps · Halteres | `ex_agachamento_bulgaro.png` |
| 42 | Afundo com Halteres | Quadríceps · Halteres | `ex_afundo_com_halteres.png` |
| 43 | Mesa Flexora | Posterior · Máquina | `ex_mesa_flexora.png` |
| 44 | Cadeira Flexora | Posterior · Máquina | `ex_cadeira_flexora.png` |
| 45 | Stiff com Barra | Posterior · Barra | `ex_stiff_com_barra.png` |
| 46 | Levantamento Terra Romeno | Posterior · Barra | `ex_levantamento_terra_romeno.png` |
| 47 | Elevação Pélvica | Glúteos · Barra | `ex_elevacao_pelvica.png` |
| 48 | Cadeira Abdutora | Glúteos · Máquina | `ex_cadeira_abdutora.png` |
| 49 | Glúteo na Polia (Coice) | Glúteos · Polia | `ex_gluteo_na_polia_coice.png` |
| 50 | Panturrilha em Pé | Panturrilha · Máquina | `ex_panturrilha_em_pe.png` |
| 51 | Panturrilha Sentado | Panturrilha · Máquina | `ex_panturrilha_sentado.png` |
| 52 | Panturrilha no Leg Press | Panturrilha · Máquina | `ex_panturrilha_no_leg_press.png` |
| 53 | Abdominal Supra | Abdômen · Peso do corpo | `ex_abdominal_supra.png` |
| 54 | Elevação de Pernas | Abdômen · Peso do corpo | `ex_elevacao_de_pernas.png` |
| 55 | Prancha | Abdômen · Peso do corpo | `ex_prancha.png` |
| 56 | Abdominal na Polia | Abdômen · Polia | `ex_abdominal_na_polia.png` |
