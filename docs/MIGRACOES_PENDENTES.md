# Migrações pendentes (para você aplicar no Supabase do GymTracker)

Projeto `ygbrrypyydxnuskrobeu` (outra conta; nenhuma sessão minha enxerga ele). Aplique na ordem, no SQL Editor. Todas são **aditivas e idempotentes**.
O app **não depende** de nenhuma delas para funcionar: o que usa coluna nova guarda local até a migração existir.

## Já escritas antes
- `docs/supabase/013_medidas_corporais.sql` — medidas corporais na nuvem (#344). O lado Kotlin ainda não foi feito (#443).

## Novas (21/11/2026)
### 014 — Esforço por série (RPE) em `workout_sets` (#350)
```sql
alter table public.workout_sets add column if not exists rpe smallint;
alter table public.workout_sets drop constraint if exists workout_sets_rpe_range;
alter table public.workout_sets add constraint workout_sets_rpe_range check (rpe is null or (rpe between 1 and 10));
comment on column public.workout_sets.rpe is 'Esforço percebido 1-10 (10 = falha); opcional.';
```
Depois de aplicar: o app pode passar a enviar `rpe` (hoje fica só no aparelho).

*(Outras migrações da noite entram abaixo, com data.)*

### 015 (opcional) — Restaurar histórico de um backup (#364)
O app já restaura o **catálogo** (grupos e exercícios). Para restaurar também **treinos e séries com as datas antigas** é preciso uma função no servidor, porque o cliente só consegue criar treino "agora". Esboço (SECURITY INVOKER, respeita RLS):
```sql
create or replace function public.restore_workout(p_started timestamptz, p_finished timestamptz, p_group uuid, p_sets jsonb)
returns uuid language plpgsql security invoker as $$
declare w uuid;
begin
  insert into public.workouts(profile_id, muscle_group_id, started_at, finished_at)
  values (auth.uid(), p_group, p_started, p_finished) returning id into w;
  insert into public.workout_sets(workout_id, exercise_id, set_number, reps, weight_kg, completed_at)
  select w, (s->>'exercise_id')::uuid, (s->>'set_number')::int, (s->>'reps')::int, (s->>'weight_kg')::numeric, (s->>'completed_at')::timestamptz
  from jsonb_array_elements(p_sets) s;
  return w;
end $$;
revoke all on function public.restore_workout(timestamptz, timestamptz, uuid, jsonb) from public, anon;
grant execute on function public.restore_workout(timestamptz, timestamptz, uuid, jsonb) to authenticated;
```
Antes de aplicar, conferir os nomes das colunas reais em `workouts`/`workout_sets` (o esboço segue o modelo do app). O app ainda não chama essa função.

### 016 — Foto do treino no post do feed (ideia 22/11)
Arquivo pronto: `docs/supabase/016_foto_no_post.sql`. Passos: (1) criar no painel o bucket **privado** `post_photos`; (2) rodar o SQL; (3) depois o app pode enviar a foto (JPEG 1080x1350, ≤ 1 MB) para `<uid>/<post>.jpg`, gravar `feed_posts.photo_path` e mostrar a foto no feed por URL assinada. Leitura segue a visibilidade do post (`can_view_post`). **Aplicado: não.** O app só compartilha a foto por fora (WhatsApp/Instagram) por enquanto.
