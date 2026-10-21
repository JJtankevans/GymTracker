-- Exercícios de TODOS os usuários, sem repetição (rodar no SQL Editor do Supabase; ele roda como dono e
-- ignora a RLS, por isso enxerga os exercícios de todo mundo). Só lê, não altera nada.
--
-- Uma linha por exercício distinto (nome sem caixa/acento/espaços nas pontas, como o app compara).
-- no_catalogo = false  -> já está no catálogo do app (70 itens; 41 têm imagem, 29 não)
-- no_catalogo = true   -> criado à mão por algum usuário: são esses que ainda precisam de imagem
-- Os que não estão no catálogo vêm primeiro. Depois do 006, os grupos vêm de group_exercises.

with catalogo(chave) as (values
    ('supino reto com barra'),
    ('supino inclinado com barra'),
    ('supino reto com halteres'),
    ('supino inclinado com halteres'),
    ('crucifixo com halteres'),
    ('supino na maquina'),
    ('crucifixo na maquina (peck deck)'),
    ('crossover na polia'),
    ('flexao de braco'),
    ('supino inclinado na maquina'),
    ('supino declinado na maquina'),
    ('supino guiado'),
    ('barra fixa'),
    ('puxada frontal na polia'),
    ('remada baixa na polia'),
    ('remada curvada com barra'),
    ('levantamento terra'),
    ('remada unilateral com halter'),
    ('remada na maquina'),
    ('pulldown na polia'),
    ('remada cavalinho'),
    ('remada cavalinho na maquina'),
    ('desenvolvimento com barra'),
    ('desenvolvimento com halteres'),
    ('desenvolvimento na maquina'),
    ('elevacao lateral'),
    ('elevacao frontal'),
    ('crucifixo inverso'),
    ('encolhimento com halteres'),
    ('elevacao lateral na maquina'),
    ('face pull'),
    ('rosca direta com barra'),
    ('rosca alternada com halteres'),
    ('rosca martelo'),
    ('rosca scott'),
    ('rosca na polia'),
    ('rosca com barra w'),
    ('rosca concentrada'),
    ('rosca martelo na polia'),
    ('triceps testa'),
    ('triceps pulley'),
    ('triceps corda'),
    ('triceps frances'),
    ('mergulho nas paralelas'),
    ('supino fechado'),
    ('triceps coice na polia'),
    ('triceps na maquina'),
    ('agachamento livre'),
    ('agachamento no smith'),
    ('leg press 45°'),
    ('hack squat'),
    ('cadeira extensora'),
    ('agachamento bulgaro'),
    ('afundo com halteres'),
    ('mesa flexora'),
    ('cadeira flexora'),
    ('stiff com barra'),
    ('levantamento terra romeno'),
    ('elevacao pelvica'),
    ('cadeira abdutora'),
    ('gluteo na polia (coice)'),
    ('gluteo na maquina'),
    ('cadeira adutora'),
    ('panturrilha em pe'),
    ('panturrilha sentado'),
    ('panturrilha no leg press'),
    ('abdominal supra'),
    ('elevacao de pernas'),
    ('prancha'),
    ('abdominal na polia')
),
ex as (
  select e.id, e.profile_id, e.name, e.default_sets, e.default_reps, e.notes, e.created_at,
         translate(lower(btrim(e.name)), 'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc') as chave
  from public.exercises e
)
select
  mode() within group (order by ex.name)                          as nome,
  not bool_or(ex.chave in (select chave from catalogo))          as no_catalogo,
  count(distinct ex.profile_id)                                   as usuarios,
  round(avg(ex.default_sets))                                     as series_padrao,
  round(avg(ex.default_reps))                                     as reps_padrao,
  string_agg(distinct g.name, ', ')                               as grupos,
  string_agg(distinct nullif(btrim(ex.notes), ''), ' | ')         as observacoes,
  min(ex.created_at)::date                                        as primeira_vez
from ex
left join public.group_exercises ge on ge.exercise_id = ex.id
left join public.muscle_groups   g  on g.id = ge.group_id
group by ex.chave
order by no_catalogo desc, nome;
