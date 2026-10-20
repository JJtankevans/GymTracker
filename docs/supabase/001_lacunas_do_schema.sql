-- Lacunas entre docs/supabaseSchema.md e o que o app (Kotlin) realmente usa.
-- Idempotente: pode rodar mais de uma vez. Só adiciona, não remove nada.

-- 1) Exercícios marcados como concluídos durante o treino (WorkoutRepositoryImpl.updateFinishedExercises)
ALTER TABLE workouts
  ADD COLUMN IF NOT EXISTS finished_exercise_ids UUID[] NOT NULL DEFAULT '{}';

-- 2) Views do supabaseSchema.md: sem security_invoker elas rodam como dono e IGNORAM a RLS,
--    ou seja, qualquer usuário logado enxergaria o histórico de todos. Corrige sem mudar colunas.
ALTER VIEW IF EXISTS vw_exercise_progression SET (security_invoker = true);
ALTER VIEW IF EXISTS vw_weekly_frequency     SET (security_invoker = true);
-- (se a view vw_exercise_progression ainda não existir, crie-a conforme app/docs/supabaseSchema.md e rode de novo)

-- 3) Índice útil para getExerciseBaseline (último treino/recorde por exercício)
CREATE INDEX IF NOT EXISTS idx_workout_sets_exercise_completed
  ON workout_sets (exercise_id, completed_at DESC);
