# GymTracker — instruções para sessões do Claude

## Ao iniciar a sessão
1. Leia `docs/CONTEXTO_SESSAO.md` primeiro (estado atual, armadilhas, próximos passos).
2. O histórico longo de decisões está em `docs/PROXIMOS_PASSOS.md`.
3. Responda em português do Brasil, de forma direta, e relate com honestidade o que não foi testado no aparelho.

## Ambiente de nuvem
- O contêiner é efêmero: o que não foi commitado e enviado com push se perde quando a sessão termina ou o espaço em disco acaba.
- Não há Windows/adb aqui; os comandos de `docs/CONTEXTO_SESSAO.md` sobre `gradlew.bat` e adb valem para o PC do Thiago.
- **O contêiner não compila o app** (sem Android SDK; `dl.google.com` bloqueado). Para compilar e testar, faça push: o GitHub Actions (`.github/workflows/android.yml`) gera o APK e roda os testes do `:shared`. Acompanhe com `gh api "repos/JJtankevans/GymTracker/actions/runs?branch=<branch>&per_page=1"` e leia os erros pelas anotações do job (`gh api repos/JJtankevans/GymTracker/check-runs/<job_id>/annotations`), porque os logs do Actions não abrem daqui. O APK fica no artefato `gymtracker-debug-apk` da execução.
- Espere uma execução terminar antes do próximo push no mesmo branch (o workflow cancela a anterior).
- SQL dá para testar com o Postgres 16 local (`/usr/lib/postgresql/16/bin`, rodando como usuário `postgres` num diretório em `/var/tmp`).

## Regra de fim de sessão (obrigatória)
Antes de encerrar a sessão, e antes de qualquer limpeza de disco/cache que possa comprometer as mudanças:
1. **Avise o Thiago** que há mudanças não commitadas e peça autorização para fazer o commit (e o push).
2. **Atualize `docs/CONTEXTO_SESSAO.md`** resumindo: o que foi feito na sessão, o que foi testado e o que não foi, onde o projeto parou, problemas em aberto e próximos passos. Mantenha o formato e a data de atualização no título.
3. Só depois de autorizado: commit (mensagem em português, descritiva) e `git push -u origin <branch>`.
4. Se o push for recusado, avise claramente e informe o que ainda está só local.

## Segurança
- Nunca gravar senhas, chaves ou `local.properties` em arquivos versionados.
- Não digitar senhas de contas de teste no app; não apagar dados do Thiago.
