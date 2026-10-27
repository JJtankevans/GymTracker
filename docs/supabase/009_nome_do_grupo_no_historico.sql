-- 009: o treino do Histórico guarda o NOME do grupo, para ele não sumir quando o grupo é apagado.
-- Antes: workouts.muscle_group_id virava NULL ao apagar o grupo (ON DELETE SET NULL) e o Histórico mostrava "Sem grupo".
-- Agora: um gatilho copia o nome do grupo para os treinos dele ANTES de apagar (vale também para versões antigas do app),
-- e o app mostra o nome ao vivo se o grupo existe (acompanha renomeações) ou o nome guardado se ele foi apagado.
-- Treinos cujo grupo JÁ tinha sido apagado antes desta migração não têm como recuperar o nome (continuam "Sem grupo").
-- Idempotente: pode rodar mais de uma vez. Rodar depois de 001..008.

alter table public.workouts add column if not exists muscle_group_name text;

-- Treinos de hoje cujo grupo ainda existe: guarda o nome agora.
update public.workouts w
   set muscle_group_name = g.name
  from public.muscle_groups g
 where w.muscle_group_id = g.id
   and w.muscle_group_name is distinct from g.name;

create or replace function public.trg_keep_group_name() returns trigger
language plpgsql
set search_path = public
as $$
begin
  -- sempre o nome mais recente: se o grupo foi renomeado, vale o nome de agora
  update public.workouts set muscle_group_name = old.name where muscle_group_id = old.id;
  return old;
end $$;

drop trigger if exists trg_keep_group_name on public.muscle_groups;
create trigger trg_keep_group_name
  before delete on public.muscle_groups
  for each row execute function public.trg_keep_group_name();
