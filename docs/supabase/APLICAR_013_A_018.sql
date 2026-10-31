-- GymTracker: migrações 013 a 018 juntas, na ordem. Cole TUDO no SQL Editor do projeto ygbrrypyydxnuskrobeu e clique em Run.
-- Todas são idempotentes (dá para rodar de novo sem estragar). Pressupõe que a 001 a 012 já foram aplicadas.
--   013 horários do treino protegidos   014 medidas corporais na nuvem   015 liga da semana
--   016 excluir a própria conta         017 personalizar grupos e desafios   018 ranqueada (elos, turmas e x1)
-- Se algo der erro no meio, anote a mensagem e o número do trecho: o que veio antes já foi aplicado e pode ficar.


-- ══════════════════════════════════════════════════════════════════════
-- 013_horarios_do_treino_protegidos.sql
-- ══════════════════════════════════════════════════════════════════════

-- 013: horários do treino protegidos (erro nº 24 de docs/ERROS_CONHECIDOS.md, "anti-trapaça").
-- Antes: a RLS deixa o dono alterar qualquer coluna dos próprios treinos pela API. Mudando started_at/finished_at
-- dá para inflar minutos, mudar o dia do treino e assim mexer em placares e títulos de desafio.
-- Agora (gatilho BEFORE INSERT OR UPDATE em workouts):
--   * started_at não muda depois de criado e não pode estar no futuro (folga de 5 min para relógio adiantado);
--   * finished_at só pode ser gravado uma vez (fechar o treino), nunca antes do início e nunca no futuro
--     (se vier no futuro, vira now());
-- O app não muda: ele só fecha treino uma vez (finishWorkout grava now(); o relógio grava a hora em que terminou,
-- que é passado). Notas e finished_exercise_ids continuam editáveis.
-- Idempotente. Depende só da tabela workouts (001).

create or replace function public.trg_workout_times_guard() returns trigger
language plpgsql as $$
begin
  if tg_op = 'INSERT' then
    if new.started_at is null then new.started_at := now(); end if;
    if new.started_at > now() + interval '5 minutes' then
      raise exception 'O início do treino não pode estar no futuro' using errcode = 'check_violation';
    end if;
  else
    if new.started_at is distinct from old.started_at then
      raise exception 'O início do treino não pode ser alterado' using errcode = 'check_violation';
    end if;
    if old.finished_at is not null and new.finished_at is distinct from old.finished_at then
      raise exception 'O fim do treino não pode ser alterado depois de fechado' using errcode = 'check_violation';
    end if;
  end if;

  if new.finished_at is not null then
    if new.finished_at > now() then new.finished_at := now(); end if;
    if new.finished_at < new.started_at then
      raise exception 'O fim do treino não pode ser antes do início' using errcode = 'check_violation';
    end if;
  end if;
  return new;
end $$;

drop trigger if exists trg_workout_times_guard on public.workouts;
create trigger trg_workout_times_guard
  before insert or update on public.workouts
  for each row execute function public.trg_workout_times_guard();


-- ══════════════════════════════════════════════════════════════════════
-- 014_medidas_corporais.sql
-- ══════════════════════════════════════════════════════════════════════

-- 014: medidas corporais com fita (cintura, quadril, peito, braço, coxa, panturrilha) na nuvem.
-- Antes: ficavam só no aparelho e eram apagadas ao sair da conta (LocalUserData). Agora o app sincroniza
-- como o peso (BodyRepository.sync): um registro por dia e local, o aparelho vence no mesmo dia.
-- Sem esta migração o app continua funcionando: as medidas só não sobem (o erro é ignorado).
-- Idempotente. Rodar depois de 003.

create table if not exists public.body_measurements (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  measured_on date not null,
  site text not null check (site in ('WAIST', 'HIPS', 'CHEST', 'ARM', 'THIGH', 'CALF')),
  cm numeric(5,1) not null check (cm between 10 and 300),
  created_at timestamptz not null default now(),
  unique (profile_id, measured_on, site)    -- uma medida por dia e local
);

create index if not exists idx_body_measurements_profile_date on public.body_measurements (profile_id, measured_on desc);

