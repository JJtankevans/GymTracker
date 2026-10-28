# GymTracker

App Android de registro de treinos de academia, com camada social. Feito em Kotlin (Jetpack Compose) com a lógica num módulo Kotlin Multiplatform e backend no Supabase.

## O que o app faz
- **Treino:** grupos de treino (ex.: "Pernas") com exercícios (um exercício pode estar em vários grupos), treino ativo com pré-preenchimento da última carga, sugestão de carga, detecção de recorde (com confete), descanso com aviso também com a tela bloqueada, modo circuito (beta, conta séries e descanso sozinho e aprende o ritmo), resumo e compartilhamento no fim.
- **Catálogo:** 93 exercícios (inclui cardio e mobilidade) com séries/reps padrão e ilustrações (41 prontas), 9 treinos prontos, agenda da semana e sugestão do próximo grupo.
- **Acompanhamento:** Histórico com mapa de calor, detalhe do treino com anotação, Recordes, detalhe do exercício (gráfico, 1RM, tabela de cargas, calculadora de anilhas com aquecimento), Evolução (4 levantamentos básicos, séries por músculo na semana, peso e medidas corporais), Conquistas e exportação CSV.
- **Social:** perfil público/só seguidores/privado, seguir, feed de treinos com reações, grupos, desafios com placar, mensagens e títulos (Alpha, Chad...), compartilhar um grupo de treino por código.
- **Aparência:** 40 temas em 4 categorias (Profissionais, Ousados, Abstratos, Loucos), vários com fundo animado e layouts de Home por tema.
- **Relógio (`:wear`):** app para Wear OS (com tile e complicação de mostrador) que recebe os grupos do celular, registra o treino exercício por exercício (carga sugerida, coroa, descanso com alarme, batimentos, tela sempre ligada) e manda o treino para o celular gravar no Supabase. Instalação e detalhes em `docs/RELOGIO.md`.

## Estrutura
| Módulo | Conteúdo |
|--------|----------|
| `:shared` | KMP (Android; iOS compila e testa no CI, ainda sem telas): modelos, repositórios Supabase, casos de uso, ViewModels e regras puras. Testes em `shared/src/commonTest` (278 `@Test`; rodam no CI do Android e no simulador de iOS). |
| `:app` | Telas Compose, temas, navegação, notificações, música, armazenamento local. Galerias de captura (Robolectric + Roborazzi) em `app/src/test`. |
| `:wear` | App do relógio (usa o `:shared` para o protocolo com o celular e as regras do treino). |
| `docs/` | Documentação (ver abaixo) e SQL do Supabase em `docs/supabase/`. |
| iOS | Ainda sem telas: o `:shared` compila para iPhone e passa nos testes no simulador (`.github/workflows/ios.yml`). Plano em `docs/IOS.md`. |
| `tools/` | `cortar_folha.py`: corta folhas de ilustrações em PNGs por exercício. |

## Como compilar
1. Crie `local.properties` na raiz (fora do git) com:
   ```
   SUPABASE_URL=https://<projeto>.supabase.co
   SUPABASE_KEY=sb_publishable_...
   ```
   Use a chave **publicável**, nunca a `service_role`.
2. `./gradlew :app:assembleDebug` (no Windows, `gradlew.bat`). APK em `app/build/outputs/apk/debug/app-debug.apk`.
3. Testes: `./gradlew :shared:testDebugUnitTest`.

O GitHub Actions (`.github/workflows/android.yml`) faz o mesmo a cada push/PR e publica o APK no artefato `gymtracker-debug-apk` (precisa dos secrets `SUPABASE_URL` e `SUPABASE_KEY` no repositório).

## Banco (Supabase)
Esquema base em `app/docs/supabaseSchema.md`; migrações numeradas em `docs/supabase/` (`001` a `011`), idempotentes, rodadas à mão no SQL Editor. Qual já foi aplicada está em `docs/CONTEXTO_SESSAO.md`.

## Documentação
- `docs/CONTEXTO_SESSAO.md`: estado atual, o que falta testar, próximos passos (comece por aqui).
- `docs/ERROS_CONHECIDOS.md`: erros encontrados na revisão de código e como corrigir.
- `docs/PROXIMOS_PASSOS.md`: histórico das rodadas de trabalho.
- `docs/SOCIAL_DESIGN.md`, `docs/IMAGENS_EXERCICIOS.md`, `docs/PROMPTS_IMAGENS_EXERCICIOS.md`, `docs/REFERENCIAS_DESIGN.md`, `docs/REFERENCIAS_TEMPOS.md`.
- `app/docs/PlanoDeAcao.md`: plano original do projeto (histórico).
- `CLAUDE.md`: instruções para as sessões do Claude.
