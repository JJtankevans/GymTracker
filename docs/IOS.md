# GymTracker para iPhone — situação e plano (28/10/2026)

O Thiago pediu "a versão iOS" para distribuir a outras pessoas. Este arquivo diz o que já existe, o que falta e o caminho sugerido.

## O que já existe
- **O módulo `:shared` é Kotlin Multiplatform** e já declara os alvos iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`, framework estático `shared`). Lá estão os modelos, os repositórios do Supabase (Ktor com motor `darwin` no iOS), os casos de uso, os ViewModels e as regras (treino, circuito, recordes, social, relógio).
- **CI de iOS** (`.github/workflows/ios.yml`, criado em 28/10/2026): num Mac do GitHub (grátis, o repositório é público) compila o `:shared` para iPhone, roda os mesmos testes do Android no simulador de iOS e gera o framework. Depois compila o **app de iPhone**, abre no simulador, tira uma captura (artefato `iphone-captura`) e lê o texto da tela por OCR (aparece como aviso na execução). Roda sozinho quando `shared/`, `composeApp/` ou `iosApp/` mudam, ou à mão em Actions → "iOS (shared)" → Run workflow.
- **App de iPhone mínimo** (28/10/2026, Compose Multiplatform):
  - `composeApp/` (só alvos iOS; usa os ViewModels e repositórios do `:shared` sem Koin): Login/criar conta, Início (semana x meta, sequência, grupos com o sugerido destacado, treino livre), treino ativo (séries por exercício com a última carga e o recorde, descanso 60/90/120 s, terminar com resumo e recordes), Histórico e Perfil (sair). Tema escuro com acento limão.
  - `iosApp/`: casca SwiftUI; o projeto Xcode é gerado pelo **XcodeGen** a partir de `iosApp/project.yml` (o `.xcodeproj` não vai para o git). As chaves do Supabase vêm do `local.properties` ou das variáveis `SUPABASE_URL`/`SUPABASE_KEY` (no CI, os mesmos secrets do Android) e viram `AppConfig.kt` gerado.
  - Num Mac: `brew install xcodegen`, `cd iosApp && xcodegen generate`, abrir `GymTracker.xcodeproj` no Xcode e rodar no simulador (ou no próprio iPhone com o Apple ID).

## O que falta (o grosso)
- **As telas.** Todo o `:app` é Jetpack Compose com APIs de Android (notificações, vibração, câmera/galeria, SharedPreferences, Koin Android, Navigation do Android, Vico). Nada disso roda no iPhone como está.
- **Coisas de plataforma:** armazenamento local (peso, agenda, medidas, tema), notificações do descanso, vibração, compartilhar, escolher foto, sessão do Supabase salva no aparelho, fontes.
- **Conta Apple e Mac.** Para instalar num iPhone de outra pessoa é preciso assinar o app com uma conta Apple Developer (US$ 99/ano) e distribuir pelo **TestFlight** (até 10.000 testadores por link). Com um Apple ID grátis dá para instalar só no próprio iPhone, pelo Xcode num Mac, e o app expira em 7 dias.

## Caminhos
1. **Compose Multiplatform (recomendado).** As telas já são Compose; o Compose Multiplatform roda o mesmo código no iOS. Plano: criar um módulo `:composeApp` com `commonMain` e ir movendo as telas do `:app` para lá, trocando o que é só Android por `expect/actual` (armazenamento, vibração, notificação, compartilhar, foto) e por bibliotecas multiplataforma (navegação do Compose Multiplatform, Koin multiplataforma, gráficos próprios em vez do Vico). O app Android passa a ser uma casca que chama o `:composeApp`; o iOS ganha um projeto Xcode pequeno (`iosApp/`) que mostra o `ComposeUIViewController`. Vantagem: uma tela só para os dois. Custo: mover 40 temas, ~30 telas e as decorações em Canvas (que funcionam no Compose Multiplatform porque não usam Android, exceto as que usam `nativeCanvas`, como o Matrix).
2. **SwiftUI usando o `:shared`.** Telas novas em Swift chamando os ViewModels/repositórios do framework `shared`. Visual nativo de iPhone, mas é escrever tudo de novo e manter duas interfaces.

## Ordem sugerida (Compose Multiplatform)
1. CI de iOS verde (feito: compila e testa o `:shared`).
2. Módulo `:composeApp` com tema, Login e Home; projeto `iosApp` mínimo; rodar no simulador pelo CI (feito em 28/10: ver a situação do build na seção "Não testado").
3. Treino ativo, histórico, recordes; armazenamento local multiplataforma (ex.: `multiplatform-settings`).
4. Social, temas animados, perfil e foto.
5. Conta Apple Developer + TestFlight (o CI pode subir o build com as chaves da Apple guardadas em secrets).
6. Apple Watch (watchOS) seria outro app, em SwiftUI, conversando com o iPhone pelo WatchConnectivity; fica para depois.

## Situação do build (28/10/2026, fim da noite)
- `:shared` compila para iPhone e **todos os testes passam no simulador de iOS**.
- O **app de iPhone compila** (precisa do **Xcode 26**: o Compose Multiplatform 1.11 usa APIs do iOS 26, ex.: `UIViewLayoutRegion`; o workflow escolhe o Xcode 26 do runner), **instala e abre no simulador e continua aberto depois de 35 s**. A captura tem conteúdo (cerca de 11% da tela clara sobre o fundo escuro, o que bate com a tela de login). O OCR serve para confirmar o texto; o resultado aparece nos avisos da execução.
- Abas: Início, Histórico, Recordes e Perfil, mais o treino ativo.

## Não testado
Nada de iOS foi visto num iPhone de verdade, nem por uma pessoa no simulador (o CI só abre o app e tira uma captura). O login não foi exercitado no CI (não digitamos senha de conta real). Falta ver: entrar com a conta, carregar grupos/semana, registrar séries, terminar o treino e ver o resumo.
