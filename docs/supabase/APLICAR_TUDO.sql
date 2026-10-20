-- GymTracker: 001 + 002 + 003 em uma transação (tudo ou nada). Para banco NOVO. Se já rodou a versão anterior, rode só o 004.
begin;
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
-- ============================================================================
-- GymTracker — camada social (grupos, desafios, feed, reações, playlists)
-- Pré-requisitos: schema base (app/docs/supabaseSchema.md) + 001_lacunas_do_schema.sql.
-- Idempotente na medida do possível. Tudo é OPT-IN: o perfil nasce privado.
-- ============================================================================

-- ───────────────────────── 1) Perfil robusto ─────────────────────────
ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS bio            TEXT,
  ADD COLUMN IF NOT EXISTS visibility     TEXT    NOT NULL DEFAULT 'private',
  ADD COLUMN IF NOT EXISTS share_workouts BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS share_music    BOOLEAN NOT NULL DEFAULT FALSE;

DO $$ BEGIN
  ALTER TABLE profiles ADD CONSTRAINT chk_profiles_visibility CHECK (visibility IN ('public','followers','private'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN
  ALTER TABLE profiles ADD CONSTRAINT chk_profiles_bio CHECK (bio IS NULL OR char_length(bio) <= 280);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Para buscar pessoas por @usuario, o ideal é nome único (case-insensitive).
-- Descomente SE não houver nomes repetidos hoje (senão o comando falha):
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_username_lower ON profiles (lower(username));

-- ───────────────────────── 2) Seguir ─────────────────────────
CREATE TABLE IF NOT EXISTS follows (
  follower_id UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  followed_id UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  status      TEXT        NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (follower_id, followed_id),
  CHECK (follower_id <> followed_id)
);
CREATE INDEX IF NOT EXISTS idx_follows_followed ON follows (followed_id, status);

-- ───────────────────────── 3) Grupos ─────────────────────────
CREATE TABLE IF NOT EXISTS groups (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id    UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name        TEXT        NOT NULL CHECK (char_length(name) BETWEEN 2 AND 60),
  description TEXT        CHECK (description IS NULL OR char_length(description) <= 500),
  privacy     TEXT        NOT NULL DEFAULT 'invite' CHECK (privacy IN ('public','invite')),
  invite_code TEXT        NOT NULL UNIQUE DEFAULT substr(replace(gen_random_uuid()::text, '-', ''), 1, 8),
  max_members INTEGER     CHECK (max_members IS NULL OR max_members BETWEEN 2 AND 500),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS group_members (
  group_id   UUID        NOT NULL REFERENCES groups(id)   ON DELETE CASCADE,
  profile_id UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  role       TEXT        NOT NULL DEFAULT 'member' CHECK (role IN ('owner','admin','member')),
  joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (group_id, profile_id)
);
CREATE INDEX IF NOT EXISTS idx_group_members_profile ON group_members (profile_id);

-- ───────────────────────── 4) Desafios (configuráveis) ─────────────────────────
CREATE TABLE IF NOT EXISTS challenges (
  id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  creator_id             UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  group_id               UUID        REFERENCES groups(id) ON DELETE CASCADE,   -- NULL = desafio aberto
  title                  TEXT        NOT NULL CHECK (char_length(title) BETWEEN 3 AND 80),
  description            TEXT        CHECK (description IS NULL OR char_length(description) <= 500),
  -- o que conta pontos
  metric                 TEXT        NOT NULL CHECK (metric IN
                           ('workouts','active_days','volume_kg','sets','reps','training_minutes',
                            'exercise_max_weight','exercise_volume')),
  exercise_name          TEXT,       -- obrigatório nas métricas de exercício (compara por nome, sem diferenciar maiúsculas)
  mode                   TEXT        NOT NULL DEFAULT 'individual' CHECK (mode IN ('individual','team')),
  teams                  TEXT[]      NOT NULL DEFAULT '{}',
  visibility             TEXT        NOT NULL DEFAULT 'group' CHECK (visibility IN ('public','group','invite')),
  invite_code            TEXT        UNIQUE DEFAULT substr(replace(gen_random_uuid()::text, '-', ''), 1, 8),
  starts_at              TIMESTAMPTZ NOT NULL,
  ends_at                TIMESTAMPTZ NOT NULL,
  tz                     TEXT        NOT NULL DEFAULT 'America/Sao_Paulo',
  target                 NUMERIC     CHECK (target IS NULL OR target > 0),   -- meta (opcional): quem bate primeiro / quem bate
  -- regras anti-trapaça
  min_minutes_per_workout INTEGER    NOT NULL DEFAULT 0  CHECK (min_minutes_per_workout >= 0),
  max_workouts_per_day    INTEGER    NOT NULL DEFAULT 1  CHECK (max_workouts_per_day BETWEEN 1 AND 10),
  max_participants       INTEGER     CHECK (max_participants IS NULL OR max_participants >= 2),
  created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (ends_at > starts_at),
  CHECK ((metric LIKE 'exercise\_%') = (exercise_name IS NOT NULL)),
  CHECK (visibility <> 'group' OR group_id IS NOT NULL),
  CHECK (mode = 'individual' OR cardinality(teams) >= 2)
);
CREATE INDEX IF NOT EXISTS idx_challenges_group ON challenges (group_id);

