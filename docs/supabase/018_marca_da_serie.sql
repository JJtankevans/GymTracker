-- 018: marca da série (aquecimento, drop-set, rest-pause, falha) na nuvem (#603 e #604).
-- Hoje web e app guardam a marca só no aparelho, e o AQUECIMENTO só sai do resumo do treino (web e Android) e das
-- estatísticas da web. No Android, Histórico/Evolução/"Hoje"/recordes vêm do servidor sem o id da série e continuam
-- contando aquecimento. Esta migração põe a marca no banco e faz o servidor ignorar o aquecimento nos números.
-- Aditiva e idempotente. NÃO aplicada (o projeto ygbrrypyydxnuskrobeu é de outra conta). NÃO executada em Postgres
-- local: leia antes de aplicar. Rodar depois de 001..012 (copia as definições da 012 e do gatilho da 002 com um filtro a mais).

-- 1) A coluna. Valores nulos = série normal.
alter table public.workout_sets add column if not exists kind text;
alter table public.workout_sets drop constraint if exists workout_sets_kind_values;
alter table public.workout_sets add constraint workout_sets_kind_values
  check (kind is null or kind in ('warmup', 'drop', 'restpause', 'failure'));
comment on column public.workout_sets.kind is 'Marca da série: warmup (aquecimento, não conta em volume/recordes), drop, restpause, failure. Nulo = normal.';

-- 2) Gráfico de progressão / sessões do exercício (012 + ignora aquecimento)
create or replace view public.vw_exercise_progression
with (security_invoker = true) as
select
  ws.exercise_id,
  e.name                                                   as exercise_name,
  w.profile_id,
  (w.started_at at time zone 'America/Sao_Paulo')::date    as session_date,
  max(ws.weight_kg)                                        as max_weight_kg,
  sum(ws.reps)                                             as total_reps,
  count(*)                                                 as total_sets
from public.workout_sets ws
join public.workouts  w on w.id = ws.workout_id
join public.exercises e on e.id = ws.exercise_id
where ws.kind is distinct from 'warmup'
group by ws.exercise_id, e.name, w.profile_id, (w.started_at at time zone 'America/Sao_Paulo')::date
order by w.profile_id, ws.exercise_id, session_date;

-- 3) Recordes em uma chamada (012 + ignora aquecimento)
create or replace function public.records_summary()
returns table (
  exercise_id      uuid,
  exercise_name    text,
  best_weight_kg   numeric,
  best_date        date,
  best_one_rm_kg   numeric,
  previous_best_kg numeric,
  sessions         integer
)
language sql
stable
security invoker
set search_path = public
as $$
  with dia as (
    select ws.exercise_id,
           (w.started_at at time zone 'America/Sao_Paulo')::date      as session_date,
           max(ws.weight_kg)                                          as max_w,
           least(greatest(floor(sum(ws.reps)::numeric / count(*)), 1), 12) as avg_reps
    from workout_sets ws
    join workouts w on w.id = ws.workout_id
    where w.profile_id = (select auth.uid())
      and ws.kind is distinct from 'warmup'
    group by ws.exercise_id, (w.started_at at time zone 'America/Sao_Paulo')::date
    having max(ws.weight_kg) > 0
  ),
  resumo as (
    select d.exercise_id,
           max(d.max_w)                                                                   as best_w,
           count(*)::int                                                                  as sessions,
           max(case when d.avg_reps <= 1 then d.max_w else d.max_w * (1 + d.avg_reps / 30.0) end) as one_rm
    from dia d
    group by d.exercise_id
  ),
  melhor as (
    select r.*,
           (select min(d.session_date) from dia d where d.exercise_id = r.exercise_id and d.max_w = r.best_w) as best_date
    from resumo r
  )
  select m.exercise_id,
         e.name,
         m.best_w,
         m.best_date,
         m.one_rm,
         (select max(d.max_w) from dia d where d.exercise_id = m.exercise_id and d.session_date < m.best_date),
         m.sessions
  from melhor m
  join exercises e on e.id = m.exercise_id
  order by m.best_date desc, m.best_w desc;
$$;

revoke all on function public.records_summary() from public, anon;
grant execute on function public.records_summary() to authenticated;

-- 4) Números do perfil (012 + ignora aquecimento nas séries e no volume)
create or replace function public.profile_stats(p_profile uuid) returns jsonb
language sql stable security definer set search_path = public as $$
  select case when not can_view_profile(p_profile) then null else (
    select jsonb_build_object(
      'workouts',     count(distinct w.id),
      'active_days',  count(distinct (w.started_at at time zone 'America/Sao_Paulo')::date),
      'sets',         count(s.id) filter (where s.kind is distinct from 'warmup'),
      'volume_kg',    coalesce(round(sum(s.reps * s.weight_kg) filter (where s.kind is distinct from 'warmup')), 0),
      'since',        (select created_at from profiles where id = p_profile))
      from workouts w left join workout_sets s on s.workout_id = w.id
     where w.profile_id = p_profile and w.finished_at is not null) end;
$$;

revoke execute on function public.profile_stats(uuid) from public, anon;
grant execute on function public.profile_stats(uuid) to authenticated;

-- 5) Post do feed quando o treino termina (002 + ignora aquecimento nas estatísticas do post)
create or replace function trg_workout_to_post() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  prof profiles%rowtype;
  st   jsonb;
begin
  if old.finished_at is not null or new.finished_at is null then return new; end if;
  select * into prof from profiles where id = new.profile_id;
  if not found or not prof.share_workouts then return new; end if;

  select jsonb_build_object(
           'minutes',   greatest(1, round(extract(epoch from (new.finished_at - new.started_at)) / 60)),
           'sets',      count(s.id),
           'reps',      coalesce(sum(s.reps), 0),
           'volume_kg', coalesce(sum(s.reps * s.weight_kg), 0),
           'exercises', count(distinct s.exercise_id),
           'group',     (select name from muscle_groups where id = new.muscle_group_id))
    into st from workout_sets s where s.workout_id = new.id and s.kind is distinct from 'warmup';

  insert into feed_posts (profile_id, workout_id, visibility, stats)
  values (new.profile_id, new.id,
          case prof.visibility when 'public' then 'public' else 'followers' end, st)
  on conflict (workout_id) do nothing;
  return new;
end $$;

-- Fora desta migração (de propósito): challenge_standings() (002/010), por ser longa e já ter regras próprias
-- (mínimo de minutos, máximo de treinos por dia). Se quiser que aquecimento não conte em desafios, acrescente
-- `and s.kind is distinct from 'warmup'` onde ela soma séries, repetições e volume.

-- Depois de aplicar:
--  * Web e app passam a gravar `kind` ao registrar a série e a ler a marca do banco (hoje: localStorage / SharedPreferences);
--  * no Android, `RecentSet`/`ExportSet` ganham o campo `kind` para o Histórico e as Análises também ignorarem aquecimento;
--  * `Hoje` (carga da semana) e as análises passam a vir sem aquecimento sem mais nenhuma mudança.
