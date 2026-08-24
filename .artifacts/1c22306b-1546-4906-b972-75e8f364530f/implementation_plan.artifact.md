# Plano: Adicionar Ícone de Voltar em Todas as Telas

Este plano descreve como adicionar um ícone de navegação "Voltar" (seta) em todas as telas que não são a Home, melhorando a interatividade e consistência do app.

## Mudanças Propostas

### 1. Navegação (App)

#### [MODIFY] [Navigation.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/Navigation.kt)
- Atualizar as chamadas das telas no `NavHost` para passar a lambda `onBack = { navController.popBackStack() }` onde ainda não é passado.

### 2. Interface de Usuário (App)

#### [MODIFY] [SignUpScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/auth/SignUpScreen.kt)
- Adicionar `TopAppBar` com ícone de voltar.

#### [MODIFY] [MuscleGroupScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/muscleGroup/MuscleGroupScreen.kt)
- Adicionar `navigationIcon` ao `TopAppBar`.

#### [MODIFY] [ExerciseScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/exercise/ExerciseScreen.kt)
- Adicionar `navigationIcon` ao `TopAppBar`.

#### [MODIFY] [ProfileScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/profile/ProfileScreen.kt)
- Adicionar `navigationIcon` ao `TopAppBar`.

#### [MODIFY] [HistoryScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/history/HistoryScreen.kt)
- Adicionar `navigationIcon` ao `TopAppBar`.

#### [MODIFY] [ActiveWorkoutScreen.kt](file:///D:/programmingProjects/AndroidCompose/GymTracker/app/src/main/java/com/vitoroliveira/gymtracker/ui/workout/ActiveWorkoutScreen.kt)
- Adicionar `navigationIcon` ao `TopAppBar`.

## Plano de Verificação

### Testes Manuais
1. Navegar da Home para "Grupos Musculares" e verificar se a seta aparece e funciona.
2. Navegar para "Perfil" e fazer o mesmo.
3. Navegar para "Histórico" e fazer o mesmo.
4. Tentar iniciar um treino e verificar se a seta aparece na tela de treino ativo.
5. Ir para a tela de Registro e verificar se a seta para voltar ao Login aparece.
