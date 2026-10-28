-- 015: liga da semana — ranking entre você e quem você segue (seguir aceito), de segunda a domingo no fuso de Brasília.
-- Uma chamada só: weekly_league() devolve uma linha por pessoa com treinos terminados, dias ativos, volume (kg) e minutos
-- na semana. Respeita a privacidade do perfil (can_view_profile, da 002): quem não pode ser visto não aparece.
-- O app mostra o cartão "Liga da semana" no Social; sem esta migração o cartão simplesmente não aparece.
-- Idempotente. Depende de 002 (follows, can_view_profile) e 012 (o fuso).

create or replace function public.weekly_league()
returns table (
  profile_id  uuid,
  username    text,
  avatar_url  text,
  workouts    int,
  active_days int,
  volume_kg   numeric,
  minutes     int,
  is_me       boolean
)
language sql stable security definer set search_path = public as $$
  with wk as (
    select date_trunc('week', now() at time zone 'America/Sao_Paulo')::date as start
  ),
  people as (
    select auth.uid() as id
    union
    select f.followed_id from follows f where f.follower_id = auth.uid() and f.status = 'accepted'
  ),
  ws as (
    select w.id, w.profile_id, w.started_at, w.finished_at
      from workouts w, wk
     where w.profile_id in (select id from people)
       and w.finished_at is not null
       and (w.started_at at time zone 'America/Sao_Paulo')::date >= wk.start
       and (w.started_at at time zone 'America/Sao_Paulo')::date <  wk.start + 7
  ),
  vol as (
    select s.workout_id, sum(s.reps * s.weight_kg) as v
      from workout_sets s
     where s.workout_id in (select id from ws)
     group by s.workout_id
  )
  select p.id,
         p.username,
         p.avatar_url,
         count(ws.id)::int,
         count(distinct (ws.started_at at time zone 'America/Sao_Paulo')::date)::int,
         coalesce(round(sum(vol.v)), 0)::numeric,
         coalesce(round(sum(extract(epoch from (ws.finished_at - ws.started_at)) / 60)), 0)::int,
         p.id = auth.uid()
    from people pe
    join profiles p on p.id = pe.id
    left join ws  on ws.profile_id = p.id
    left join vol on vol.workout_id = ws.id
   where auth.uid() is not null
     and (p.id = auth.uid() or can_view_profile(p.id))
   group by p.id, p.username, p.avatar_url
   order by 4 desc, 5 desc, 6 desc, 2;
$$;

revoke all on function public.weekly_league() from public, anon;
grant execute on function public.weekly_league() to authenticated;
