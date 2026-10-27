-- 012: dia do treino no fuso de Brasília (e não em UTC) nos gráficos, recordes e números do perfil.
-- Antes: vw_exercise_progression, records_summary() e profile_stats() usavam started_at::date, que converte no fuso da
-- sessão do banco (UTC no Supabase). Um treino às 22h30 de 27/10 em Brasília virava 28/10: a data do recorde, os pontos
-- do gráfico de progressão, as "sessões" do exercício e os "dias ativos" do perfil saíam um dia à frente, e dois treinos
-- em dias diferentes podiam virar a mesma sessão. (Desafios e títulos já usavam o fuso do desafio e não mudam.)
-- Agora: (started_at at time zone 'America/Sao_Paulo')::date. O Brasil não tem horário de verão desde 2019, mas o nome
-- do fuso cuida disso se voltar. Mesmas colunas e tipos de antes: o app não muda.
-- Idempotente. Rodar depois de 001..011 (só depende de 001, 002 e 008).

-- 1) Gráfico de progressão / sessões do exercício (o app consulta exercício por exercício)
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
group by ws.exercise_id, e.name, w.profile_id, (w.started_at at time zone 'America/Sao_Paulo')::date
order by w.profile_id, ws.exercise_id, session_date;

-- 2) Recordes em uma chamada (mesma conta da 008, só muda o dia)
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

-- 3) Números do perfil: "dias ativos" no fuso de Brasília (o resto igual à 002)
create or replace function public.profile_stats(p_profile uuid) returns jsonb
language sql stable security definer set search_path = public as $$
  select case when not can_view_profile(p_profile) then null else (
    select jsonb_build_object(
      'workouts',     count(distinct w.id),
      'active_days',  count(distinct (w.started_at at time zone 'America/Sao_Paulo')::date),
      'sets',         count(s.id),
      'volume_kg',    coalesce(round(sum(s.reps * s.weight_kg)), 0),
      'since',        (select created_at from profiles where id = p_profile))
      from workouts w left join workout_sets s on s.workout_id = w.id
     where w.profile_id = p_profile and w.finished_at is not null) end;
$$;

revoke execute on function public.profile_stats(uuid) from public, anon;
grant execute on function public.profile_stats(uuid) to authenticated;
