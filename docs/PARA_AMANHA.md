# Para amanhã (coisas que só o Thiago pode fazer)

Escrito em 22/11/2026. Marque `[x]` conforme for fazendo. Tudo aqui precisa de você porque envolve conta, senha ou decisão; eu não consigo (ou não devo) fazer sozinho.

## 1. Banco de dados (Supabase, projeto `ygbrrypyydxnuskrobeu`)
Abra o **SQL Editor** e rode **nesta ordem** (todas aditivas e idempotentes; detalhes em `docs/MIGRACOES_PENDENTES.md`):
- [ ] `docs/supabase/013_medidas_corporais.sql` — medidas corporais na nuvem (libera #568 e as medidas na web)
- [ ] `014` (RPE por série) — o SQL está em `MIGRACOES_PENDENTES.md`
- [ ] **Storage:** criar o bucket **privado** `post_photos` (painel → Storage → New bucket, *Public* desligado)
- [ ] `docs/supabase/016_foto_no_post.sql` — foto do treino no post (libera #554)
- [ ] `docs/supabase/017_superset_e_cadencia.sql` — supersérie e cadência na nuvem (#575, #581)
- [ ] `docs/supabase/018_marca_da_serie.sql` — marca da série (aquecimento/drop/rest-pause/falha) e aquecimento fora dos recordes e do perfil (#603, #604). **Rodar depois da 012.** Leia antes: não foi testada em Postgres local.
- [ ] (opcional) `015` — restaurar histórico de backup (#538); confira os nomes das colunas antes
- Depois de cada uma, me avise: eu ligo o código (web e Android) às colunas novas.

## 2. Publicar a web na Vercel
- [ ] Criar projeto novo na Vercel apontando para o repositório, **Root Directory = `web`**, Framework **Vite** (o `vercel.json` já cuida de rotas e do service worker).
- [ ] Variáveis de ambiente do projeto: `VITE_SUPABASE_URL` (endereço do projeto) e `VITE_SUPABASE_KEY` (**chave `sb_publishable_...`, nunca a service role**).
- [ ] No Supabase → Authentication → URL Configuration: adicionar o domínio da Vercel em *Site URL* e *Redirect URLs*.
- [ ] Abrir o site no iPhone (Safari) → entrar com a sua conta → **Compartilhar → Adicionar à Tela de Início**.
- [ ] Conferir no iPhone: login, Início, registrar uma série, descanso com bip, compartilhar imagem, foto com estatísticas. (Eu só testei no navegador do computador com dados falsos.)
- Se algo der erro de dados, a tela mostra a mensagem com "Tentar de novo"; me mande o texto. As consultas foram escritas pelo esquema, **nunca rodaram contra o banco real**.

## 3. Conferir o que eu fiz (Runtime → GymTracker → coluna "Revisão")
- [ ] Passar os cards da coluna **Revisão** e mover para **Concluído** o que estiver bom (só você marca, depois de ver no aparelho).
- [ ] No celular Android: instalar o APK novo e olhar Início ("Hoje"), Evolução (Análises: equilíbrio, platôs, comparar treinos, pesquisa).
- [ ] CI: abrir a aba **Actions** do GitHub e ver se o último push passou (testes do `:app` ainda com `continue-on-error`; se estiverem verdes, peça para tirar).

## 4. Decisões suas
- [ ] **Merge:** o trabalho está no branch `claude/remover-leitura-notificacoes`. Decidir se faz PR/merge para o `master`.
- [ ] **Push na web (#534):** exige chaves VAPID e um serviço no servidor (Edge Function). Quer que eu monte o plano e o SQL/função para você aplicar?
- [ ] **Importar CSV do Hevy/Strong (#589):** me mande um arquivo de exemplo exportado (pode apagar nomes) para eu casar o formato.
- [ ] **Social novo** (Liga #569, Duelo #570, Treinar junto #572, comentários #594): confirmar que posso escrever o SQL de cada um para você aplicar.
- [ ] **Relógio / Health Connect (#597/#598):** precisam de aparelho/contas de desenvolvedor; dizer se vale a pena agora.

## 5. Lembretes de segurança
- Não colar chaves, tokens ou `local.properties` em chat nem em arquivo versionado.
- A chave que vai na Vercel é só a **publishable**.
- O `RUNTIME_TOKEN` (board) é pessoal: se vazar, revogue em Personalizar → API.
