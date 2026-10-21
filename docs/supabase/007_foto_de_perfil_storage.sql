-- 007: foto de perfil no Supabase Storage (bucket PÚBLICO `user_profile_images`, criado pelo Thiago no painel).
-- O app grava cada foto em  <id do usuário>/<milissegundos>.jpg  (nome novo a cada troca, para não cair em cache)
-- e guarda a URL pública em profiles.avatar_url; as fotos antigas do usuário são apagadas depois da troca.
-- Leitura: o bucket é público, então qualquer um com o link vê a foto (é o que o Social precisa).
-- Escrita: só o dono, só dentro da própria pasta. Idempotente: pode rodar mais de uma vez. Rodar depois de 001..006.

do $$
begin
  if not exists (select 1 from storage.buckets where id = 'user_profile_images') then
    raise exception 'Crie primeiro o bucket "user_profile_images" em Storage (marque Public bucket).';
  end if;
end $$;

-- Limites no próprio bucket: até 2 MB por arquivo e só imagem (o app já envia JPEG 384x384, em geral 20-50 KB e no máximo ~100 KB; ver AvatarRules).
update storage.buckets
   set public = true,
       file_size_limit = 2097152,
       allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']
 where id = 'user_profile_images';

-- Políticas em storage.objects. A pasta é o id do usuário: (storage.foldername(name))[1].
drop policy if exists "foto de perfil: dono lista as suas" on storage.objects;
create policy "foto de perfil: dono lista as suas" on storage.objects
  for select to authenticated
  using (bucket_id = 'user_profile_images' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists "foto de perfil: dono envia na sua pasta" on storage.objects;
create policy "foto de perfil: dono envia na sua pasta" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'user_profile_images' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists "foto de perfil: dono troca a sua" on storage.objects;
create policy "foto de perfil: dono troca a sua" on storage.objects
  for update to authenticated
  using (bucket_id = 'user_profile_images' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'user_profile_images' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists "foto de perfil: dono apaga a sua" on storage.objects;
create policy "foto de perfil: dono apaga a sua" on storage.objects
  for delete to authenticated
  using (bucket_id = 'user_profile_images' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- profiles.avatar_url já existe e a RLS de profiles já deixa o dono atualizar o próprio perfil;
-- public_profiles() e challenge_standings() já devolvem avatar_url para o Social.
