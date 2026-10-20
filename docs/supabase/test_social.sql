\set ON_ERROR_STOP off
\pset pager off
\set A '''aaaaaaaa-0000-0000-0000-000000000001'''
\set B '''bbbbbbbb-0000-0000-0000-000000000002'''
\set C '''cccccccc-0000-0000-0000-000000000003'''

insert into auth.users(id,email) values (:A,'a@x.com'),(:B,'b@x.com'),(:C,'c@x.com');
select username from profiles order by username;

-- A: público, compartilha treinos e música
set role authenticated; select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', false);
update profiles set visibility='public', share_workouts=true, share_music=true where id=auth.uid();
insert into muscle_groups(profile_id,name) values (auth.uid(),'Peito');
insert into exercises(profile_id,name) values (auth.uid(),'Supino Reto');
insert into workouts(id,profile_id,started_at) values ('11111111-0000-0000-0000-000000000001', auth.uid(), now()-interval '70 minutes');
insert into workout_sets(workout_id,exercise_id,set_number,reps,weight_kg)
  select '11111111-0000-0000-0000-000000000001', id, n, 10, 80 from exercises, generate_series(1,3) n;
update workouts set finished_at=now() where id='11111111-0000-0000-0000-000000000001';
-- treino curto (10 min) pra testar anti-trapaça
insert into workouts(id,profile_id,started_at) values ('11111111-0000-0000-0000-000000000002', auth.uid(), now()-interval '10 minutes');
insert into workout_sets(workout_id,exercise_id,set_number,reps,weight_kg)
  select '11111111-0000-0000-0000-000000000002', id, 1, 10, 100 from exercises;
update workouts set finished_at=now() where id='11111111-0000-0000-0000-000000000002';
select 'A vê seus posts:' t, count(*) from feed_posts;
select 'stats:' t, stats from feed_posts order by created_at limit 1;
insert into workout_tracks(workout_id,profile_id,title,artist) values ('11111111-0000-0000-0000-000000000001', auth.uid(), 'Blinding Lights','The Weeknd'),('11111111-0000-0000-0000-000000000001', auth.uid(), 'Blinding Lights','The Weeknd');
-- A edita stats do post? deve falhar
update feed_posts set stats='{"volume_kg":999999}' ;
update feed_posts set caption='dia de peito' ;
-- grupo privado
insert into groups(id,owner_id,name,privacy) values ('99999999-0000-0000-0000-000000000001', auth.uid(), 'Galera da Academia','invite');
select 'membros do grupo (dono entrou?):' t, count(*) from group_members;
select invite_code from groups \gset
-- desafio: volume 7 dias, mín 30 min por treino, 1 treino/dia
insert into challenges(id,creator_id,group_id,title,metric,starts_at,ends_at,min_minutes_per_workout)
 values ('88888888-0000-0000-0000-000000000001', auth.uid(), '99999999-0000-0000-0000-000000000001','Volume da semana','volume_kg', now()-interval '2 days', now()+interval '5 days', 30);
select join_challenge('88888888-0000-0000-0000-000000000001');
reset role;

-- B: privado (padrão)
set role authenticated; select set_config('request.jwt.claim.sub', 'bbbbbbbb-0000-0000-0000-000000000002', false);
select 'B vê grupo privado antes de entrar (esperado 0):' t, count(*) from groups;
select 'B vê perfil de A via public_profiles (esperado 1):' t, count(*) from public_profiles where username not like 'b%';
select 'B lê perfil de A direto (esperado 0):' t, count(*) from profiles where id<>auth.uid();
select 'B vê post público de A (esperado 1 — o do treino longo e o curto = 2):' t, count(*) from feed_posts;
select 'B vê músicas de A (esperado 2):' t, count(*) from workout_tracks;
select 'top tracks de A:' t, title, plays from profile_top_tracks('aaaaaaaa-0000-0000-0000-000000000001');
select 'B entra com código ruim:' t; select join_group_by_code('zzzz');
select 'B entra com código certo:' t; select join_group_by_code(:'invite_code') is not null as ok;
select 'B vê grupo agora (esperado 1):' t, count(*) from groups;
select 'B insere direto em group_members (esperado erro):' t; insert into group_members(group_id,profile_id,role) values ('99999999-0000-0000-0000-000000000001', auth.uid(), 'owner');
select 'B vira participante do desafio:' t; select join_challenge('88888888-0000-0000-0000-000000000001');
insert into workouts(id,profile_id,started_at) values ('22222222-0000-0000-0000-000000000001', auth.uid(), now()-interval '50 minutes');
insert into exercises(profile_id,name) values (auth.uid(),'supino reto');
insert into workout_sets(workout_id,exercise_id,set_number,reps,weight_kg)
  select '22222222-0000-0000-0000-000000000001', id, n, 8, 60 from exercises, generate_series(1,2) n;
update workouts set finished_at=now() where id='22222222-0000-0000-0000-000000000001';
select 'B terminou treino, sem share → posts de B (esperado 0 próprios):' t, count(*) from feed_posts where profile_id=auth.uid();
select 'placar:' t, username, score, rank from challenge_standings('88888888-0000-0000-0000-000000000001');
select 'B reage no post de A:' t; insert into post_reactions select id, auth.uid(), 'fire' from feed_posts where profile_id<>auth.uid() limit 1;
select 'B tenta reagir como outro (esperado erro):' t; insert into post_reactions select id, 'aaaaaaaa-0000-0000-0000-000000000001', 'flex' from feed_posts limit 1;
reset role;

-- C: não participa
set role authenticated; select set_config('request.jwt.claim.sub', 'cccccccc-0000-0000-0000-000000000003', false);
select 'C vê grupo (esperado 0):' t, count(*) from groups;
select 'C vê desafio (esperado 0):' t, count(*) from challenges;
select 'C pede placar (esperado 0 linhas):' t, count(*) from challenge_standings('88888888-0000-0000-0000-000000000001');
select 'C lê workouts de A (esperado 0):' t, count(*) from workouts;
select 'C vê view de progressão (security_invoker; esperado 0):' t, count(*) from vw_exercise_progression;
reset role;

-- A desliga música → B não vê mais
set role authenticated; select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', false);
update profiles set share_music=false where id=auth.uid();
select 'placar visto por A:' t, username, score from challenge_standings('88888888-0000-0000-0000-000000000001');
reset role;
set role authenticated; select set_config('request.jwt.claim.sub', 'bbbbbbbb-0000-0000-0000-000000000002', false);
select 'B vê músicas após A desligar (esperado 0):' t, count(*) from workout_tracks;
select 'top tracks após desligar (esperado 0):' t, count(*) from profile_top_tracks('aaaaaaaa-0000-0000-0000-000000000001');
select 'stats de A:' t, profile_stats('aaaaaaaa-0000-0000-0000-000000000001');
-- B segue C (privado): pendente
insert into follows(follower_id,followed_id) values (auth.uid(), 'cccccccc-0000-0000-0000-000000000003');
select 'B→C status (esperado pending):' t, status from follows;
insert into follows(follower_id,followed_id) values (auth.uid(), 'aaaaaaaa-0000-0000-0000-000000000001');
select 'B→A status (esperado accepted, A é público):' t, status from follows where followed_id='aaaaaaaa-0000-0000-0000-000000000001';
