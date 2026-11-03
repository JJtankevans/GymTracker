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
