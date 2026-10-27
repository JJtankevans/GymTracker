-- 010: títulos de desafio no perfil (honra e falha).
-- Ao criar o desafio o criador pode definir (ambos opcionais):
--   fail_threshold  x: quem acumular MAIS de x treinos abaixo do mínimo de minutos do desafio ganha um título de falha;
--   streak_goal     y: quem mantiver y dias SEGUIDOS com treino válido ganha "Alpha"; 2y "Chad"; 3y "Giga Chad"; 4y "Mr. Olimpia" (máximo).
-- Regras (combinadas com o Thiago):
--   * falha = treino terminado dentro do desafio com duração menor que min_minutes_per_workout (dia sem treino NÃO é falha);
--   * dia válido = dia (no fuso do desafio) com pelo menos 1 treino terminado dentro do desafio e com o mínimo de minutos;
--   * a sequência precisa estar viva: terminou hoje ou ontem (desafio em andamento) ou no último dia do desafio (encerrado);
--     se o usuário deixa de treinar, a sequência zera e o título de honra some;
--   * o título aparece enquanto o desafio está em andamento e até 7 dias depois do fim;
--   * se o usuário tem honra e falha ao mesmo tempo, vale a honra mais alta; sem honra, vale a falha mais recente;
--   * quem pode ver: as mesmas regras do perfil (can_view_profile: público para todos, "só seguidores" para quem segue, privado só o dono).
-- O app escolhe o texto (nomes de honra e a frase do título de falha); o banco só devolve o tipo e o nível.
-- Idempotente. Rodar depois de 001..009.

alter table public.challenges
  add column if not exists fail_threshold integer check (fail_threshold is null or fail_threshold between 1 and 100),
  add column if not exists streak_goal    integer check (streak_goal    is null or streak_goal    between 1 and 365);

create or replace function public.profile_titles(p_profiles uuid[])
returns table (
  profile_id      uuid,
  kind            text,      -- 'honor' | 'fail'
  level           integer,   -- honra: 1 Alpha, 2 Chad, 3 Giga Chad, 4 Mr. Olimpia; falha: 0
  challenge_id    uuid,
  challenge_title text,      -- null se quem consulta não pode ver o desafio
  streak_days     integer,
  failures        integer
)
language sql
stable
security definer
set search_path = public
as $$
  with cand as (
    select c.id, c.title, c.tz, c.starts_at, c.ends_at, c.min_minutes_per_workout as min_min,
           c.streak_goal, c.fail_threshold, cp.profile_id as pid
      from challenges c
      join challenge_participants cp on cp.challenge_id = c.id
     where cp.profile_id = any (p_profiles)
       and (c.fail_threshold is not null or c.streak_goal is not null)
       and c.starts_at <= now()
       and now() < c.ends_at + interval '7 days'
       and can_view_profile(cp.profile_id)
  ),
  wk as (   -- treinos terminados dentro do desafio, com o dia no fuso do desafio e os minutos
    select cand.id as cid, cand.pid, cand.min_min,
           (w.started_at at time zone cand.tz)::date as d,
           extract(epoch from (w.finished_at - w.started_at)) / 60 as minutes
      from cand
      join workouts w on w.profile_id = cand.pid
     where w.finished_at is not null
       and w.started_at >= cand.starts_at
       and w.finished_at <= cand.ends_at
  ),
  falhas as (
    select cid, pid, count(*) as n from wk where minutes < min_min group by cid, pid
  ),
  dias as (
    select distinct cid, pid, d from wk where minutes >= min_min
  ),
  ilhas as (   -- dias seguidos: a diferença entre a data e o número da linha é igual dentro de uma sequência
    select cid, pid, d, d - (row_number() over (partition by cid, pid order by d))::int as g from dias
  ),
  seqs as (
    select cid, pid, count(*) as len, max(d) as last_d from ilhas group by cid, pid, g
  ),
  viva as (    -- sequência que ainda não quebrou
    select s.cid, s.pid, max(s.len) as len
      from seqs s
      join cand on cand.id = s.cid and cand.pid = s.pid
     where s.last_d >= least((now() at time zone cand.tz)::date, (cand.ends_at at time zone cand.tz)::date)
                       - (case when now() < cand.ends_at then 1 else 0 end)
     group by s.cid, s.pid
  ),
  scored as (
    select cand.*, coalesce(f.n, 0) as falhas, coalesce(v.len, 0) as seq_len
      from cand
      left join falhas f on f.cid = cand.id and f.pid = cand.pid
      left join viva   v on v.cid = cand.id and v.pid = cand.pid
  )
  select distinct on (r.pid)
         r.pid, r.kind, r.level, r.cid,
         case when can_view_challenge(r.cid) then r.title end,
         r.streak_days, r.failures
    from (
      select pid, 'honor'::text as kind, least(4, seq_len / streak_goal)::int as level, id as cid, title,
             seq_len::int as streak_days, falhas::int as failures, ends_at, 1 as pri
        from scored
       where streak_goal is not null and seq_len >= streak_goal
      union all
      select pid, 'fail'::text, 0, id, title, seq_len::int, falhas::int, ends_at, 2
        from scored
       where fail_threshold is not null and falhas > fail_threshold
    ) r
   order by r.pid, r.pri, r.level desc, r.failures desc, r.ends_at desc;
$$;

revoke all on function public.profile_titles(uuid[]) from public, anon;
grant execute on function public.profile_titles(uuid[]) to authenticated;
