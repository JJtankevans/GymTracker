# Como gerar as imagens que faltam (textos para o gerador + corte automático)

O Claude (nesta ferramenta) não gera imagens; quem gerou as folhas em `docs/folhas/` foi o Thiago com um gerador de imagens.
Este arquivo deixa pronto o texto para pedir as que faltam e o passo a passo para colocá-las no app.

## 1. Quais faltam (29 de 70 em 21/10/2026: 15 que já eram do catálogo + 14 que os usuários criaram e agora estão no catálogo)
Confira sempre com `python3 tools/cortar_folha.py --faltando` (lê o catálogo e a pasta `drawable-nodpi`).

| # | Exercício | Arquivo | Pose e músculo destacado |
|---|-----------|---------|--------------------------|
| 1 | Supino na Máquina | `ex_supino_na_maquina` | sentado na máquina de supino, empurrando as manoplas para a frente · peitoral |
| 2 | Remada na Máquina | `ex_remada_na_maquina` | sentado com o peito apoiado, puxando as manoplas para trás · costas (dorsais) |
| 3 | Desenvolvimento na Máquina | `ex_desenvolvimento_na_maquina` | sentado, empurrando as manoplas para cima · ombros (deltoides) |
| 4 | Rosca Martelo | `ex_rosca_martelo` | em pé, halteres com pegada neutra (palmas uma para a outra), cotovelo flexionado · bíceps e antebraço |
| 5 | Rosca na Polia | `ex_rosca_na_polia` | em pé de frente para a polia baixa, barra reta, flexionando o cotovelo · bíceps |
| 6 | Tríceps Testa | `ex_triceps_testa` | deitado no banco, barra W descendo em direção à testa, cotovelos apontando para cima · tríceps |
| 7 | Supino Fechado | `ex_supino_fechado` | deitado no banco, barra com pegada fechada, cotovelos junto ao corpo · tríceps e peitoral |
| 8 | Agachamento no Smith | `ex_agachamento_no_smith` | agachado dentro do Smith (barra guiada), pés ligeiramente à frente · quadríceps e glúteos |
| 9 | Hack Squat | `ex_hack_squat` | costas apoiadas na máquina inclinada, joelhos flexionados · quadríceps |
| 10 | Elevação Pélvica | `ex_elevacao_pelvica` | parte alta das costas no banco, barra sobre o quadril, quadril elevado · glúteos |
| 11 | Cadeira Abdutora | `ex_cadeira_abdutora` | sentado na máquina, pernas abrindo para os lados · glúteo médio (lateral do quadril) |
| 12 | Glúteo na Polia (Coice) | `ex_gluteo_na_polia_coice` | em pé, tornozeleira na polia baixa, perna estendida para trás · glúteo |
| 13 | Panturrilha Sentado | `ex_panturrilha_sentado` | sentado na máquina com os joelhos sob o apoio, ponta dos pés na plataforma · panturrilha |
| 14 | Panturrilha no Leg Press | `ex_panturrilha_no_leg_press` | sentado no leg press, só a ponta dos pés na plataforma, joelhos quase estendidos · panturrilha |
| 15 | Abdominal na Polia | `ex_abdominal_na_polia` | de joelhos de frente para a polia alta, corda junto à cabeça, tronco flexionando · abdômen |
| 16 | Supino Inclinado na Máquina | `ex_supino_inclinado_na_maquina` | sentado na máquina com o encosto inclinado, empurrando as manoplas para cima e para a frente · peitoral superior |
| 17 | Supino Declinado na Máquina | `ex_supino_declinado_na_maquina` | sentado na máquina com o encosto reclinado, empurrando as manoplas para baixo e para a frente · peitoral inferior |
| 18 | Supino Guiado | `ex_supino_guiado` | deitado no banco sob o Smith, barra guiada sendo empurrada para cima · peitoral |
| 19 | Remada Cavalinho | `ex_remada_cavalinho` | em pé, tronco inclinado sobre a barra T (uma ponta no chão, anilhas na outra), puxando a pegada em V · costas |
| 20 | Remada Cavalinho na Máquina | `ex_remada_cavalinho_na_maquina` | peito apoiado na almofada inclinada, puxando as manoplas para trás · costas |
| 21 | Elevação Lateral na Máquina | `ex_elevacao_lateral_na_maquina` | sentado, braços abrindo para os lados contra as almofadas · ombros (deltoide lateral) |
| 22 | Face Pull | `ex_face_pull` | em pé de frente para a polia alta, corda puxada em direção ao rosto com os cotovelos altos · ombro posterior |
| 23 | Rosca com Barra W | `ex_rosca_com_barra_w` | em pé, barra W, cotovelos fixos ao lado do corpo, flexionando · bíceps |
| 24 | Rosca Concentrada | `ex_rosca_concentrada` | sentado, cotovelo apoiado na parte interna da coxa, halter subindo · bíceps |
| 25 | Rosca Martelo na Polia | `ex_rosca_martelo_na_polia` | em pé de frente para a polia baixa, corda com pegada neutra, flexionando os cotovelos · bíceps e braquial |
| 26 | Tríceps Coice na Polia | `ex_triceps_coice_na_polia` | tronco inclinado, cotovelo fixo junto ao corpo, estendendo o braço para trás com a polia baixa · tríceps |
| 27 | Tríceps na Máquina | `ex_triceps_na_maquina` | sentado na máquina de extensão de tríceps, empurrando as manoplas para baixo · tríceps |
| 28 | Glúteo na Máquina | `ex_gluteo_na_maquina` | na máquina de glúteo (coice), perna empurrando a plataforma para trás · glúteo |
| 29 | Cadeira Adutora | `ex_cadeira_adutora` | sentado, pernas fechando contra as almofadas · adutores (parte interna da coxa) |

