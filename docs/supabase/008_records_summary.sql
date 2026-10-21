-- 008: recordes pessoais calculados no servidor, em UMA chamada.
-- Antes: a tela de Recordes buscava a lista de exercícios e depois pedia a view vw_exercise_progression
-- uma vez POR EXERCÍCIO, em sequência (N+1). Com ~50 exercícios eram ~50 idas e voltas ao servidor.
-- Agora: records_summary() devolve 1 linha por exercício com carga (melhor carga, dia em que bateu, recorde
-- anterior, 1RM estimado e nº de sessões). A conta é a mesma de buildRecordRow (PersonalRecords.kt):
--   * 1 sessão = 1 dia de treino do exercício (started_at::date, como a view); só entram dias com carga > 0;
--   * recorde "nasce" no primeiro dia que chegou na melhor carga; o anterior é o maior de antes desse dia;
--   * 1RM (Epley): carga x (1 + reps/30), com reps = média de reps por série do dia (truncada, entre 1 e 12);
--     com 1 rep é a própria carga.
-- SECURITY INVOKER: roda como o usuário logado, então a RLS continua valendo (e o filtro por auth.uid() ajuda o plano).
-- Idempotente. Rodar depois de 001..007.

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
           w.started_at::date                                          as session_date,
           max(ws.weight_kg)                                           as max_w,
           least(greatest(floor(sum(ws.reps)::numeric / count(*)), 1), 12) as avg_reps
    from workout_sets ws
    join workouts w on w.id = ws.workout_id
    where w.profile_id = (select auth.uid())
    group by ws.exercise_id, w.started_at::date
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

-- só usuário logado chama
revoke all on function public.records_summary() from public, anon;
grant execute on function public.records_summary() to authenticated;
