-- 016: excluir a própria conta (exigência da LGPD e da Play Store para apps com cadastro).
-- O app chama rpc("delete_my_account") depois de remover a foto de perfil pelo Storage (o Supabase não deixa apagar
-- arquivos por SQL). Apagar a linha de auth.users leva junto, em cascata, o perfil e TUDO que é do usuário: grupos,
-- exercícios, treinos e séries, peso e medidas, seguidores, posts, reações, participações, treinos compartilhados
-- e as faixas de música antigas. Desafios e grupos que ele CRIOU somem para todos (creator_id/owner_id em cascata).
-- Sem esta migração o app continua funcionando: o botão "Excluir minha conta" só mostra um aviso de erro.
-- Idempotente. Não precisa de nenhuma outra migração antes.

create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'Entre na sua conta para excluí-la' using errcode = '28000';
  end if;
  delete from auth.users where id = uid;
end;
$$;

-- só quem está logado chama; o definer é o dono da função (que pode apagar em auth.users)
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