Sugestão de folhas: **folha A** = itens 1 a 15 (5 colunas × 3 linhas) e **folha B** = itens 16 a 29 (5 colunas × 3 linhas, a última célula vazia: use `-` no arquivo de chaves).

## 2. Texto base (estilo das folhas)
Há duas folhas com estilos diferentes: a 1 (realista, sombreada) e a 2 (plana, mais limpa, anilhas vermelhas). O app já mistura as duas.
Para as novas, o mais seguro é o estilo da **folha 2 (plano)**; se preferir o da folha 1, troque a linha "Estilo".

```
Folha de ilustrações de exercícios de musculação, grade de 5 colunas por 3 linhas (até 15 desenhos), todos do mesmo tamanho,
bem espaçados entre si, sem nenhum texto, número, legenda ou moldura. Fundo azul-marinho escuro liso (#0B1230).
Estilo: ilustração vetorial plana (flat), personagem sem rosto detalhado, corpo em tons de roxo/azul-violeta (#5B54B0 e #2A2E6E),
sombras simples em blocos, equipamentos em roxo mais claro, anilhas e halteres em vermelho-coral (#F25C54).
O músculo trabalhado é destacado em vermelho-coral (#FF5A52). Cada desenho mostra o corpo inteiro e o equipamento inteiro,
centralizado, vista lateral ou 3/4 (a que mostra melhor o movimento), com ~8% de folga em volta.
Os desenhos da folha, em ordem de leitura (esquerda para a direita, de cima para baixo):
1) ... 2) ... (cole aqui as linhas "Pose e músculo destacado" da tabela: 1 a 15 na folha A, 16 a 29 na folha B)
```

Dicas: se o gerador errar a quantidade de desenhos, peça em folhas menores (por exemplo 5 por vez) e rode o script uma vez por folha.
Peça sempre o fundo **liso** (o script usa a cor das bordas como fundo e remove só o que está ligado a ela).

## 3. Colocar no app
1. Salve a folha (por exemplo `docs/folhas/folha3.png`).
2. Crie um arquivo de chaves, uma por linha, na ordem de leitura (copie da tabela; use `-` para pular uma célula vazia):
   ```
   ex_supino_na_maquina
   ex_remada_na_maquina
   ...
   ```
3. Confira o corte sem nomear: `python3 tools/cortar_folha.py docs/folhas/folha3.png --out /tmp/cortes` (gera `cell_01.png`...; abra e confira a ordem).
4. Gere já com os nomes certos dentro do app:
   `python3 tools/cortar_folha.py docs/folhas/folha3.png --keys chaves.txt --out app/src/main/res/drawable-nodpi`
5. Compile (`./gradlew.bat :app:assembleDebug`). Se faltar alguma, o app cai no ícone do equipamento (nada quebra).

O script exige só Pillow e numpy (`pip install pillow numpy`). Foi testado nas folhas 1 e 2 (30 desenhos cada, centralizados em 384×384 com fundo transparente).

## 4. Exercícios criados pelos usuários (fora do catálogo)
Rode `docs/supabase/consulta_exercicios_de_todos_os_usuarios.sql` e traga as linhas com `no_catalogo = true`. Para cada um,
acrescente uma linha na tabela acima com a pose e o músculo; o nome do arquivo segue a regra do catálogo (minúsculas, sem acento,
`ex_` + nome com `_`). Exercícios fora do catálogo só ganham imagem no app se forem incluídos em `ExerciseCatalog.kt`
(a imagem é buscada pelo nome do catálogo).

## 5. Apelidos: exercícios que os usuários criaram e já usam imagem existente
Estes nomes da listagem dos usuários são o mesmo exercício de um item do catálogo (erro de digitação ou outro jeito de falar), então
o app já mostra a imagem do catálogo por apelido (`aliases` em `ExerciseCatalog.kt`; o nome do exercício do usuário não é alterado):

| Nome criado pelo usuário | Usa a imagem de |
|---|---|
| Agachamento Hack | Hack Squat |
| Agachamento Smith | Agachamento no Smith |
| Elevacao Frontral | Elevação Frontal |
| Elevao lateral | Elevação Lateral |
| Leg Press 45 | Leg Press 45° |
| Panturrilha Maquina | Panturrilha em Pé |
| Puxada Alta | Puxada Frontal na Polia |
| Remada baixa | Remada Baixa na Polia |
| Rosca Scot | Rosca Scott |
| Supino Sentado | Supino na Máquina |
| Crucifixo Invertido na Máquina, Crucifixo Invertido Ombro, Fly Costas | Crucifixo Inverso |
| Fly | Crucifixo na Máquina (Peck Deck) |
| Elevação Lateral Máquina, Glúteo Máquina, Rosca com a barra W, Triceps Coice na Polia Baixa, Triceps Maquina | os novos itens 21, 28, 23, 26 e 27 |

"Fly" e "Fly Costas" foram interpretados como voador (peck deck) e voador inverso; se não for isso, é só ajustar os apelidos.
Nomes de teste ("aaaaa", "teste") foram ignorados.