CREATE TABLE IF NOT EXISTS challenge_participants (
  challenge_id UUID        NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
  profile_id   UUID        NOT NULL REFERENCES profiles(id)   ON DELETE CASCADE,
  team         TEXT,
  joined_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (challenge_id, profile_id)
);
CREATE INDEX IF NOT EXISTS idx_challenge_participants_profile ON challenge_participants (profile_id);

-- ───────────────────────── 5) Feed de treinos e reações ─────────────────────────
CREATE TABLE IF NOT EXISTS feed_posts (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  workout_id UUID        NOT NULL UNIQUE REFERENCES workouts(id) ON DELETE CASCADE,
  caption    TEXT        CHECK (caption IS NULL OR char_length(caption) <= 280),
  visibility TEXT        NOT NULL DEFAULT 'followers' CHECK (visibility IN ('public','followers','private')),
  stats      JSONB       NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_feed_posts_created ON feed_posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_posts_profile ON feed_posts (profile_id, created_at DESC);

CREATE TABLE IF NOT EXISTS post_reactions (
  post_id    UUID        NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
  profile_id UUID        NOT NULL REFERENCES profiles(id)   ON DELETE CASCADE,
  kind       TEXT        NOT NULL CHECK (kind IN ('flex','fire','clap','headphones')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (post_id, profile_id, kind)
);

-- ───────────────────────── 6) Músicas do treino ─────────────────────────
CREATE TABLE IF NOT EXISTS workout_tracks (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  workout_id UUID        NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
  profile_id UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  title      TEXT        NOT NULL CHECK (char_length(title) <= 200),
  artist     TEXT        CHECK (artist IS NULL OR char_length(artist) <= 200),
  album      TEXT        CHECK (album  IS NULL OR char_length(album)  <= 200),
  source_app TEXT,
  played_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_workout_tracks_workout ON workout_tracks (workout_id, played_at);
CREATE INDEX IF NOT EXISTS idx_workout_tracks_profile ON workout_tracks (profile_id);

-- ============================================================================
-- Funções auxiliares (SECURITY DEFINER: evitam recursão de RLS e centralizam as regras)
-- ============================================================================

CREATE OR REPLACE FUNCTION is_following(target UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM follows f
                 WHERE f.follower_id = auth.uid() AND f.followed_id = target AND f.status = 'accepted');
$$;

CREATE OR REPLACE FUNCTION is_group_member(g UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM group_members m WHERE m.group_id = g AND m.profile_id = auth.uid());
$$;

CREATE OR REPLACE FUNCTION is_group_admin(g UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM group_members m
                 WHERE m.group_id = g AND m.profile_id = auth.uid() AND m.role IN ('owner','admin'));
$$;

CREATE OR REPLACE FUNCTION shares_group_with(other UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM group_members a JOIN group_members b ON a.group_id = b.group_id
                 WHERE a.profile_id = auth.uid() AND b.profile_id = other);
$$;

CREATE OR REPLACE FUNCTION can_view_profile(target UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT target = auth.uid() OR EXISTS (
    SELECT 1 FROM profiles p WHERE p.id = target AND (
      p.visibility = 'public' OR (p.visibility = 'followers' AND is_following(p.id))));
$$;

-- Post visível: dono; 'public' para qualquer logado; 'followers' para seguidores aceitos e colegas de grupo.
CREATE OR REPLACE FUNCTION can_view_post(owner UUID, vis TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT owner = auth.uid()
      OR (vis = 'public'    AND auth.uid() IS NOT NULL)
      OR (vis = 'followers' AND (is_following(owner) OR shares_group_with(owner)));
$$;

CREATE OR REPLACE FUNCTION can_view_challenge(cid UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM challenges c WHERE c.id = cid AND (
         c.creator_id = auth.uid()
      OR c.visibility = 'public'
      OR (c.visibility = 'group' AND is_group_member(c.group_id))
      OR EXISTS (SELECT 1 FROM challenge_participants p WHERE p.challenge_id = c.id AND p.profile_id = auth.uid())));
$$;

-- Músicas de um treino: dono, ou quem pode ver o post E o dono liberou música.
CREATE OR REPLACE FUNCTION can_view_tracks(owner UUID, wid UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT owner = auth.uid() OR EXISTS (
    SELECT 1 FROM profiles p JOIN feed_posts fp ON fp.workout_id = wid AND fp.profile_id = p.id
    WHERE p.id = owner AND p.share_music AND can_view_post(fp.profile_id, fp.visibility));
$$;

-- ============================================================================
-- Triggers
-- ============================================================================

-- Dono entra no grupo como 'owner'
CREATE OR REPLACE FUNCTION trg_group_add_owner() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO group_members (group_id, profile_id, role) VALUES (NEW.id, NEW.owner_id, 'owner')
  ON CONFLICT DO NOTHING;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_group_add_owner ON groups;
CREATE TRIGGER trg_group_add_owner AFTER INSERT ON groups FOR EACH ROW EXECUTE FUNCTION trg_group_add_owner();

-- Seguir: aceita direto se o perfil é público, senão fica pendente.
CREATE OR REPLACE FUNCTION trg_follow_status() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    NEW.status := CASE WHEN (SELECT visibility FROM profiles WHERE id = NEW.followed_id) = 'public'
                       THEN 'accepted' ELSE 'pending' END;
  ELSE
    -- na atualização só dá pra aceitar; ids não mudam
    IF NEW.follower_id <> OLD.follower_id OR NEW.followed_id <> OLD.followed_id THEN
      RAISE EXCEPTION 'ids não podem mudar';
    END IF;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_follow_status ON follows;
CREATE TRIGGER trg_follow_status BEFORE INSERT OR UPDATE ON follows FOR EACH ROW EXECUTE FUNCTION trg_follow_status();

-- Treino finalizado vira post, se (e só se) o usuário liberou.
CREATE OR REPLACE FUNCTION trg_workout_to_post() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  prof profiles%ROWTYPE;
  st   JSONB;
BEGIN
  IF OLD.finished_at IS NOT NULL OR NEW.finished_at IS NULL THEN RETURN NEW; END IF;
  SELECT * INTO prof FROM profiles WHERE id = NEW.profile_id;
  IF NOT FOUND OR NOT prof.share_workouts THEN RETURN NEW; END IF;

  SELECT jsonb_build_object(
           'minutes',   GREATEST(1, round(extract(epoch FROM (NEW.finished_at - NEW.started_at)) / 60)),
           'sets',      COUNT(s.id),
           'reps',      COALESCE(SUM(s.reps), 0),
           'volume_kg', COALESCE(SUM(s.reps * s.weight_kg), 0),
           'exercises', COUNT(DISTINCT s.exercise_id),
           'group',     (SELECT name FROM muscle_groups WHERE id = NEW.muscle_group_id))
    INTO st FROM workout_sets s WHERE s.workout_id = NEW.id;

  INSERT INTO feed_posts (profile_id, workout_id, visibility, stats)
  VALUES (NEW.profile_id, NEW.id,
          CASE prof.visibility WHEN 'public' THEN 'public' ELSE 'followers' END, st)
  ON CONFLICT (workout_id) DO NOTHING;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_workout_to_post ON workouts;
CREATE TRIGGER trg_workout_to_post AFTER UPDATE OF finished_at ON workouts FOR EACH ROW EXECUTE FUNCTION trg_workout_to_post();

-- ============================================================================
-- RPCs
-- ============================================================================

-- Entrar em grupo público
CREATE OR REPLACE FUNCTION join_group(p_group UUID) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE g groups%ROWTYPE;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'não autenticado'; END IF;
  SELECT * INTO g FROM groups WHERE id = p_group;
  IF NOT FOUND OR g.privacy <> 'public' THEN RAISE EXCEPTION 'grupo indisponível'; END IF;
  IF g.max_members IS NOT NULL AND (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) >= g.max_members THEN
    RAISE EXCEPTION 'grupo cheio';
  END IF;
  INSERT INTO group_members (group_id, profile_id) VALUES (g.id, auth.uid()) ON CONFLICT DO NOTHING;
END $$;

-- Entrar por código de convite (grupos privados)
CREATE OR REPLACE FUNCTION join_group_by_code(p_code TEXT) RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE g groups%ROWTYPE;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'não autenticado'; END IF;
  SELECT * INTO g FROM groups WHERE invite_code = lower(trim(p_code));
  IF NOT FOUND THEN RAISE EXCEPTION 'código inválido'; END IF;
  IF g.max_members IS NOT NULL AND (SELECT COUNT(*) FROM group_members WHERE group_id = g.id) >= g.max_members THEN
    RAISE EXCEPTION 'grupo cheio';
  END IF;
  INSERT INTO group_members (group_id, profile_id) VALUES (g.id, auth.uid()) ON CONFLICT DO NOTHING;
  RETURN g.id;
END $$;

-- Entrar em desafio (por id, se visível; ou por código) escolhendo equipe quando for em times
CREATE OR REPLACE FUNCTION join_challenge(p_challenge UUID, p_team TEXT DEFAULT NULL, p_code TEXT DEFAULT NULL) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE c challenges%ROWTYPE;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'não autenticado'; END IF;
  SELECT * INTO c FROM challenges WHERE id = p_challenge;
  IF NOT FOUND THEN RAISE EXCEPTION 'desafio não encontrado'; END IF;
  IF NOT (can_view_challenge(c.id) OR (p_code IS NOT NULL AND c.invite_code = lower(trim(p_code)))) THEN
    RAISE EXCEPTION 'sem acesso a este desafio';
  END IF;
  IF c.ends_at < NOW() THEN RAISE EXCEPTION 'desafio encerrado'; END IF;
  IF c.max_participants IS NOT NULL
     AND (SELECT COUNT(*) FROM challenge_participants WHERE challenge_id = c.id) >= c.max_participants THEN
    RAISE EXCEPTION 'desafio lotado';
  END IF;
  IF c.mode = 'team' AND (p_team IS NULL OR NOT (p_team = ANY (c.teams))) THEN
    RAISE EXCEPTION 'escolha uma equipe válida';
  END IF;
  INSERT INTO challenge_participants (challenge_id, profile_id, team)
  VALUES (c.id, auth.uid(), CASE WHEN c.mode = 'team' THEN p_team END)
  ON CONFLICT DO NOTHING;
END $$;

-- Placar: calculado direto dos treinos reais, respeitando as regras do desafio.
CREATE OR REPLACE FUNCTION challenge_standings(p_challenge UUID)
RETURNS TABLE (profile_id UUID, username TEXT, avatar_url TEXT, team TEXT, score NUMERIC, rank BIGINT)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
DECLARE c challenges%ROWTYPE;
BEGIN
  SELECT * INTO c FROM challenges WHERE id = p_challenge;
  IF NOT FOUND OR NOT can_view_challenge(p_challenge) THEN RETURN; END IF;

  RETURN QUERY
  WITH eligible AS (
    SELECT w.id AS wid, w.profile_id AS pid, w.started_at, w.finished_at,
           ROW_NUMBER() OVER (PARTITION BY w.profile_id, (w.started_at AT TIME ZONE c.tz)::date
                              ORDER BY w.started_at) AS day_n
      FROM workouts w
      JOIN challenge_participants cp ON cp.challenge_id = c.id AND cp.profile_id = w.profile_id
     WHERE w.finished_at IS NOT NULL
       AND w.started_at >= c.starts_at AND w.finished_at <= c.ends_at
       AND extract(epoch FROM (w.finished_at - w.started_at)) / 60 >= c.min_minutes_per_workout
  ),
  wk AS (
    SELECT e.pid, e.wid, (e.started_at AT TIME ZONE c.tz)::date AS d,
           extract(epoch FROM (e.finished_at - e.started_at)) / 60 AS minutes,
           x.n_sets, x.n_reps, x.vol, x.max_w
      FROM eligible e
      CROSS JOIN LATERAL (
        SELECT COUNT(s.id)::numeric AS n_sets, COALESCE(SUM(s.reps), 0)::numeric AS n_reps,
               COALESCE(SUM(s.reps * s.weight_kg), 0)::numeric AS vol, COALESCE(MAX(s.weight_kg), 0)::numeric AS max_w
          FROM workout_sets s
          JOIN exercises ex ON ex.id = s.exercise_id
         WHERE s.workout_id = e.wid
           AND (c.exercise_name IS NULL OR lower(ex.name) = lower(c.exercise_name))
      ) x
     WHERE e.day_n <= c.max_workouts_per_day
       AND (c.exercise_name IS NULL OR x.n_sets > 0)
  ),
  agg AS (
    SELECT k.pid,
           CASE c.metric
             WHEN 'workouts'            THEN COUNT(*)::numeric
             WHEN 'active_days'         THEN COUNT(DISTINCT k.d)::numeric
             WHEN 'training_minutes'    THEN round(SUM(k.minutes))
             WHEN 'sets'                THEN SUM(k.n_sets)
             WHEN 'reps'                THEN SUM(k.n_reps)
             WHEN 'volume_kg'           THEN round(SUM(k.vol))
             WHEN 'exercise_volume'     THEN round(SUM(k.vol))
             WHEN 'exercise_max_weight' THEN MAX(k.max_w)
           END AS sc
      FROM wk k GROUP BY k.pid
  )
  SELECT cp.profile_id, pr.username, pr.avatar_url, cp.team,
         COALESCE(a.sc, 0)::numeric AS score,
         RANK() OVER (ORDER BY COALESCE(a.sc, 0) DESC) AS rank
    FROM challenge_participants cp
    JOIN profiles pr ON pr.id = cp.profile_id
    LEFT JOIN agg a ON a.pid = cp.profile_id
   WHERE cp.challenge_id = c.id
   ORDER BY score DESC, pr.username;
END $$;

-- Músicas mais tocadas nos treinos de alguém (só se a pessoa liberou)
CREATE OR REPLACE FUNCTION profile_top_tracks(p_profile UUID, p_limit INTEGER DEFAULT 20)
RETURNS TABLE (title TEXT, artist TEXT, album TEXT, plays BIGINT, last_played TIMESTAMPTZ)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT t.title, t.artist, MAX(t.album), COUNT(*)::bigint, MAX(t.played_at)
    FROM workout_tracks t
    JOIN profiles p ON p.id = t.profile_id
   WHERE t.profile_id = p_profile
     AND (p_profile = auth.uid() OR (p.share_music AND can_view_profile(p_profile)))
   GROUP BY t.title, t.artist
   ORDER BY COUNT(*) DESC, MAX(t.played_at) DESC
   LIMIT LEAST(GREATEST(p_limit, 1), 100);
$$;

-- Números do perfil (para a área de perfil)
CREATE OR REPLACE FUNCTION profile_stats(p_profile UUID) RETURNS JSONB
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT CASE WHEN NOT can_view_profile(p_profile) THEN NULL ELSE (
    SELECT jsonb_build_object(
      'workouts',     COUNT(DISTINCT w.id),
      'active_days',  COUNT(DISTINCT w.started_at::date),
      'sets',         COUNT(s.id),
      'volume_kg',    COALESCE(round(SUM(s.reps * s.weight_kg)), 0),
      'since',        (SELECT created_at FROM profiles WHERE id = p_profile))
      FROM workouts w LEFT JOIN workout_sets s ON s.workout_id = w.id
     WHERE w.profile_id = p_profile AND w.finished_at IS NOT NULL) END;
$$;

-- ============================================================================
-- Perfis públicos (só colunas inofensivas, respeitando a visibilidade).
-- É função (não view) para não disparar o alerta "Security Definer View" do Supabase; ver 004.
-- ============================================================================
CREATE OR REPLACE FUNCTION public_profiles()
RETURNS TABLE (id UUID, username TEXT, full_name TEXT, avatar_url TEXT, bio TEXT, visibility TEXT, created_at TIMESTAMPTZ)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT p.id, p.username, p.full_name, p.avatar_url, p.bio, p.visibility::text, p.created_at
    FROM public.profiles p WHERE public.can_view_profile(p.id);
$$;

-- ============================================================================
-- RLS
-- ============================================================================
ALTER TABLE follows                ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members          ENABLE ROW LEVEL SECURITY;
ALTER TABLE challenges             ENABLE ROW LEVEL SECURITY;
ALTER TABLE challenge_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE feed_posts             ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_reactions         ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_tracks         ENABLE ROW LEVEL SECURITY;

-- follows
DROP POLICY IF EXISTS follows_select ON follows;
CREATE POLICY follows_select ON follows FOR SELECT USING (follower_id = auth.uid() OR followed_id = auth.uid());
DROP POLICY IF EXISTS follows_insert ON follows;
CREATE POLICY follows_insert ON follows FOR INSERT WITH CHECK (follower_id = auth.uid());
DROP POLICY IF EXISTS follows_update ON follows;
CREATE POLICY follows_update ON follows FOR UPDATE USING (followed_id = auth.uid()) WITH CHECK (followed_id = auth.uid());
DROP POLICY IF EXISTS follows_delete ON follows;
CREATE POLICY follows_delete ON follows FOR DELETE USING (follower_id = auth.uid() OR followed_id = auth.uid());

-- groups
DROP POLICY IF EXISTS groups_select ON groups;
CREATE POLICY groups_select ON groups FOR SELECT USING (privacy = 'public' OR is_group_member(id));
DROP POLICY IF EXISTS groups_insert ON groups;
CREATE POLICY groups_insert ON groups FOR INSERT WITH CHECK (owner_id = auth.uid());
DROP POLICY IF EXISTS groups_update ON groups;
CREATE POLICY groups_update ON groups FOR UPDATE USING (owner_id = auth.uid()) WITH CHECK (owner_id = auth.uid());
DROP POLICY IF EXISTS groups_delete ON groups;
CREATE POLICY groups_delete ON groups FOR DELETE USING (owner_id = auth.uid());

-- group_members (entrada só pelas RPCs)
DROP POLICY IF EXISTS gm_select ON group_members;
CREATE POLICY gm_select ON group_members FOR SELECT
  USING (is_group_member(group_id) OR EXISTS (SELECT 1 FROM groups g WHERE g.id = group_id AND g.privacy = 'public'));
DROP POLICY IF EXISTS gm_delete ON group_members;
CREATE POLICY gm_delete ON group_members FOR DELETE
  USING ((profile_id = auth.uid() AND role <> 'owner') OR (is_group_admin(group_id) AND role = 'member'));
DROP POLICY IF EXISTS gm_update ON group_members;
CREATE POLICY gm_update ON group_members FOR UPDATE
  USING (EXISTS (SELECT 1 FROM groups g WHERE g.id = group_id AND g.owner_id = auth.uid()) AND role <> 'owner')
  WITH CHECK (role IN ('admin','member'));

-- challenges
DROP POLICY IF EXISTS ch_select ON challenges;
CREATE POLICY ch_select ON challenges FOR SELECT USING (can_view_challenge(id));
DROP POLICY IF EXISTS ch_insert ON challenges;
CREATE POLICY ch_insert ON challenges FOR INSERT
  WITH CHECK (creator_id = auth.uid() AND (group_id IS NULL OR is_group_admin(group_id)));
DROP POLICY IF EXISTS ch_update ON challenges;
CREATE POLICY ch_update ON challenges FOR UPDATE USING (creator_id = auth.uid()) WITH CHECK (creator_id = auth.uid());
DROP POLICY IF EXISTS ch_delete ON challenges;
CREATE POLICY ch_delete ON challenges FOR DELETE USING (creator_id = auth.uid());

-- challenge_participants (entrada só por join_challenge)
DROP POLICY IF EXISTS cp_select ON challenge_participants;
CREATE POLICY cp_select ON challenge_participants FOR SELECT USING (can_view_challenge(challenge_id));
DROP POLICY IF EXISTS cp_delete ON challenge_participants;
CREATE POLICY cp_delete ON challenge_participants FOR DELETE
  USING (profile_id = auth.uid()
         OR EXISTS (SELECT 1 FROM challenges c WHERE c.id = challenge_id AND c.creator_id = auth.uid()));

-- feed_posts
DROP POLICY IF EXISTS fp_select ON feed_posts;
CREATE POLICY fp_select ON feed_posts FOR SELECT USING (can_view_post(profile_id, visibility));
DROP POLICY IF EXISTS fp_update ON feed_posts;
CREATE POLICY fp_update ON feed_posts FOR UPDATE USING (profile_id = auth.uid()) WITH CHECK (profile_id = auth.uid());
DROP POLICY IF EXISTS fp_delete ON feed_posts;
CREATE POLICY fp_delete ON feed_posts FOR DELETE USING (profile_id = auth.uid());
REVOKE UPDATE ON feed_posts FROM authenticated;
GRANT  UPDATE (caption, visibility) ON feed_posts TO authenticated;   -- stats não são editáveis

-- post_reactions
DROP POLICY IF EXISTS pr_select ON post_reactions;
CREATE POLICY pr_select ON post_reactions FOR SELECT
  USING (EXISTS (SELECT 1 FROM feed_posts fp WHERE fp.id = post_id AND can_view_post(fp.profile_id, fp.visibility)));
DROP POLICY IF EXISTS pr_insert ON post_reactions;
CREATE POLICY pr_insert ON post_reactions FOR INSERT
  WITH CHECK (profile_id = auth.uid()
              AND EXISTS (SELECT 1 FROM feed_posts fp WHERE fp.id = post_id AND can_view_post(fp.profile_id, fp.visibility)));
DROP POLICY IF EXISTS pr_delete ON post_reactions;
CREATE POLICY pr_delete ON post_reactions FOR DELETE USING (profile_id = auth.uid());

-- workout_tracks
DROP POLICY IF EXISTS wt_select ON workout_tracks;
CREATE POLICY wt_select ON workout_tracks FOR SELECT USING (can_view_tracks(profile_id, workout_id));
DROP POLICY IF EXISTS wt_insert ON workout_tracks;
CREATE POLICY wt_insert ON workout_tracks FOR INSERT
  WITH CHECK (profile_id = auth.uid()
              AND EXISTS (SELECT 1 FROM workouts w WHERE w.id = workout_id AND w.profile_id = auth.uid()));
DROP POLICY IF EXISTS wt_delete ON workout_tracks;
CREATE POLICY wt_delete ON workout_tracks FOR DELETE USING (profile_id = auth.uid());

-- ============================================================================
-- Permissões: nada para anon; funções só para usuários logados
-- ============================================================================

REVOKE EXECUTE ON FUNCTION public_profiles(), join_group(UUID), join_group_by_code(TEXT), join_challenge(UUID, TEXT, TEXT),
  challenge_standings(UUID), profile_top_tracks(UUID, INTEGER), profile_stats(UUID) FROM PUBLIC, anon;
GRANT  EXECUTE ON FUNCTION public_profiles(), join_group(UUID), join_group_by_code(TEXT), join_challenge(UUID, TEXT, TEXT),
  challenge_standings(UUID), profile_top_tracks(UUID, INTEGER), profile_stats(UUID) TO authenticated;
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
drop policy if exists "dono lê seus pesos" on public.body_weights;
create policy "dono lê seus pesos" on public.body_weights
  for select using (profile_id = auth.uid());
drop policy if exists "dono registra seus pesos" on public.body_weights;
create policy "dono registra seus pesos" on public.body_weights
  for insert with check (profile_id = auth.uid());
drop policy if exists "dono altera seus pesos" on public.body_weights;
create policy "dono altera seus pesos" on public.body_weights
  for update using (profile_id = auth.uid()) with check (profile_id = auth.uid());
drop policy if exists "dono apaga seus pesos" on public.body_weights;
create policy "dono apaga seus pesos" on public.body_weights
  for delete using (profile_id = auth.uid());
commit;