alter table public.body_measurements enable row level security;

-- Dado pessoal: só o dono lê e escreve.
drop policy if exists "dono lê suas medidas" on public.body_measurements;
create policy "dono lê suas medidas" on public.body_measurements
  for select using (profile_id = auth.uid());
drop policy if exists "dono registra suas medidas" on public.body_measurements;
create policy "dono registra suas medidas" on public.body_measurements
  for insert with check (profile_id = auth.uid());
drop policy if exists "dono altera suas medidas" on public.body_measurements;
create policy "dono altera suas medidas" on public.body_measurements
  for update using (profile_id = auth.uid()) with check (profile_id = auth.uid());
drop policy if exists "dono apaga suas medidas" on public.body_measurements;
create policy "dono apaga suas medidas" on public.body_measurements
  for delete using (profile_id = auth.uid());


-- ══════════════════════════════════════════════════════════════════════
-- 015_liga_da_semana.sql
-- ══════════════════════════════════════════════════════════════════════

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


-- ══════════════════════════════════════════════════════════════════════
-- 016_excluir_conta.sql
-- ══════════════════════════════════════════════════════════════════════

-- 016: excluir a própria conta (exigência da LGPD e da Play Store para apps com cadastro).
-- O app chama rpc("delete_my_account") depois de remover a foto de perfil pelo Storage (o Supabase não deixa apagar
-- arquivos por SQL). Apagar a linha de auth.users leva junto, em cascata, o perfil e TUDO que é do usuário: grupos,
-- exercícios, treinos e séries, peso e medidas, seguidores, posts, reações, participações, treinos compartilhados
-- e as faixas de música antigas. Desafios e grupos que ele CRIOU somem para todos (creator_id/owner_id em cascata).
-- Sem esta migração o app continua funcionando: o botão "Excluir minha conta" só mostra um aviso de erro.
-- Idempotente. Não precisa de nenhuma outra migração antes.

create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'Entre na sua conta para excluí-la' using errcode = '28000';
  end if;
  delete from auth.users where id = uid;
end;
$$;

-- só quem está logado chama; o definer é o dono da função (que pode apagar em auth.users)
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;


-- ══════════════════════════════════════════════════════════════════════
-- 017_personalizar_grupos_e_desafios.sql
-- ══════════════════════════════════════════════════════════════════════

-- 017: personalização de grupos e desafios.
--  - grupos:   capa (id de tema, o app desenha o fundo animado), cor de destaque, ícone, aviso fixado, meta semanal de treinos;
--  - desafios: capa, cor, ícone e prêmio ("quem perder paga o açaí").
--  - group_week_progress(grupo): treinos dos membros na semana (segunda a domingo, fuso de Brasília) para a barra da meta.
-- Sem esta migração o app continua funcionando: grupos e desafios só ficam sem capa/cor/ícone e quem tentar personalizar
-- vê o aviso de que falta rodar a 017. Idempotente. Depende de 002 (groups, group_members, is_group_member) e 012.

alter table public.groups
  add column if not exists cover          text check (cover is null or char_length(cover) <= 30),
  add column if not exists accent_hex     text check (accent_hex is null or accent_hex ~ '^#[0-9A-Fa-f]{6}$'),
  add column if not exists icon           text check (icon is null or char_length(icon) <= 20),
  add column if not exists pinned_message text check (pinned_message is null or char_length(pinned_message) <= 300),
  add column if not exists weekly_goal    integer check (weekly_goal is null or weekly_goal between 1 and 500);

alter table public.challenges
  add column if not exists cover      text check (cover is null or char_length(cover) <= 30),
  add column if not exists accent_hex text check (accent_hex is null or accent_hex ~ '^#[0-9A-Fa-f]{6}$'),
  add column if not exists icon       text check (icon is null or char_length(icon) <= 20),
  add column if not exists prize      text check (prize is null or char_length(prize) <= 120);

