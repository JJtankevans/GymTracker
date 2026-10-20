# Camada social — desenho

Objetivo: usuários interagirem **pelo treino e pelas playlists**, sem chat (por enquanto). Tudo opt-in: o perfil nasce `private`, sem compartilhar treino nem música.

## O que existe (SQL: `docs/supabase/002_social.sql`, testado num Postgres local com 3 usuários)
| Peça | Como funciona |
|---|---|
| **Perfil** | `bio`, `visibility` (public / followers / private), `share_workouts`, `share_music`. View `public_profiles` só mostra colunas seguras e respeita visibilidade. `profile_stats()` dá os números do perfil. |
| **Seguir** | Aceito direto se o perfil é público; pendente caso contrário (o dono aceita). |
| **Grupos** | Públicos (qualquer um entra) ou por convite (código de 8 caracteres). Papéis: dono / admin / membro. Limite de membros. Entrada só por RPC (`join_group`, `join_group_by_code`). |
| **Desafios** | Em grupo ou abertos. Métricas: treinos, dias ativos, volume (kg), séries, repetições, minutos treinados, maior carga em um exercício, volume de um exercício. Modo individual ou em equipes. Meta opcional, período, limite de participantes, convite. **Regras anti-trapaça:** mínimo de minutos por treino e máximo de treinos por dia. Placar calculado direto dos treinos reais (`challenge_standings`). |
| **Feed** | Quando o treino termina e o usuário liberou, vira um post com resumo (minutos, séries, volume...). Só legenda e visibilidade são editáveis; as estatísticas não. |
| **Reações** | 💪 flex, 🔥 fire, 👏 clap, 🎧 headphones. Só em post que você pode ver. |
| **Músicas do treino** | O app grava faixa + artista durante o treino; se `share_music`, outros veem a playlist do treino no post e as "mais tocadas" no perfil. |

## Limites honestos (leia antes de lançar desafios valendo prêmio)
- **Dá pra trapacear.** O app grava as séries direto no banco; um usuário técnico pode inserir séries falsas pela API. As regras (mín. minutos, máx. por dia) dificultam, não impedem. Para desafios sérios: moderação do admin do grupo (já pode remover participante) e, depois, validar tempo no servidor (ex.: trigger que ignora séries com `completed_at` do cliente).
- Nome de exibição único (`lower(username)`) está comentado na migração; ative se não houver repetidos.
- Música depende da permissão de acesso à mídia no celular e de o app de música expor título/artista (Spotify e YouTube Music expõem).
- Falta tudo do lado do app: telas de grupos, desafios, feed e perfil robusto, e os repositórios Kotlin. O SQL e as regras de segurança já estão prontos e testados.

## Ordem sugerida de construção
1. Rodar `001` e `002` no projeto Supabase certo; rodar `test_social.sql` em um banco de teste/branch.
2. Perfil robusto (editar bio, visibilidade, toggles de compartilhar) + captura de músicas (já no app) + upload.
3. Grupos (criar/entrar por código) e lista de membros.
4. Desafios (assistente de criação com todas as opções) + placar ao vivo (Realtime).
5. Feed + reações; perfil público com playlist.
6. Ideias seguintes: ligas semanais, selos, "treinar junto" (sessão simultânea), duelos 1×1, ranking por exercício.
