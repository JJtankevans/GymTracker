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