-- Treinos terminados dos membros do grupo na semana. Só quem é membro consulta; devolve contagens, nunca quem treinou.
create or replace function public.group_week_progress(p_group uuid)
returns table (workouts int, members int, active_members int)
language sql stable security definer set search_path = public as $$
  with wk as (
    select date_trunc('week', now() at time zone 'America/Sao_Paulo')::date as start
  ),
  mem as (
    select gm.profile_id from group_members gm where gm.group_id = p_group
  ),
  ws as (
    select w.id, w.profile_id
      from workouts w, wk
     where w.profile_id in (select profile_id from mem)
       and w.finished_at is not null
       and (w.started_at at time zone 'America/Sao_Paulo')::date >= wk.start
       and (w.started_at at time zone 'America/Sao_Paulo')::date <  wk.start + 7
  )
  select (select count(*) from ws)::int,
         (select count(*) from mem)::int,
         (select count(distinct profile_id) from ws)::int
   where auth.uid() is not null
     and is_group_member(p_group);
$$;

revoke all on function public.group_week_progress(uuid) from public, anon;
grant execute on function public.group_week_progress(uuid) to authenticated;


-- ══════════════════════════════════════════════════════════════════════
-- 018_ranqueada.sql
-- ══════════════════════════════════════════════════════════════════════

-- 018: ranqueada (liga com elos), turmas semanais e x1. Ver docs/RANQUEADA_DESIGN.md.
--
-- Os PONTOS vêm dos treinos reais (workouts + workout_sets), calculados aqui no banco: ninguém manda o próprio número.
-- As contas são as mesmas de shared/.../presentation/ranked/RankedRules.kt (o app usa aquele arquivo só para mostrar e
-- testar). Mudou uma regra? Mude as duas pontas.
--
--  * Elo: rp 0..1799 = Ferro..Diamante (3 divisões de 100 RP); 1800+ = Lenda. tier = least(rp / 300, 6).
--  * Semana (segunda a domingo, fuso de Brasília): cada pessoa entra numa turma de até 30 do MESMO elo. No fechamento,
--    20% do topo sobe, 20% do fundo desce, quem não treinou perde RP (ranked_delta).
--  * x1: duelo de 7 dias entre dois do mesmo elo; vale RP (Elo, K=24) ou é amistoso.
--  * Temporada de 8 semanas (início 26/10/2026); no fim, o RP encolhe para 60%.
--
-- Sem esta migração o app funciona: a aba "Ranqueada" mostra que ainda não está ativada. Idempotente.
-- Depende de 002 (profiles.weekly_goal vem do esquema base, workouts, workout_sets) e 012 (fuso).

