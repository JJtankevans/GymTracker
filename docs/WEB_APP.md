# Versão web (PWA) do GymTracker — plano (22/11/2026)

**Objetivo:** um web app que funciona no iPhone (Safari → "Adicionar à Tela de Início"), com a mesma conta e os mesmos dados do app Android.

## Decisão recomendada
PWA separada em `GymTracker/web/`: **Vite + React + TypeScript + supabase-js**, mesmo projeto Supabase (`ygbrrypyydxnuskrobeu`), hospedada na Vercel.
- Por que não Compose Multiplatform Web: Wasm pesado (vários MB) e ainda irregular no Safari do iPhone; o app Android é Compose + ViewModels do `:shared`, e nada disso roda como PWA leve.
- Por que PWA nova: Runtime e Vault já usam essa pilha e rodam no iPhone; o banco e as funções (RPC) já existem; a interface mobile-first é pequena (5 abas).
- O que se reaproveita do Android: o esquema do banco, as funções do servidor, os tokens de tema, o conteúdo (catálogo de 70 exercícios, treinos prontos, conquistas) e os casos de teste das regras (fixtures JSON geradas dos testes do `:shared`).

## Limites do iPhone (documentar para o usuário)
- Sem widgets nem atalhos no ícone; push só com a PWA instalada (iOS 16.4+).
- Sem prompt de instalar: mostrar a instrução "Compartilhar → Adicionar à Tela de Início".
- A tela apaga e o JS para: cronômetros usam timestamps; o treino aberto fica em `localStorage` para sobreviver a recarregar.
- O Safari pode limpar o armazenamento local depois de semanas sem uso: a nuvem é a fonte de verdade.
- Campos com fonte < 16 px dão zoom ao focar: usar >= 16 px.

## Ordem (cards no Runtime, projeto GymTracker)
#524 esqueleto e login → #525 PWA no iPhone → #526 Home → #527 treino ativo → #528 Histórico → #529 Grupos/catálogo → #530 Evolução/Recordes/Conquistas → #531 temas → #532 testes de paridade das regras → #533 Social → #534 push → #535 backup.

## Segurança
Só a chave publicável (`sb_publishable_...`) no front; nada de service role. A RLS do Supabase continua sendo a barreira (já testada para o app Android).
