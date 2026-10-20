-- 004: troca a VIEW public_profiles (SECURITY DEFINER, alerta do Supabase) por uma FUNÇÃO equivalente.
-- Motivo: outros usuários precisam ver só colunas inofensivas do perfil (nome, avatar, bio) respeitando a
-- visibilidade, mas a RLS de `profiles` só deixa cada um ler o próprio. A view ignorava a RLS de propósito;
-- a função faz o mesmo, de forma explícita, com search_path fixo e sem acesso para anon.
-- Uso no app: supabase.postgrest.rpc("public_profiles") (aceita filtros como .eq("username", ...)).
-- Idempotente.

drop view if exists public.public_profiles;

create or replace function public.public_profiles()
returns table (id uuid, username text, full_name text, avatar_url text, bio text, visibility text, created_at timestamptz)
language sql stable security definer set search_path = public
as $$
  select p.id, p.username, p.full_name, p.avatar_url, p.bio, p.visibility::text, p.created_at
    from public.profiles p
   where public.can_view_profile(p.id);
$$;

revoke all on function public.public_profiles() from public, anon;
grant execute on function public.public_profiles() to authenticated;