-- ───────────────────────── tabelas ─────────────────────────
create table if not exists public.ranked_players (
  profile_id   uuid primary key references public.profiles(id) on delete cascade,
  rp           integer not null default 0 check (rp >= 0),
  peak_rp      integer not null default 0,
  weeks_played integer not null default 0,
  joined_at    timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table if not exists public.ranked_cohorts (
  id         uuid primary key default gen_random_uuid(),
  week_start date not null,
  tier       integer not null check (tier between 0 and 6),
  created_at timestamptz not null default now()
);
create index if not exists idx_ranked_cohorts_week_tier on public.ranked_cohorts (week_start, tier);

create table if not exists public.ranked_entries (
  week_start date    not null,
  profile_id uuid    not null references public.profiles(id) on delete cascade,
  cohort_id  uuid    not null references public.ranked_cohorts(id) on delete cascade,
  tier       integer not null,
  points     integer,          -- preenchidos no fechamento da semana
  position   integer,
  delta      integer,
  rp_after   integer,
  primary key (week_start, profile_id)
);
create index if not exists idx_ranked_entries_cohort on public.ranked_entries (cohort_id);

create table if not exists public.ranked_weeks (
  week_start date primary key,
  settled_at timestamptz not null default now()
);

create table if not exists public.ranked_duels (
  id               uuid primary key default gen_random_uuid(),
  challenger_id    uuid not null references public.profiles(id) on delete cascade,
  opponent_id      uuid not null references public.profiles(id) on delete cascade,
  ranked           boolean not null default true,
  status           text not null default 'pending' check (status in ('pending','active','declined','cancelled','finished')),
  starts_at        timestamptz,
  ends_at          timestamptz,
  pt_challenger    integer,
  pt_opponent      integer,
  winner_id        uuid,
  delta_challenger integer,
  delta_opponent   integer,
  created_at       timestamptz not null default now(),
  check (challenger_id <> opponent_id)
);
create index if not exists idx_ranked_duels_challenger on public.ranked_duels (challenger_id, status);
create index if not exists idx_ranked_duels_opponent   on public.ranked_duels (opponent_id, status);

alter table public.ranked_players enable row level security;
alter table public.ranked_cohorts enable row level security;
alter table public.ranked_entries enable row level security;
alter table public.ranked_weeks   enable row level security;
alter table public.ranked_duels   enable row level security;

-- o elo de todo mundo é público para quem está logado; o resto só pelas funções abaixo
drop policy if exists ranked_players_select on public.ranked_players;
create policy ranked_players_select on public.ranked_players for select to authenticated using (true);
drop policy if exists ranked_entries_select on public.ranked_entries;
create policy ranked_entries_select on public.ranked_entries for select to authenticated using (profile_id = auth.uid());
drop policy if exists ranked_duels_select on public.ranked_duels;
create policy ranked_duels_select on public.ranked_duels for select to authenticated
  using (challenger_id = auth.uid() or opponent_id = auth.uid());

-- ───────────────────────── contas puras ─────────────────────────
create or replace function public.ranked_tier_of(p_rp integer) returns integer
language sql immutable as $$ select least(greatest(p_rp, 0) / 300, 6) $$;

-- pontos de um treino: 0 se <20 min ou >240 min; senão 100 + esforço (até 30) + 2 por série com repetição (até 20)
create or replace function public.ranked_workout_points(p_minutes double precision, p_sets integer) returns integer
language sql immutable as $$
  select case when p_minutes < 20 or p_minutes > 240 then 0
              else 100 + least(30, floor((least(p_minutes, 80) - 20) / 2))::int + 2 * least(20, greatest(0, p_sets))
         end
$$;

-- variação de RP no fechamento da semana (posição 1 = melhor; size = tamanho da turma)
create or replace function public.ranked_delta(p_pos integer, p_size integer, p_points integer) returns integer
language sql immutable as $$
  select case
    when p_points <= 0 then -35
    when p_size < 5 then case when p_points >= 100 then 10 else -20 end
    when p_pos = 1 then 40
    when p_pos <= (p_size * 20 + 99) / 100 then 30
    when p_pos > p_size - (p_size * 20 + 99) / 100 then -25
    when p_pos <= (p_size + 1) / 2 then 10
    else -10
  end
$$;

-- variação de RP de quem está em p_rp_self num x1 (Elo, K = 24, limitado a +-35)
create or replace function public.ranked_duel_delta(p_rp_self integer, p_rp_opp integer, p_pt_self integer, p_pt_opp integer, p_ranked boolean)
returns integer language sql immutable as $$
  select case when not p_ranked or (p_pt_self = 0 and p_pt_opp = 0) then 0
    else greatest(-35, least(35, floor(
      24 * ((case when p_pt_self > p_pt_opp then 1.0::float8 when p_pt_self < p_pt_opp then 0.0::float8 else 0.5::float8 end)
            - 1.0::float8 / (1.0::float8 + power(10.0::float8, (p_rp_opp - p_rp_self)::float8 / 400.0::float8))) + 0.5::float8
    )::int)) end
$$;

-- pontos de treino de uma pessoa nos dias [p_from, p_to) (fuso de Brasília): o melhor treino de cada dia,
-- os 6 melhores dias, +50 por treinar em 3+ dias, +50 por bater a meta semanal do perfil (dias com treino)
create or replace function public.ranked_points(p_profile uuid, p_from date, p_to date) returns integer
language sql stable security definer set search_path = public as $$
  with ws as (
    select w.id,
           (w.started_at at time zone 'America/Sao_Paulo')::date as d,
           (extract(epoch from (w.finished_at - w.started_at)) / 60.0)::float8 as mins
      from workouts w
     where w.profile_id = p_profile
       and w.finished_at is not null
       and (w.started_at at time zone 'America/Sao_Paulo')::date >= p_from
       and (w.started_at at time zone 'America/Sao_Paulo')::date <  p_to
  ),
  nsets as (
    select s.workout_id, count(*) filter (where s.reps > 0)::int as n
      from workout_sets s
     where s.workout_id in (select id from ws)
     group by s.workout_id
  ),
  scored as (
    select ws.d, ranked_workout_points(ws.mins, coalesce(nsets.n, 0)) as pts
      from ws left join nsets on nsets.workout_id = ws.id
  ),
  per_day as (select d, max(pts) as pts from scored where pts > 0 group by d),
  top6    as (select pts from per_day order by pts desc limit 6),
  goal    as (select coalesce(weekly_goal, 0) as g from profiles where id = p_profile)
  select case when (select count(*) from per_day) = 0 then 0
         else coalesce((select sum(pts) from top6), 0)::int
              + case when (select count(*) from per_day) >= 3 then 50 else 0 end
              + case when (select g from goal) > 0 and (select count(*) from per_day) >= (select g from goal) then 50 else 0 end
         end
$$;

-- segunda-feira da semana atual no fuso de Brasília
create or replace function public.ranked_current_week() returns date
language sql stable as $$ select date_trunc('week', now() at time zone 'America/Sao_Paulo')::date $$;

-- ───────────────────────── turmas ─────────────────────────
-- Põe a pessoa numa turma da semana: a primeira do mesmo elo com menos de 30; senão cria outra. Interna.
create or replace function public.ranked_assign(p_profile uuid, p_week date) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_tier int;
  v_cohort uuid;
begin
  select cohort_id into v_cohort from ranked_entries where week_start = p_week and profile_id = p_profile;
  if v_cohort is not null then return v_cohort; end if;
  select ranked_tier_of(rp) into v_tier from ranked_players where profile_id = p_profile;
  if v_tier is null then raise exception 'Entre na ranqueada primeiro' using errcode = 'P0001'; end if;
  perform pg_advisory_xact_lock(hashtext('ranked_cohort:' || p_week::text || ':' || v_tier::text));
  select c.id into v_cohort
    from ranked_cohorts c
   where c.week_start = p_week and c.tier = v_tier
     and (select count(*) from ranked_entries e where e.cohort_id = c.id) < 30
   order by c.created_at, c.id
   limit 1;
  if v_cohort is null then
    insert into ranked_cohorts(week_start, tier) values (p_week, v_tier) returning id into v_cohort;
  end if;
  insert into ranked_entries(week_start, profile_id, cohort_id, tier) values (p_week, p_profile, v_cohort, v_tier)
    on conflict do nothing;
  return v_cohort;
end $$;
revoke all on function public.ranked_assign(uuid, date) from public, anon, authenticated;

-- Coloca na turma da semana todo mundo que já era da ranqueada e ainda não tem turma (montadas por elo, em ordem fixa).
-- Interna. Assim o placar é o mesmo para todos desde a primeira consulta da semana, e faltar não protege de cair.
create or replace function public.ranked_assign_all(p_week date) returns void
language plpgsql security definer set search_path = public as $$
declare r record;
begin
  for r in
    select rpl.profile_id from ranked_players rpl
     where rpl.joined_at < (p_week + 7)::timestamp
       and not exists (select 1 from ranked_entries e where e.week_start = p_week and e.profile_id = rpl.profile_id)
     order by ranked_tier_of(rpl.rp), rpl.profile_id
  loop
    perform ranked_assign(r.profile_id, p_week);
  end loop;
end $$;
revoke all on function public.ranked_assign_all(date) from public, anon, authenticated;

-- Entra na ranqueada (começa em Ferro III, 0 RP) e já cai na turma da semana.
create or replace function public.ranked_join() returns integer
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'Entre na sua conta' using errcode = '28000'; end if;
  insert into ranked_players(profile_id) values (auth.uid()) on conflict do nothing;
  perform ranked_assign(auth.uid(), ranked_current_week());
  return (select rp from ranked_players where profile_id = auth.uid());
end $$;
revoke all on function public.ranked_join() from public, anon;
grant execute on function public.ranked_join() to authenticated;

-- A turma da semana de quem chama, com os pontos de agora. Quem ainda não está na turma é colocado nela.
create or replace function public.ranked_board(p_week date default null)
returns table (pos integer, profile_id uuid, username text, avatar_url text, rp integer, points integer, is_me boolean)
language plpgsql security definer set search_path = public as $$
declare
  v_week date := coalesce(p_week, ranked_current_week());
  v_cohort uuid;
begin
  if auth.uid() is null then return; end if;
  if not exists (select 1 from ranked_players where ranked_players.profile_id = auth.uid()) then return; end if;
  if v_week = ranked_current_week() then
    perform ranked_assign_all(v_week);
    v_cohort := ranked_assign(auth.uid(), v_week);
  else
    select e.cohort_id into v_cohort from ranked_entries e where e.week_start = v_week and e.profile_id = auth.uid();
  end if;
  if v_cohort is null then return; end if;
  return query
    with m as (
      select e.profile_id as pid, coalesce(e.points, ranked_points(e.profile_id, v_week, v_week + 7)) as pts
        from ranked_entries e where e.cohort_id = v_cohort
    )
    select (rank() over (order by m.pts desc))::int, m.pid, p.username, p.avatar_url, rp_.rp, m.pts, m.pid = auth.uid()
      from m
      join profiles p on p.id = m.pid
      join ranked_players rp_ on rp_.profile_id = m.pid
     order by 1, p.username;
end $$;
revoke all on function public.ranked_board(date) from public, anon;
grant execute on function public.ranked_board(date) to authenticated;

-- Resultado da semana passada de quem chama (para mostrar "você subiu"/"você caiu"); vazio se ainda não fechou.
create or replace function public.ranked_last_result()
returns table (week_start date, points integer, pos integer, cohort_size integer, delta integer, rp_after integer)
language sql stable security definer set search_path = public as $$
  select e.week_start, e.points, e.position,
         (select count(*) from ranked_entries x where x.cohort_id = e.cohort_id)::int,
         e.delta, e.rp_after
    from ranked_entries e
   where e.profile_id = auth.uid() and e.delta is not null
   order by e.week_start desc
   limit 1
$$;
revoke all on function public.ranked_last_result() from public, anon;
grant execute on function public.ranked_last_result() to authenticated;

-- ───────────────────────── fechamento da semana ─────────────────────────
-- Fecha a semana p_week (uma segunda-feira já passada): calcula os pontos de todos, as posições em cada turma e aplica o
-- RP. Qualquer pessoa logada pode chamar (o app chama ao abrir a aba); só a primeira chamada faz algo. Quem já estava
-- na ranqueada e não abriu a aba também entra (turmas montadas na hora): faltar não protege de cair.
create or replace function public.ranked_settle_week(p_week date) returns integer
language plpgsql security definer set search_path = public as $$
declare
  v_n integer;
  v_idx integer;
  v_last boolean;
begin
  if auth.uid() is null then raise exception 'Entre na sua conta' using errcode = '28000'; end if;
  if date_trunc('week', p_week::timestamp)::date <> p_week then raise exception 'A semana começa numa segunda-feira'; end if;
  if p_week >= ranked_current_week() then raise exception 'Essa semana ainda não acabou'; end if;
  perform pg_advisory_xact_lock(hashtext('ranked_settle:' || p_week::text));
  if exists (select 1 from ranked_weeks where week_start = p_week) then return 0; end if;

  -- todo mundo que já era da ranqueada no fim da semana entra, mesmo sem ter aberto a aba
  perform ranked_assign_all(p_week);

  update ranked_entries e set points = ranked_points(e.profile_id, p_week, p_week + 7) where e.week_start = p_week;

  update ranked_entries e
     set position = x.pos, delta = ranked_delta(x.pos, x.n, x.pts)
    from (select ee.profile_id, ee.points as pts,
                 (rank() over (partition by ee.cohort_id order by ee.points desc))::int as pos,
                 (count(*) over (partition by ee.cohort_id))::int as n
            from ranked_entries ee where ee.week_start = p_week) x
   where e.week_start = p_week and e.profile_id = x.profile_id;

  v_idx := greatest(0, (p_week - date '2026-10-26') / 7);
  v_last := (v_idx % 8) = 7;

  update ranked_players p
     set rp = case when v_last then greatest(0, p.rp + e.delta) * 6 / 10 else greatest(0, p.rp + e.delta) end,
         peak_rp = greatest(p.peak_rp, greatest(0, p.rp + e.delta)),
         weeks_played = p.weeks_played + 1,
         updated_at = now()
    from ranked_entries e
   where e.week_start = p_week and e.profile_id = p.profile_id;

  update ranked_entries e set rp_after = p.rp from ranked_players p where e.week_start = p_week and e.profile_id = p.profile_id;

  insert into ranked_weeks(week_start) values (p_week);
  select count(*) into v_n from ranked_entries where week_start = p_week;
  return v_n;
end $$;
revoke all on function public.ranked_settle_week(date) from public, anon;
grant execute on function public.ranked_settle_week(date) to authenticated;

-- ───────────────────────── x1 ─────────────────────────
-- Desafia alguém para um x1 de 7 dias. Ranqueado: os dois precisam estar na ranqueada e no mesmo elo; amistoso: qualquer um.
create or replace function public.ranked_duel_create(p_opponent uuid, p_ranked boolean default true) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := auth.uid();
  v_id uuid;
  v_a int; v_b int;
begin
  if v_me is null then raise exception 'Entre na sua conta' using errcode = '28000'; end if;
  if p_opponent = v_me then raise exception 'Não dá para desafiar a si mesmo'; end if;
  if not exists (select 1 from profiles where id = p_opponent) then raise exception 'Essa pessoa não existe'; end if;
  if p_ranked then
    select rp into v_a from ranked_players where profile_id = v_me;
    select rp into v_b from ranked_players where profile_id = p_opponent;
    if v_a is null then raise exception 'Entre na ranqueada primeiro'; end if;
    if v_b is null then raise exception 'Essa pessoa ainda não está na ranqueada'; end if;
    if ranked_tier_of(v_a) <> ranked_tier_of(v_b) then raise exception 'x1 valendo RP é só entre pessoas do mesmo elo'; end if;
  end if;
  if exists (
    select 1 from ranked_duels d
     where d.status in ('pending', 'active')
       and ((d.challenger_id = v_me and d.opponent_id = p_opponent) or (d.challenger_id = p_opponent and d.opponent_id = v_me))
  ) then raise exception 'Já existe um x1 em aberto entre vocês'; end if;
  insert into ranked_duels(challenger_id, opponent_id, ranked) values (v_me, p_opponent, p_ranked) returning id into v_id;
  return v_id;
end $$;
revoke all on function public.ranked_duel_create(uuid, boolean) from public, anon;
grant execute on function public.ranked_duel_create(uuid, boolean) to authenticated;

-- Quem foi desafiado aceita (o x1 começa agora e dura 7 dias) ou recusa; quem desafiou pode cancelar um pedido pendente.
create or replace function public.ranked_duel_respond(p_duel uuid, p_accept boolean) returns void
language plpgsql security definer set search_path = public as $$
declare d ranked_duels;
begin
  if auth.uid() is null then raise exception 'Entre na sua conta' using errcode = '28000'; end if;
  select * into d from ranked_duels where id = p_duel for update;
  if d.id is null or d.status <> 'pending' then raise exception 'Esse pedido de x1 não está mais aberto'; end if;
  if d.opponent_id = auth.uid() then
    if p_accept then
      update ranked_duels set status = 'active', starts_at = now(), ends_at = now() + interval '7 days' where id = p_duel;
    else
      update ranked_duels set status = 'declined' where id = p_duel;
    end if;
  elsif d.challenger_id = auth.uid() and not p_accept then
    update ranked_duels set status = 'cancelled' where id = p_duel;
  else
    raise exception 'Você não pode responder a esse x1';
  end if;
end $$;
revoke all on function public.ranked_duel_respond(uuid, boolean) from public, anon;
grant execute on function public.ranked_duel_respond(uuid, boolean) to authenticated;

-- Fecha os x1 que acabaram (pontos finais, vencedor e RP). Qualquer pessoa logada pode chamar; é idempotente.
create or replace function public.ranked_settle_duels() returns integer
language plpgsql security definer set search_path = public as $$
declare
  d record;
  v_n int := 0;
  v_from date; v_to date;
  v_pa int; v_pb int; v_ra int; v_rb int; v_da int; v_db int;
begin
  if auth.uid() is null then raise exception 'Entre na sua conta' using errcode = '28000'; end if;
  for d in select * from ranked_duels where status = 'active' and ends_at <= now() order by ends_at for update skip locked loop
    v_from := (d.starts_at at time zone 'America/Sao_Paulo')::date;
    v_to   := (d.ends_at   at time zone 'America/Sao_Paulo')::date;
    v_pa := ranked_points(d.challenger_id, v_from, v_to);
    v_pb := ranked_points(d.opponent_id,   v_from, v_to);
    select rp into v_ra from ranked_players where profile_id = d.challenger_id;
    select rp into v_rb from ranked_players where profile_id = d.opponent_id;
    v_da := ranked_duel_delta(coalesce(v_ra, 0), coalesce(v_rb, 0), v_pa, v_pb, d.ranked);
    v_db := ranked_duel_delta(coalesce(v_rb, 0), coalesce(v_ra, 0), v_pb, v_pa, d.ranked);
    if d.ranked then
      update ranked_players set rp = greatest(0, rp + v_da), peak_rp = greatest(peak_rp, greatest(0, rp + v_da)), updated_at = now() where profile_id = d.challenger_id;
      update ranked_players set rp = greatest(0, rp + v_db), peak_rp = greatest(peak_rp, greatest(0, rp + v_db)), updated_at = now() where profile_id = d.opponent_id;
    end if;
    update ranked_duels
       set status = 'finished', pt_challenger = v_pa, pt_opponent = v_pb,
           winner_id = case when v_pa > v_pb then d.challenger_id when v_pb > v_pa then d.opponent_id else null end,
           delta_challenger = v_da, delta_opponent = v_db
     where id = d.id;
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;
revoke all on function public.ranked_settle_duels() from public, anon;
grant execute on function public.ranked_settle_duels() to authenticated;

-- Meus x1 (pendentes, ativos e os últimos terminados), com nomes e a pontuação de agora nos ativos.
create or replace function public.ranked_my_duels()
returns table (
  id uuid, ranked boolean, status text, starts_at timestamptz, ends_at timestamptz,
  challenger_id uuid, challenger_name text, challenger_avatar text, challenger_rp integer, pt_challenger integer,
  opponent_id uuid, opponent_name text, opponent_avatar text, opponent_rp integer, pt_opponent integer,
  winner_id uuid, delta_challenger integer, delta_opponent integer
)
language sql stable security definer set search_path = public as $$
  select d.id, d.ranked, d.status, d.starts_at, d.ends_at,
         d.challenger_id, pc.username, pc.avatar_url, coalesce(rc.rp, 0),
         case when d.status = 'active' then ranked_points(d.challenger_id, (d.starts_at at time zone 'America/Sao_Paulo')::date, (d.ends_at at time zone 'America/Sao_Paulo')::date)
              else d.pt_challenger end,
         d.opponent_id, po.username, po.avatar_url, coalesce(ro.rp, 0),
         case when d.status = 'active' then ranked_points(d.opponent_id, (d.starts_at at time zone 'America/Sao_Paulo')::date, (d.ends_at at time zone 'America/Sao_Paulo')::date)
              else d.pt_opponent end,
         d.winner_id, d.delta_challenger, d.delta_opponent
    from ranked_duels d
    join profiles pc on pc.id = d.challenger_id
    join profiles po on po.id = d.opponent_id
    left join ranked_players rc on rc.profile_id = d.challenger_id
    left join ranked_players ro on ro.profile_id = d.opponent_id
   where auth.uid() is not null
     and (d.challenger_id = auth.uid() or d.opponent_id = auth.uid())
     and (d.status in ('pending', 'active') or d.created_at > now() - interval '30 days')
   order by case d.status when 'active' then 0 when 'pending' then 1 else 2 end, d.created_at desc
   limit 30
$$;
revoke all on function public.ranked_my_duels() from public, anon;
grant execute on function public.ranked_my_duels() to authenticated;

