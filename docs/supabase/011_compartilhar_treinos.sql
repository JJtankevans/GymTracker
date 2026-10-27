-- 011: compartilhar um grupo de treino (ex.: "Pernas" com os exercícios) por código.
-- Quem compartilha grava uma CÓPIA (snapshot) do grupo: nome, cor e exercícios (nome, séries, reps, observação).
-- Quem recebe digita o código e o app cria um grupo dele com esses exercícios (cada um com o próprio histórico).
-- O código não é uma senha forte: dá acesso só a essa cópia (nomes de exercícios e séries/reps), nada mais da conta.
-- Ninguém lista os códigos dos outros: a leitura por código é só pela função get_shared_routine().
-- Idempotente. Rodar depois de 001..010.

create table if not exists public.shared_routines (
  id          uuid        primary key default gen_random_uuid(),
  code        text        not null unique default upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8)),
  owner_id    uuid        not null default auth.uid() references public.profiles(id) on delete cascade,
  name        text        not null check (char_length(name) between 1 and 60),
  color_hex   text        not null default '#6200EE' check (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
  -- [{"name": "...", "sets": 3, "reps": 12, "notes": "..."}], no máximo 40 exercícios
  exercises   jsonb       not null check (jsonb_typeof(exercises) = 'array' and jsonb_array_length(exercises) between 1 and 40),
  imports     integer     not null default 0,
  created_at  timestamptz not null default now()
);

create index if not exists idx_shared_routines_owner on public.shared_routines (owner_id);

alter table public.shared_routines enable row level security;

drop policy if exists "dono lê seus compartilhamentos" on public.shared_routines;
create policy "dono lê seus compartilhamentos" on public.shared_routines
  for select using (owner_id = (select auth.uid()));

drop policy if exists "dono compartilha" on public.shared_routines;
create policy "dono compartilha" on public.shared_routines
  for insert with check (owner_id = (select auth.uid()));

drop policy if exists "dono apaga seus compartilhamentos" on public.shared_routines;
create policy "dono apaga seus compartilhamentos" on public.shared_routines
  for delete using (owner_id = (select auth.uid()));

grant select, insert, delete on public.shared_routines to authenticated;

-- Busca um treino compartilhado pelo código (sem diferenciar maiúsculas/espaços) e conta a importação.
-- SECURITY DEFINER porque quem importa não é o dono; devolve só o snapshot e o nome de quem compartilhou.
create or replace function public.get_shared_routine(p_code text, p_count_import boolean default false)
returns table (code text, name text, color_hex text, exercises jsonb, owner_username text, imports integer, created_at timestamptz)
language plpgsql
volatile
security definer
set search_path = public
as $$
#variable_conflict use_column
declare r shared_routines%rowtype;
begin
  if auth.uid() is null then raise exception 'não autenticado'; end if;
  select * into r from shared_routines s where s.code = upper(btrim(p_code));
  if not found then return; end if;
  if p_count_import and r.owner_id <> auth.uid() then
    update shared_routines s set imports = s.imports + 1 where s.id = r.id;
    r.imports := r.imports + 1;
  end if;
  return query select r.code, r.name, r.color_hex, r.exercises, p.username, r.imports, r.created_at
    from profiles p where p.id = r.owner_id;
end $$;

revoke all on function public.get_shared_routine(text, boolean) from public, anon;
grant execute on function public.get_shared_routine(text, boolean) to authenticated;
