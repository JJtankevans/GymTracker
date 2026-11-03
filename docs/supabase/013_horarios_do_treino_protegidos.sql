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
