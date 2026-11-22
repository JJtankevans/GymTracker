-- 017: supersérie/bi-set (#575) e cadência/tempo sob tensão (#581) na nuvem.
-- Hoje a web e o app guardam isso só no aparelho; esta migração deixa o banco pronto para sincronizar entre aparelhos.
-- Aditiva e idempotente. NÃO aplicada (o projeto ygbrrypyydxnuskrobeu é de outra conta).

-- Cadência da série: "descida-pausa embaixo-subida-pausa em cima", um dígito de 0 a 9 por fase (ex.: 3-1-2-0).
alter table public.workout_sets add column if not exists tempo text;
alter table public.workout_sets drop constraint if exists workout_sets_tempo_format;
alter table public.workout_sets add constraint workout_sets_tempo_format
  check (tempo is null or tempo ~ '^[0-9]-[0-9]-[0-9]-[0-9]$');
comment on column public.workout_sets.tempo is 'Cadência da série (ex.: 3-1-2-0). TUT = reps x soma das fases; opcional.';

-- Supersérie: mapa {exercise_id: true} dos exercícios ligados ao PRÓXIMO da lista, por treino.
alter table public.workouts add column if not exists superset_links jsonb not null default '{}'::jsonb;
alter table public.workouts drop constraint if exists workouts_superset_links_object;
alter table public.workouts add constraint workouts_superset_links_object
  check (jsonb_typeof(superset_links) = 'object');
comment on column public.workouts.superset_links is 'Exercícios ligados ao próximo (supersérie/bi-set). Formato {"<exercise_id>": true}.';

-- RLS: as políticas existentes de workouts e workout_sets (dono do treino) já cobrem as colunas novas; nada a criar.
-- Depois de aplicar: web e app passam a gravar/ler `tempo` e `superset_links` (hoje só localStorage/SharedPreferences).
