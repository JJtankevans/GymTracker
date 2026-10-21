# 🏋️ GymTracker — Plano de Ação Completo
### Kotlin Multiplatform (Android & iOS) + Supabase

---

## Sumário

1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Stack Tecnológica](#2-stack-tecnológica)
3. [Modelagem do Banco de Dados (Supabase)](#3-modelagem-do-banco-de-dados-supabase)
4. [Arquitetura do Projeto](#4-arquitetura-do-projeto)
5. [Fases de Desenvolvimento](#5-fases-de-desenvolvimento)
6. [Detalhamento das Telas](#6-detalhamento-das-telas)
7. [Fluxo de Navegação](#7-fluxo-de-navegação)
8. [Cronograma Sugerido](#8-cronograma-sugerido)

---

## 1. Visão Geral do Projeto

O **GymTracker** é um aplicativo mobile multiplataforma (Android e iOS) para registro e acompanhamento de treinos de academia. O usuário poderá:

- Cadastrar-se e fazer login com autenticação segura.
- Criar grupos musculares (estilo treino ABC) e associar exercícios.
- Registrar sessões de treino com séries, repetições e cargas.
- Acompanhar a progressão de carga de cada exercício via gráfico.
- Monitorar a frequência semanal de treinos.
- Usar um temporizador de descanso entre séries durante o treino.

---

## 2. Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| **UI Android** | Jetpack Compose |
| **UI iOS** | SwiftUI (via Compose Multiplatform ou bindings nativos) |
| **Lógica compartilhada** | Kotlin Multiplatform (KMP) |
| **Backend / BaaS** | Supabase (PostgreSQL + Auth + Realtime) |
| **Cliente Supabase KMP** | `supabase-kt` (biblioteca oficial) |
| **Injeção de Dependência** | Koin Multiplatform |
| **Navegação** | Decompose ou Compose Navigation |
| **Gráficos** | Vico (Android) / Charts SwiftUI (iOS) ou MPAndroidChart |
| **Armazenamento Local** | SQLDelight (cache offline) |
| **Serialização** | kotlinx.serialization |
| **Async** | Kotlin Coroutines + Flow |
| **Build** | Gradle (Version Catalogs) |

---

## 3. Modelagem do Banco de Dados (Supabase)

### 3.1 Diagrama de Relacionamentos

```
profiles
  └──< muscle_groups
          └──< workout_exercises (exercícios do grupo)
                  └── exercises (tabela mestra)

workouts (sessão de treino)
  ├── profiles (dono)
  ├── muscle_groups (grupo treinado)
  └──< workout_sets (séries registradas na sessão)
          └── exercises (exercício realizado)
```

---

### 3.2 Tabelas e Campos

---

#### Tabela: `profiles`
Estende o usuário autenticado do Supabase Auth.

```sql
CREATE TABLE profiles (
  id            UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username      TEXT NOT NULL,
  full_name     TEXT,
  avatar_url    TEXT,
  weekly_goal   INTEGER NOT NULL DEFAULT 3,   -- meta de treinos por semana
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Campos:**
- `id` → FK para `auth.users` do Supabase (mesmo UUID)
- `username` → apelido de exibição
- `weekly_goal` → quantas vezes por semana o usuário pretende treinar

---

#### Tabela: `muscle_groups`
Grupos musculares criados pelo usuário (ex: Treino A, Treino B, Peito/Tríceps).

```sql
CREATE TABLE muscle_groups (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id  UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name        TEXT NOT NULL,                  -- "Treino A", "Peito e Tríceps", etc.
  color_hex   TEXT DEFAULT '#6200EE',         -- cor para identificação visual
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Relacionamentos:**
- `profile_id` → pertence a um usuário
- Um `profile` pode ter N `muscle_groups`

---

#### Tabela: `exercises`
Catálogo de exercícios do usuário.

```sql
CREATE TABLE exercises (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id      UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  muscle_group_id UUID REFERENCES muscle_groups(id) ON DELETE SET NULL,
  name            TEXT NOT NULL,              -- "Supino Reto", "Agachamento", etc.
  default_sets    INTEGER NOT NULL DEFAULT 3, -- séries padrão
  default_reps    INTEGER NOT NULL DEFAULT 12,-- repetições padrão
  notes           TEXT,                       -- observações opcionais
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Relacionamentos:**
- `profile_id` → exercício pertence ao usuário
- `muscle_group_id` → **legado** (21/10/2026): o exercício pertence a um grupo muscular (opcional)
- Um `muscle_group` pode ter N `exercises` e um `exercise` pode estar em N `muscle_groups`, pela tabela `group_exercises` (migração 006)

---

#### Tabela: `workouts`
Representa uma sessão de treino (um dia de academia).

```sql
CREATE TABLE workouts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id      UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  muscle_group_id UUID REFERENCES muscle_groups(id) ON DELETE SET NULL,
  started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at     TIMESTAMPTZ,                -- NULL enquanto treino está em andamento
  notes           TEXT,                       -- observações do treino do dia
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Relacionamentos:**
- `profile_id` → sessão pertence ao usuário
- `muscle_group_id` → grupo muscular treinado nessa sessão
- Um `profile` pode ter N `workouts`

---

#### Tabela: `workout_sets`
Cada série registrada durante uma sessão de treino.

```sql
CREATE TABLE workout_sets (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workout_id   UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
  exercise_id  UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  set_number   INTEGER NOT NULL,              -- número da série (1, 2, 3...)
  reps         INTEGER NOT NULL,              -- repetições realizadas
  weight_kg    NUMERIC(6, 2) NOT NULL DEFAULT 0, -- carga em kg
  rest_seconds INTEGER,                       -- tempo de descanso configurado (em segundos)
  completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Relacionamentos:**
- `workout_id` → pertence a uma sessão de treino
- `exercise_id` → exercício executado
- Um `workout` pode ter N `workout_sets` (de diferentes exercícios)
- Um `exercise` pode aparecer em N `workout_sets` (histórico completo de carga)

---

### 3.3 Visão Geral do Relacionamento

```
profiles (1)
  ├──(N) muscle_groups (N)──group_exercises──(N) exercises   (desde a migração 006)
  │
  ├──(N) exercises
  │
  └──(N) workouts (1)
            ├── muscle_groups (N:1)
            └──(N) workout_sets
                      └── exercises (N:1)
```

---

### 3.4 Consultas Importantes

**Progressão de carga de um exercício:**
```sql
SELECT
  w.started_at::DATE AS treino_data,
  ws.set_number,
  ws.reps,
  ws.weight_kg
FROM workout_sets ws
JOIN workouts w ON w.id = ws.workout_id
WHERE ws.exercise_id = :exercise_id
  AND w.profile_id   = :profile_id
ORDER BY w.started_at ASC;
```

**Frequência semanal (últimas 4 semanas):**
```sql
SELECT
  DATE_TRUNC('week', started_at) AS semana,
  COUNT(*)                        AS total_treinos
FROM workouts
WHERE profile_id = :profile_id
  AND started_at >= NOW() - INTERVAL '4 weeks'
GROUP BY semana
ORDER BY semana;
```

**Carga máxima por exercício:**
```sql
SELECT
  e.name,
  MAX(ws.weight_kg) AS carga_maxima
FROM workout_sets ws
JOIN exercises e ON e.id = ws.exercise_id
WHERE e.profile_id = :profile_id
GROUP BY e.id, e.name;
```

---

### 3.5 Row Level Security (RLS)

Ative o RLS em todas as tabelas para garantir que cada usuário acesse apenas seus próprios dados:

```sql
-- Exemplo para a tabela workouts
ALTER TABLE workouts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Usuário vê apenas seus treinos"
ON workouts FOR ALL
USING (profile_id = auth.uid());
```

Repita o padrão para: `profiles`, `muscle_groups`, `exercises`, `workout_sets`.

---

## 4. Arquitetura do Projeto

### 4.1 Estrutura de Módulos KMP

```
GymTracker/
├── androidApp/             # App Android (Jetpack Compose)
├── iosApp/                 # App iOS (SwiftUI / Compose Multiplatform)
└── shared/                 # Módulo KMP compartilhado
    ├── commonMain/
    │   ├── data/
    │   │   ├── remote/     # Repositórios Supabase
    │   │   ├── local/      # SQLDelight (cache)
    │   │   └── model/      # Data classes (kotlinx.serialization)
    │   ├── domain/
    │   │   ├── usecase/    # Casos de uso
    │   │   └── repository/ # Interfaces de repositório
    │   └── presentation/
    │       └── viewmodel/  # ViewModels (StateFlow)
    ├── androidMain/        # Implementações Android específicas
    └── iosMain/            # Implementações iOS específicas
```

### 4.2 Padrão de Arquitetura

Utilizar **MVVM + Clean Architecture** com separação clara entre camadas:

```
View (Compose / SwiftUI)
  ↕
ViewModel (shared KMP)
  ↕
UseCase (shared KMP)
  ↕
Repository Interface (shared KMP)
  ↕
Repository Implementation (shared KMP)
  ↕
Supabase Client / SQLDelight
```

---

## 5. Fases de Desenvolvimento

---

### FASE 1 — Setup e Infraestrutura (Semana 1–2)

**Objetivo:** Projeto funcionando com autenticação e conexão ao Supabase.

#### Passo 1.1 — Configurar o projeto KMP
- [ ] Criar projeto com o [Kotlin Multiplatform Wizard](https://kmp.jetbrains.com)
- [ ] Configurar `gradle/libs.versions.toml` (Version Catalogs)
- [ ] Adicionar dependências base: `supabase-kt`, `kotlinx.serialization`, `kotlinx.coroutines`, `koin-multiplatform`

#### Passo 1.2 — Criar projeto no Supabase
- [ ] Criar projeto em [supabase.com](https://supabase.com)
- [ ] Copiar `SUPABASE_URL` e `SUPABASE_ANON_KEY`
- [ ] Criar todas as tabelas conforme seção 3 (via SQL Editor ou migrations)
- [ ] Configurar RLS em todas as tabelas
- [ ] Ativar autenticação por e-mail/senha

#### Passo 1.3 — Configurar o cliente Supabase no KMP
```kotlin
// shared/commonMain/di/SupabaseModule.kt
val supabaseClient = createSupabaseClient(
    supabaseUrl = "https://xxxx.supabase.co",
    supabaseKey = "eyJ..."
) {
    install(Auth)
    install(Postgrest)
    install(Storage)
}
```

#### Passo 1.4 — Implementar autenticação
- [ ] Tela de Login (e-mail + senha)
- [ ] Tela de Cadastro (e-mail + senha + username)
- [ ] Criação automática do `profile` via Supabase Trigger após signup
- [ ] Persistência da sessão (token refresh automático pelo `supabase-kt`)

---

### FASE 2 — Cadastro de Exercícios e Grupos (Semana 3–4)

**Objetivo:** Usuário consegue montar sua estrutura de treino.

#### Passo 2.1 — CRUD de Grupos Musculares
- [ ] Tela listando grupos musculares do usuário
- [ ] Formulário para criar/editar grupo (nome + cor)
- [ ] Opção de excluir grupo
- [ ] `MuscleGroupRepository` + `MuscleGroupViewModel`

#### Passo 2.2 — CRUD de Exercícios
- [ ] Tela listando exercícios (filtrável por grupo muscular)
- [ ] Formulário para criar exercício (nome, grupo, séries padrão, repetições padrão, observações)
- [ ] Opção de editar e excluir exercício
- [ ] `ExerciseRepository` + `ExerciseViewModel`

#### Passo 2.3 — Configuração semanal
- [ ] Tela de Perfil com campo `weekly_goal`
- [ ] Widget de frequência semanal na Home (ex: "3/4 treinos essa semana")

---

### FASE 3 — Registro de Treinos (Semana 5–6)

**Objetivo:** Usuário consegue iniciar e registrar uma sessão de treino.

#### Passo 3.1 — Iniciar treino
- [ ] Botão "Iniciar Treino" na Home
- [ ] Bottom sheet / diálogo para selecionar o grupo muscular do dia
- [ ] Criar registro em `workouts` com `started_at = now()` e `finished_at = null`

#### Passo 3.2 — Tela do Treino Ativo
- [ ] Lista de exercícios do grupo muscular selecionado
- [ ] Para cada exercício: adicionar séries (reps + peso)
- [ ] Histórico de séries registradas na sessão atual
- [ ] Botão "Finalizar Treino" → preenche `finished_at`

#### Passo 3.3 — Temporizador de descanso
- [ ] Ao completar uma série, exibir opção de iniciar temporizador
- [ ] Temporizador configurável (ex: 30s, 60s, 90s, 2min ou personalizado)
- [ ] Componente visual: círculo de progresso + segundos restantes
- [ ] Notificação sonora/vibração ao finalizar o descanso
- [ ] Implementação com `kotlinx.coroutines` (countdown flow compartilhado)

---

### FASE 4 — Dashboard e Histórico (Semana 7–8)

**Objetivo:** Visualização de progresso e histórico de treinos.

#### Passo 4.1 — Tela Home / Dashboard
- [ ] Resumo da semana: treinos realizados vs meta (`weekly_goal`)
- [ ] Streak de dias treinados
- [ ] Último treino realizado (data + grupo muscular)
- [ ] Atalho para iniciar novo treino

#### Passo 4.2 — Histórico de Treinos
- [ ] Lista de sessões passadas (data, grupo muscular, duração)
- [ ] Detalhe da sessão: exercícios e séries realizadas

#### Passo 4.3 — Gráfico de Progressão de Carga
- [ ] Tela de detalhe do exercício
- [ ] Gráfico de linha: eixo X = data, eixo Y = carga máxima na sessão
- [ ] Exibir também: maior carga registrada, última carga, total de sessões
- [ ] Usar **Vico** (Android) ou solução equivalente iOS

---

### FASE 5 — Polimento e Publicação (Semana 9–10)

**Objetivo:** App pronto para publicação.

#### Passo 5.1 — UX / UI
- [ ] Aplicar Design System consistente (Material 3 no Android, Human Interface no iOS)
- [ ] Dark mode
- [ ] Animações de transição suaves
- [ ] Estados de loading, erro e empty state em todas as telas

#### Passo 5.2 — Qualidade
- [ ] Testes unitários nos UseCases
- [ ] Testes de integração nos Repositórios (com Supabase local/dev)
- [ ] Tratamento global de erros (sem crashes visíveis ao usuário)

#### Passo 5.3 — Publicação
- [ ] Configurar signing key Android → publicar na Google Play (internal track)
- [ ] Configurar provisioning profile iOS → publicar no TestFlight
- [ ] Configurar variáveis de ambiente (Supabase URL/Key) via `BuildConfig` / `Info.plist`

---

## 6. Detalhamento das Telas

| Tela | Descrição |
|---|---|
| **Splash / Onboarding** | Verificação de sessão ativa, redireciona para Home ou Login |
| **Login** | E-mail e senha, link para cadastro |
| **Cadastro** | E-mail, senha, username, aceite de termos |
| **Home** | Frequência semanal, streak, último treino, botão iniciar |
| **Grupos Musculares** | Lista + criação de grupos (Treino A, B, C...) |
| **Exercícios** | Lista por grupo + CRUD de exercícios |
| **Iniciar Treino** | Seleção do grupo muscular do dia |
| **Treino Ativo** | Registro de séries em tempo real + temporizador |
| **Histórico** | Lista de sessões passadas |
| **Detalhe do Treino** | Exercícios e séries de uma sessão específica |
| **Detalhe do Exercício** | Gráfico de progressão + estatísticas |
| **Perfil** | Username, meta semanal, logout |

---

## 7. Fluxo de Navegação

```
Splash
  ├── [Não autenticado] → Login → Cadastro
  └── [Autenticado] → Home
        ├── → Grupos Musculares
        │     └── → Exercícios do Grupo
        │               └── → Detalhe do Exercício (gráfico)
        ├── → Iniciar Treino
        │     └── → Treino Ativo
        │               └── (Temporizador sobreposto)
        ├── → Histórico
        │     └── → Detalhe do Treino
        └── → Perfil
```

---

## 8. Cronograma Sugerido

| Semana | Fase | Entregável |
|---|---|---|
| 1–2 | Setup + Auth | Login/Cadastro funcional, Supabase conectado |
| 3–4 | Grupos + Exercícios | CRUD completo de grupos e exercícios |
| 5–6 | Treino Ativo | Registro de séries + temporizador |
| 7–8 | Dashboard + Gráfico | Home com frequência + gráfico de progressão |
| 9–10 | Polimento + Deploy | App publicado no Play Store e TestFlight |

---

## Referências e Links Úteis

- [Supabase KMP Client (`supabase-kt`)](https://github.com/supabase-community/supabase-kt)
- [Kotlin Multiplatform Wizard](https://kmp.jetbrains.com)
- [Vico Charts (Android)](https://patrykandpatrick.com/vico/)
- [Koin Multiplatform](https://insert-koin.io/docs/reference/koin-mp/kmp/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Decompose Navigation](https://arkivanov.github.io/Decompose/)
- [Supabase Row Level Security](https://supabase.com/docs/guides/auth/row-level-security)

---

*Documento gerado para o projeto GymTracker — Kotlin Multiplatform + Supabase*