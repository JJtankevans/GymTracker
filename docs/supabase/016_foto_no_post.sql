-- 016: foto do treino no post do feed (ideia "foto com estatísticas" + Social).
-- O app tira/escolhe a foto, desenha as estatísticas por cima (imagem 1080x1350) e, se o usuário quiser, anexa ao post.
-- Bucket PRIVADO `post_photos` (crie no painel: Storage > New bucket, Public DESLIGADO). Caminho: <id do usuário>/<id do post>.jpg
-- Leitura: só quem pode ver o post (mesma regra do feed: can_view_post). O app pede uma URL assinada de curta duração.
-- Escrita: só o dono, só na própria pasta. Idempotente. Rodar depois de 002 e 007. NÃO aplicado ainda (aguarda o Thiago).

do $$
begin
  if not exists (select 1 from storage.buckets where id = 'post_photos') then
    raise exception 'Crie primeiro o bucket "post_photos" em Storage (deixe Public DESLIGADO).';
  end if;
end $$;

update storage.buckets
   set public = false,
       file_size_limit = 1048576,   -- 1 MB: o app envia JPEG 1080x1350 (em geral 150-400 KB)
       allowed_mime_types = array['image/jpeg', 'image/webp']
 where id = 'post_photos';

-- Coluna no post: caminho da foto dentro do bucket (null = post sem foto). Só o dono troca, como a legenda.
alter table public.feed_posts add column if not exists photo_path text;
alter table public.feed_posts drop constraint if exists feed_posts_photo_path_len;
alter table public.feed_posts add constraint feed_posts_photo_path_len check (photo_path is null or char_length(photo_path) <= 200);
grant update (photo_path) on public.feed_posts to authenticated;

-- Políticas em storage.objects (a pasta é o id do usuário).
drop policy if exists "foto de post: dono envia na sua pasta" on storage.objects;
create policy "foto de post: dono envia na sua pasta" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'post_photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists "foto de post: dono apaga a sua" on storage.objects;
create policy "foto de post: dono apaga a sua" on storage.objects
  for delete to authenticated
  using (bucket_id = 'post_photos' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- Ler: o dono sempre; os outros só se puderem ver o post que aponta para esse arquivo.
drop policy if exists "foto de post: quem pode ver o post" on storage.objects;
create policy "foto de post: quem pode ver o post" on storage.objects
  for select to authenticated
  using (
    bucket_id = 'post_photos'
    and (
      (storage.foldername(name))[1] = (select auth.uid())::text
      or exists (
        select 1 from public.feed_posts fp
        where fp.photo_path = storage.objects.name and public.can_view_post(fp.profile_id, fp.visibility)
      )
    )
  );

-- Apagar o post apaga a linha, mas não o arquivo: o app remove o arquivo ao excluir o post (ou rode uma limpeza periódica
-- dos objetos de post_photos sem feed_posts.photo_path correspondente).
