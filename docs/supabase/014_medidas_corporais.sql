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
