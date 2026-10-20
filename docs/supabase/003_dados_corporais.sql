-- 003: dados corporais (altura, nascimento, sexo) e histórico de peso.
-- Hoje o app guarda isso só no aparelho; este SQL prepara a sincronização com o Supabase.
-- Rodar depois de 001 e 002.

alter table public.profiles
  add column if not exists height_cm int check (height_cm between 100 and 250),
  add column if not exists birth_year int check (birth_year between 1900 and 2100),
  add column if not exists sex text check (sex in ('MALE', 'FEMALE')),
  add column if not exists ask_weight_each_workout boolean not null default false;

create table if not exists public.body_weights (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  measured_on date not null,
  weight_kg numeric(5,1) not null check (weight_kg between 20 and 400),
  created_at timestamptz not null default now(),
  unique (profile_id, measured_on)          -- um registro por dia
);

create index if not exists idx_body_weights_profile_date on public.body_weights (profile_id, measured_on desc);

alter table public.body_weights enable row level security;

-- Peso é dado pessoal: só o dono lê e escreve. Nada de leitura por grupo/amigo.
create policy "dono lê seus pesos" on public.body_weights
  for select using (profile_id = auth.uid());
create policy "dono registra seus pesos" on public.body_weights
  for insert with check (profile_id = auth.uid());
create policy "dono altera seus pesos" on public.body_weights
  for update using (profile_id = auth.uid()) with check (profile_id = auth.uid());
create policy "dono apaga seus pesos" on public.body_weights
  for delete using (profile_id = auth.uid());
