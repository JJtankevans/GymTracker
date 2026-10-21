# Camada social — desenho

Objetivo: usuários interagirem **pelo treino e pelas playlists**, sem chat (por enquanto). Tudo opt-in: o perfil nasce `private`, sem compartilhar treino nem música.

> **Status (21/10/2026):** o lado do app existe (feed, reações, grupos, desafios, pessoas, privacidade; ver `CONTEXTO_SESSAO.md` e `PROXIMOS_PASSOS.md`, Sétima rodada) e foi testado no celular com duas contas. As migrações 002, 004 (`public_profiles()` como função) e 005 (perfis "Só seguidores" aparecem na busca, com bio/números/posts escondidos até aceitar) já foram aplicadas no Supabase. Ainda sem teste no aparelho: desafio em equipes, privacidade de música, sair de grupo/desafio, tela de erro "Tentar de novo". Desafios (21/10, 4ª sessão): o placar mostra a foto dos participantes; a folha do desafio mostra mensagens (frases do Thiago em 3 layouts, `ChallengeMessages.kt`) e o perfil pode exibir um título de honra (Alpha/Chad/Giga Chad/Mr. Olimpia, por dias seguidos) ou de falha (treino abaixo do mínimo), configurados por desafio (`fail_threshold`, `streak_goal`; SQL 010; regras em `CONTEXTO_SESSAO.md`). Foto de perfil: o usuário já envia e troca a foto no Perfil (Supabase Storage, ver `007`) e ela aparece nas linhas de Pessoas; falta mostrar no feed, no placar e no perfil público. Pendentes de produto: notificações, ligas semanais.

## O que existe (SQL: `docs/supabase/002_social.sql`, testado num Postgres local com 3 usuários)
| Peça | Como funciona |
|---|---|
| **Perfil** | `bio`, `visibility` (public / followers / private), `share_workouts`, `share_music`. View `public_profiles` só mostra colunas seguras e respeita visibilidade. `profile_stats()` dá os números do perfil. |
| **Seguir** | Aceito direto se o perfil é público; pendente caso contrário (o dono aceita). |
| **Grupos** | Públicos (qualquer um entra) ou por convite (código de 8 caracteres). Papéis: dono / admin / membro. Limite de membros. Entrada só por RPC (`join_group`, `join_group_by_code`). |
| **Desafios** | Em grupo ou abertos. Métricas: treinos, dias ativos, volume (kg), séries, repetições, minutos treinados, maior carga em um exercício, volume de um exercício. Modo individual ou em equipes. Meta opcional, período, limite de participantes, convite. **Regras anti-trapaça:** mínimo de minutos por treino e máximo de treinos por dia. Placar calculado direto dos treinos reais (`challenge_standings`). |
| **Feed** | Quando o treino termina e o usuário liberou, vira um post com resumo (minutos, séries, volume...). Só legenda e visibilidade são editáveis; as estatísticas não. |
| **Reações** | flex, fire, clap, headphones (no app são ícones desenhados, não emojis). Só em post que você pode ver. |
| **Músicas do treino** | O app grava faixa + artista durante o treino; se `share_music`, outros veem a playlist do treino no post e as "mais tocadas" no perfil. |

## Limites honestos (leia antes de lançar desafios valendo prêmio)
- **Dá pra trapacear.** O app grava as séries direto no banco; um usuário técnico pode inserir séries falsas pela API. As regras (mín. minutos, máx. por dia) dificultam, não impedem. Para desafios sérios: moderação do admin do grupo (já pode remover participante) e, depois, validar tempo no servidor (ex.: trigger que ignora séries com `completed_at` do cliente).
- Nome de exibição único (`lower(username)`) está comentado na migração; ative se não houver repetidos.
- Música depende da permissão de acesso à mídia no celular e de o app de música expor título/artista (Spotify e YouTube Music expõem).
- *(Superado: as telas e os repositórios Kotlin já existem; ver o status no topo.)* As regras de segurança ficam no banco (RLS); o app só consome.

## Ordem sugerida de construção *(itens 1 a 5 já feitos; resta o 6)*
1. Rodar `001` e `002` no projeto Supabase certo; rodar `test_social.sql` em um banco de teste/branch.
2. Perfil robusto (editar bio, visibilidade, toggles de compartilhar) + captura de músicas (já no app) + upload.
3. Grupos (criar/entrar por código) e lista de membros.
4. Desafios (assistente de criação com todas as opções) + placar ao vivo (Realtime).
5. Feed + reações; perfil público com playlist.
6. Ideias seguintes: ligas semanais, selos, "treinar junto" (sessão simultânea), duelos 1×1, ranking por exercício.
