# 🗄️ GymTracker — Schema SQL (Supabase)

> Cole cada bloco no **SQL Editor** do seu projeto Supabase na ordem apresentada.

---

## Extensions

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- para gen_random_uuid()
```

---

## Tabelas

### 1. `profiles`
Criado automaticamente via trigger após signup no Auth.

```sql
CREATE TABLE profiles (
  id           UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username     TEXT        NOT NULL,
  full_name    TEXT,
  avatar_url   TEXT,
  weekly_goal  INTEGER     NOT NULL DEFAULT 3 CHECK (weekly_goal BETWEEN 1 AND 7),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  profiles              IS 'Perfil público do usuário, espelha auth.users.';
COMMENT ON COLUMN profiles.weekly_goal IS 'Meta de treinos por semana (1–7).';
```

---

### 2. `muscle_groups`
Grupos musculares definidos pelo usuário (ex: Treino A, Peito).

```sql
CREATE TABLE muscle_groups (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id  UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name        TEXT        NOT NULL,
  color_hex   TEXT        NOT NULL DEFAULT '#6200EE'
                            CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_muscle_group_name_per_user UNIQUE (profile_id, name)
);

COMMENT ON TABLE  muscle_groups           IS 'Grupos musculares criados pelo usuário.';
COMMENT ON COLUMN muscle_groups.color_hex IS 'Cor em hexadecimal para identificação visual (ex: #FF5722).';
```

---

### 3. `exercises`
Catálogo de exercícios do usuário.

```sql
CREATE TABLE exercises (
  id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id      UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  muscle_group_id UUID        REFERENCES muscle_groups(id) ON DELETE SET NULL,
  name            TEXT        NOT NULL,
  default_sets    INTEGER     NOT NULL DEFAULT 3  CHECK (default_sets  > 0),
  default_reps    INTEGER     NOT NULL DEFAULT 12 CHECK (default_reps  > 0),
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_exercise_name_per_user UNIQUE (profile_id, name)
);

COMMENT ON TABLE  exercises                 IS 'Catálogo de exercícios do usuário.';
COMMENT ON COLUMN exercises.default_sets    IS 'Número padrão de séries para este exercício.';
COMMENT ON COLUMN exercises.default_reps    IS 'Número padrão de repetições por série.';
COMMENT ON COLUMN exercises.muscle_group_id IS 'Grupo muscular ao qual o exercício pertence (nullable).';
```

> **Atualização (migração 006, 21/10/2026):** `muscle_group_id` é **legado**. Um exercício pode estar em vários grupos pela tabela `group_exercises` (seção 6); o app não lê mais esta coluna. Veja `docs/supabase/006_exercicio_em_varios_grupos.sql`.

---

### 4. `workouts`
Cada sessão de treino (um dia de academia).

```sql
CREATE TABLE workouts (
  id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id      UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  muscle_group_id UUID        REFERENCES muscle_groups(id) ON DELETE SET NULL,
  started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at     TIMESTAMPTZ,
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_workout_finished_after_started
    CHECK (finished_at IS NULL OR finished_at > started_at)
);

COMMENT ON TABLE  workouts              IS 'Sessões de treino do usuário.';
COMMENT ON COLUMN workouts.finished_at IS 'Preenchido ao finalizar o treino; NULL = treino em andamento.';
```

---

### 5. `workout_sets`
Cada série registrada dentro de uma sessão.

```sql
CREATE TABLE workout_sets (
  id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
  workout_id   UUID           NOT NULL REFERENCES workouts(id)   ON DELETE CASCADE,
  exercise_id  UUID           NOT NULL REFERENCES exercises(id)  ON DELETE CASCADE,
  set_number   INTEGER        NOT NULL CHECK (set_number > 0),
  reps         INTEGER        NOT NULL CHECK (reps > 0),
  weight_kg    NUMERIC(6, 2)  NOT NULL DEFAULT 0 CHECK (weight_kg >= 0),
  rest_seconds INTEGER        CHECK (rest_seconds IS NULL OR rest_seconds >= 0),
  completed_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  workout_sets              IS 'Séries realizadas em cada sessão de treino.';
COMMENT ON COLUMN workout_sets.set_number   IS 'Número da série dentro do exercício (1, 2, 3...).';
COMMENT ON COLUMN workout_sets.weight_kg    IS 'Carga utilizada em quilogramas.';
COMMENT ON COLUMN workout_sets.rest_seconds IS 'Tempo de descanso configurado após esta série (segundos).';
```

---

### 6. `group_exercises` (migração 006)
Relação N:N: em quais grupos cada exercício está. Apagar o grupo ou o exercício remove o vínculo (cascade); o exercício e o histórico dele permanecem se só o grupo for apagado.

```sql
CREATE TABLE group_exercises (
  group_id    UUID        NOT NULL REFERENCES muscle_groups(id) ON DELETE CASCADE,
  exercise_id UUID        NOT NULL REFERENCES exercises(id)     ON DELETE CASCADE,
  profile_id  UUID        NOT NULL DEFAULT auth.uid() REFERENCES profiles(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (group_id, exercise_id)
);
-- RLS: o dono lê, cria (só com grupo e exercício dele) e apaga. Não há update (troca = apagar + criar).
```

---

## Índices

```sql
CREATE INDEX idx_muscle_groups_profile  ON muscle_groups (profile_id);
CREATE INDEX idx_exercises_profile      ON exercises     (profile_id);
CREATE INDEX idx_exercises_muscle_group ON exercises     (muscle_group_id);
CREATE INDEX idx_workouts_profile       ON workouts      (profile_id);
CREATE INDEX idx_workouts_started_at    ON workouts      (profile_id, started_at DESC);
CREATE INDEX idx_workout_sets_workout   ON workout_sets  (workout_id);
CREATE INDEX idx_workout_sets_exercise  ON workout_sets  (exercise_id);
```

---

## Triggers

### `updated_at` automático

```sql
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_exercises_updated_at
  BEFORE UPDATE ON exercises
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

### Criar `profile` automaticamente após signup

```sql
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  INSERT INTO profiles (id, username, full_name, avatar_url)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
    NEW.raw_user_meta_data->>'full_name',
    NEW.raw_user_meta_data->>'avatar_url'
  );
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();
```

---

## Row Level Security (RLS)

### Ativar RLS

```sql
ALTER TABLE profiles      ENABLE ROW LEVEL SECURITY;
ALTER TABLE muscle_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE exercises     ENABLE ROW LEVEL SECURITY;
ALTER TABLE workouts      ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_sets  ENABLE ROW LEVEL SECURITY;
```

### Políticas: `profiles`

```sql
CREATE POLICY "Usuário lê seu próprio perfil"
  ON profiles FOR SELECT
  USING (id = auth.uid());

CREATE POLICY "Usuário atualiza seu próprio perfil"
  ON profiles FOR UPDATE
  USING (id = auth.uid());
```

### Políticas: `muscle_groups`

```sql
CREATE POLICY "Usuário gerencia seus grupos musculares"
  ON muscle_groups FOR ALL
  USING (profile_id = auth.uid());
```

### Políticas: `exercises`

```sql
CREATE POLICY "Usuário gerencia seus exercícios"
  ON exercises FOR ALL
  USING (profile_id = auth.uid());
```

### Políticas: `workouts`

```sql
CREATE POLICY "Usuário gerencia seus treinos"
  ON workouts FOR ALL
  USING (profile_id = auth.uid());
```

### Políticas: `workout_sets`
Valida o dono via `JOIN` com `workouts`, pois a tabela não possui `profile_id` diretamente.

```sql
CREATE POLICY "Usuário gerencia suas séries"
  ON workout_sets FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM workouts w
      WHERE w.id = workout_sets.workout_id
        AND w.profile_id = auth.uid()
    )
  );
```

---

## Views

### `vw_exercise_progression`
Base para o gráfico de progressão de carga — retorna a carga máxima por exercício por dia.

```sql
CREATE OR REPLACE VIEW vw_exercise_progression AS
SELECT
  ws.exercise_id,
  e.name                         AS exercise_name,
  w.profile_id,
  w.started_at::DATE             AS session_date,
  MAX(ws.weight_kg)              AS max_weight_kg,
  SUM(ws.reps)                   AS total_reps,
  COUNT(*)                       AS total_sets
FROM workout_sets ws
JOIN workouts  w ON w.id = ws.workout_id
JOIN exercises e ON e.id = ws.exercise_id
GROUP BY ws.exercise_id, e.name, w.profile_id, w.started_at::DATE
ORDER BY w.profile_id, ws.exercise_id, session_date;
```

### `vw_weekly_frequency`
Usada no dashboard — retorna o total de treinos por semana por usuário.

```sql
CREATE OR REPLACE VIEW vw_weekly_frequency AS
SELECT
  profile_id,
  DATE_TRUNC('week', started_at)::DATE AS week_start,
  COUNT(*)                              AS total_workouts
FROM workouts
GROUP BY profile_id, week_start
ORDER BY profile_id, week_start;
```

---

*GymTracker — Supabase Schema v1.0*

---

## Atualizações posteriores ao schema original
- `workouts.finished_exercise_ids` e `security_invoker` nas views: `docs/supabase/001_lacunas_do_schema.sql`.
- Tabelas sociais (perfil público, seguir, grupos, desafios, feed, reações, músicas): `docs/supabase/002_social.sql`, `004`, `005` e `docs/SOCIAL_DESIGN.md`.
- Peso corporal e dados do perfil: `docs/supabase/003_dados_corporais.sql`.
- Nome do grupo no histórico: `workouts.muscle_group_name` + gatilho `trg_keep_group_name` (BEFORE DELETE em `muscle_groups`) em `docs/supabase/009_nome_do_grupo_no_historico.sql`.
- Títulos de desafio: `challenges.fail_threshold` e `challenges.streak_goal` (opcionais) e função `profile_titles(uuid[])` em `docs/supabase/010_titulos_do_desafio.sql`.
- Recordes pessoais em uma chamada: função `records_summary()` (SECURITY INVOKER) em `docs/supabase/008_records_summary.sql`; substitui consultar `vw_exercise_progression` exercício por exercício na tela de Recordes.
- Foto de perfil: bucket público `user_profile_images` no Supabase Storage (arquivos em `<id do usuário>/<milissegundos>.jpg`; a URL pública fica em `profiles.avatar_url`); limites e RLS em `docs/supabase/007_foto_de_perfil_storage.sql`.
- Exercício em vários grupos (`group_exercises`): `docs/supabase/006_exercicio_em_varios_grupos.sql`; índices `idx_group_exercises_exercise` e `idx_group_exercises_profile`.
