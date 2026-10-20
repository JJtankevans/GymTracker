-- 005: perfis "Só seguidores" passam a ser ENCONTRÁVEIS (nome e @usuário) para poder pedir para seguir.
-- Antes: public_profiles() só devolvia perfis que o usuário já podia ver; um perfil "Só seguidores" sumia da busca
-- para quem ainda não o segue, então ninguém conseguia mandar o pedido (o trigger trg_follow_status já cria o
-- pedido como 'pending' e a tela de Pessoas já lista/aceita pedidos).
-- O que continua escondido para quem não segue: bio, números (profile_stats), posts e músicas.
-- Perfis "Privado" continuam invisíveis. Idempotente. Rodar no SQL Editor do Supabase.

create or replace function public.public_profiles()
returns table (id uuid, username text, full_name text, avatar_url text, bio text, visibility text, created_at timestamptz)
language sql stable security definer set search_path = public
as $$
  select p.id, p.username, p.full_name, p.avatar_url,
         case when public.can_view_profile(p.id) then p.bio else null end,
         p.visibility::text, p.created_at
    from public.profiles p
   where public.can_view_profile(p.id) or p.visibility = 'followers';
$$;

revoke all on function public.public_profiles() from public, anon;
grant execute on function public.public_profiles() to authenticated;
