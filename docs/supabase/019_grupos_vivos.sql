-- 019: grupos "vivos": aviso fixado maior, ranking dos membros na semana, sequência da meta e mural.
--  - groups.pinned_message passa de 300 para 1000 caracteres (a 017 limitava a 300 e cortava textos longos);
--  - group_week_board(grupo): cada membro com treinos, dias ativos e minutos da semana (segunda a domingo, Brasília);
--  - group_goal_streak(grupo): quantas semanas seguidas o grupo bateu a meta semanal (a semana atual conta se já bateu);
--  - group_messages: mural do grupo (mensagens de até 280 caracteres; membros leem e escrevem, o autor e os
--    admins apagam).
-- Tirar alguém do grupo e mudar cargos já funciona pelas políticas da 002 (gm_delete e gm_update).
-- Sem esta migração o app continua funcionando; o ranking, a sequência e o mural só não aparecem.
-- Idempotente. Depende de 002 (groups, group_members, is_group_member, is_group_admin), 012 e 017.

-- aviso fixado até 1000 caracteres
do $$
declare c record;
begin
  for c in
    select conname from pg_constraint
     where conrelid = 'public.groups'::regclass and contype = 'c' and pg_get_constraintdef(oid) like '%pinned_message%'
  loop
    execute format('alter table public.groups drop constraint %I', c.conname);
  end loop;
end $$;
alter table public.groups add constraint groups_pinned_message_len check (pinned_message is null or char_length(pinned_message) <= 1000);

-- ranking dos membros na semana
create or replace function public.group_week_board(p_group uuid)
returns table (profile_id uuid, username text, avatar_url text, role text, workouts int, active_days int, minutes int, is_me boolean)
language sql stable security definer set search_path = public as $$
  with wk as (
    select date_trunc('week', now() at time zone 'America/Sao_Paulo')::date as start
  ),
  ws as (
    select w.profile_id, w.started_at, w.finished_at
      from workouts w, wk
     where w.profile_id in (select gm.profile_id from group_members gm where gm.group_id = p_group)
       and w.finished_at is not null
       and (w.started_at at time zone 'America/Sao_Paulo')::date >= wk.start
       and (w.started_at at time zone 'America/Sao_Paulo')::date <  wk.start + 7
  )
  select p.id, p.username, p.avatar_url, gm.role,
         count(ws.started_at)::int,
         count(distinct (ws.started_at at time zone 'America/Sao_Paulo')::date)::int,
         coalesce(round(sum(extract(epoch from (ws.finished_at - ws.started_at)) / 60)), 0)::int,
         p.id = auth.uid()
    from group_members gm
    join profiles p on p.id = gm.profile_id
    left join ws on ws.profile_id = gm.profile_id
   where gm.group_id = p_group
     and auth.uid() is not null
     and is_group_member(p_group)
   group by p.id, p.username, p.avatar_url, gm.role
   order by 5 desc, 6 desc, 7 desc, 2
$$;
revoke all on function public.group_week_board(uuid) from public, anon;
grant execute on function public.group_week_board(uuid) to authenticated;

-- semanas seguidas batendo a meta do grupo (0 sem meta); a semana atual só conta se já bateu
create or replace function public.group_goal_streak(p_group uuid) returns integer
language sql stable security definer set search_path = public as $$
  with g as (
    select weekly_goal as goal from groups where id = p_group
  ),
  wk as (
    select date_trunc('week', now() at time zone 'America/Sao_Paulo')::date as cur
  ),
  weeks as (
    select n, (wk.cur - 7 * n) as w from wk, generate_series(0, 51) n
  ),
  counts as (
    select weeks.n, count(w.id) as c
      from weeks
      left join workouts w
        on w.profile_id in (select gm.profile_id from group_members gm where gm.group_id = p_group)
       and w.finished_at is not null
       and (w.started_at at time zone 'America/Sao_Paulo')::date >= weeks.w
       and (w.started_at at time zone 'America/Sao_Paulo')::date <  weeks.w + 7
     group by weeks.n
  ),
  ok as (
    select n, c >= (select goal from g) as met from counts
  ),
  start as (
    select case when (select met from ok where n = 0) then 0 else 1 end as s
  )
  select case
    when auth.uid() is null or not is_group_member(p_group) or (select goal from g) is null then 0
    else (select count(*)::int from ok, start
           where ok.n >= start.s
             and ok.n < coalesce((select min(o2.n) from ok o2 where o2.n >= start.s and not o2.met), 52))
  end
$$;
revoke all on function public.group_goal_streak(uuid) from public, anon;
grant execute on function public.group_goal_streak(uuid) to authenticated;

-- mural do grupo
create table if not exists public.group_messages (
  id         uuid primary key default gen_random_uuid(),
  group_id   uuid not null references public.groups(id) on delete cascade,
  profile_id uuid not null default auth.uid() references public.profiles(id) on delete cascade,
  body       text not null check (char_length(btrim(body)) between 1 and 280),
  created_at timestamptz not null default now()
);
create index if not exists idx_group_messages_group_time on public.group_messages (group_id, created_at desc);

alter table public.group_messages enable row level security;

drop policy if exists group_messages_select on public.group_messages;
create policy group_messages_select on public.group_messages for select to authenticated
  using (is_group_member(group_id));
drop policy if exists group_messages_insert on public.group_messages;
create policy group_messages_insert on public.group_messages for insert to authenticated
  with check (profile_id = auth.uid() and is_group_member(group_id));
drop policy if exists group_messages_delete on public.group_messages;
create policy group_messages_delete on public.group_messages for delete to authenticated
  using (profile_id = auth.uid() or is_group_admin(group_id));
