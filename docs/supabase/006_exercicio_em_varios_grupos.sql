-- 006: um exercício pode estar em vários grupos (relação N:N).
-- Antes: exercises.muscle_group_id (1 grupo só) + nome único por usuário. Ao apagar o grupo o exercício
-- ficava órfão (ON DELETE SET NULL), mas o nome continuava ocupado: não dava para pôr "Cadeira extensora"
-- num grupo novo. Agora o vínculo fica em group_exercises; o exercício (e o histórico dele) é um só.
-- Idempotente: pode rodar mais de uma vez. Não apaga nada. Rodar depois de 001..005.

create table if not exists public.group_exercises (
  group_id    uuid        not null references public.muscle_groups(id) on delete cascade,
  exercise_id uuid        not null references public.exercises(id)     on delete cascade,
  profile_id  uuid        not null default auth.uid() references public.profiles(id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (group_id, exercise_id)
);

create index if not exists idx_group_exercises_exercise on public.group_exercises (exercise_id);
create index if not exists idx_group_exercises_profile  on public.group_exercises (profile_id);

alter table public.group_exercises enable row level security;

-- Só o dono lê e mexe; e só liga grupo e exercício que são dele.
drop policy if exists "dono lê seus vínculos" on public.group_exercises;
create policy "dono lê seus vínculos" on public.group_exercises
  for select using (profile_id = (select auth.uid()));

drop policy if exists "dono cria seus vínculos" on public.group_exercises;
create policy "dono cria seus vínculos" on public.group_exercises
  for insert with check (
    profile_id = (select auth.uid())
    and exists (select 1 from public.muscle_groups g where g.id = group_id    and g.profile_id = (select auth.uid()))
    and exists (select 1 from public.exercises     e where e.id = exercise_id and e.profile_id = (select auth.uid()))
  );

drop policy if exists "dono apaga seus vínculos" on public.group_exercises;
create policy "dono apaga seus vínculos" on public.group_exercises
  for delete using (profile_id = (select auth.uid()));

grant select, insert, delete on public.group_exercises to authenticated;

-- Leva o que já existe: cada exercício com grupo vira um vínculo.
-- Exercícios órfãos (grupo apagado antes desta migração) continuam na lista "Todos" e podem ser
-- ligados a um grupo novo pelo app.
insert into public.group_exercises (group_id, exercise_id, profile_id)
select e.muscle_group_id, e.id, e.profile_id
from public.exercises e
where e.muscle_group_id is not null
on conflict do nothing;

-- exercises.muscle_group_id fica no banco só por compatibilidade (o app novo não lê nem decide nada por ele).
comment on column public.exercises.muscle_group_id is
  'LEGADO: substituído por group_exercises (006). Pode ser removido depois que todos estiverem no app novo.';
