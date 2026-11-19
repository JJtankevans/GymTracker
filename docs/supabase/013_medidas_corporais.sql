-- 013: medidas corporais (fita métrica) na nuvem. Card #344.
-- Hoje as medidas ficam só no aparelho (PrefsBodyStore) e SE PERDEM ao sair da conta. Esta tabela só prepara o banco:
-- o app ainda não lê nem grava aqui (falta o código em BodyCloudImpl + BodyRepository; ver docs/CONTEXTO_SESSAO.md).
-- Só adiciona (nada existente é alterado); seguro rodar mais de uma vez. Rodar depois de 003.
--
-- Valores de `site` = nomes do enum MeasureSite do app (WAIST, HIPS, CHEST, ARM, THIGH, CALF).

create table if not exists public.body_measurements (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  measured_on date not null,
  site text not null check (site in ('WAIST', 'HIPS', 'CHEST', 'ARM', 'THIGH', 'CALF')),
  cm numeric(5,1) not null check (cm between 10 and 300),
  created_at timestamptz not null default now(),
  unique (profile_id, measured_on, site)      -- um registro por dia e local (igual ao app)
);

create index if not exists idx_body_measurements_profile_date on public.body_measurements (profile_id, measured_on desc);

alter table public.body_measurements enable row level security;

-- Medida corporal é dado pessoal: só o dono lê e escreve (como body_weights). Nada de leitura por grupo/amigo.
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

-- No Supabase toda tabela nova nasce com TODAS as permissões para anon e authenticated (inclusive TRUNCATE).
-- Tira o que o app não usa: anônimo não toca; logado só lê/grava linhas (o RLS filtra por dono).
revoke all on public.body_measurements from anon;
revoke all on public.body_measurements from authenticated;
grant select, insert, update, delete on public.body_measurements to authenticated;
