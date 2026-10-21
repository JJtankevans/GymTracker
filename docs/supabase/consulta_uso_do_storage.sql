-- Uso do Storage (plano grátis: 1 GB). Rodar no SQL Editor do Supabase; só lê, não altera nada.
-- (Não apague arquivos com DELETE em storage.objects: isso some com a linha mas pode deixar o arquivo no
--  armazenamento. Para apagar, use o painel do Storage ou a API.)

-- 1) Quanto as fotos de perfil ocupam hoje
select count(*)                                                      as arquivos,
       pg_size_pretty(coalesce(sum((metadata->>'size')::bigint), 0)) as total,
       pg_size_pretty(coalesce(avg((metadata->>'size')::bigint), 0)::bigint) as media_por_foto,
       pg_size_pretty(coalesce(max((metadata->>'size')::bigint), 0)) as maior_foto
from storage.objects
where bucket_id = 'user_profile_images';

-- 2) Quem tem mais de uma foto guardada (sobra de uma troca em que a faxina falhou; o normal é 1 por usuário)
select (storage.foldername(name))[1]                                 as usuario,
       count(*)                                                      as arquivos,
       pg_size_pretty(sum((metadata->>'size')::bigint))              as total
from storage.objects
where bucket_id = 'user_profile_images'
group by 1
having count(*) > 1
order by count(*) desc;

-- 3) Arquivos que nenhum perfil usa mais (candidatos a apagar pelo painel do Storage)
select o.name, pg_size_pretty((o.metadata->>'size')::bigint) as tamanho, o.created_at
from storage.objects o
where o.bucket_id = 'user_profile_images'
  and not exists (select 1 from public.profiles p where p.avatar_url like '%/' || o.name)
order by o.created_at;

-- 4) Visão geral de todos os buckets
select bucket_id, count(*) as arquivos, pg_size_pretty(sum((metadata->>'size')::bigint)) as total
from storage.objects
group by bucket_id
order by sum((metadata->>'size')::bigint) desc;
