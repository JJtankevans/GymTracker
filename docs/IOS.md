# GymTracker para iPhone — situação e plano (28/10/2026)

O Thiago pediu "a versão iOS" para distribuir a outras pessoas. Este arquivo diz o que já existe, o que falta e o caminho sugerido.

## O que já existe
- **O módulo `:shared` é Kotlin Multiplatform** e já declara os alvos iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`, framework estático `shared`). Lá estão os modelos, os repositórios do Supabase (Ktor com motor `darwin` no iOS), os casos de uso, os ViewModels e as regras (treino, circuito, recordes, social, relógio).
- **CI de iOS** (`.github/workflows/ios.yml`, criado em 28/10/2026): num Mac do GitHub (grátis, o repositório é público) compila o `:shared` para iPhone, roda os mesmos testes do Android no simulador de iOS e gera o framework. Roda sozinho quando o `:shared` muda, ou à mão em Actions → "iOS (shared)" → Run workflow.

## O que falta (o grosso)
- **As telas.** Todo o `:app` é Jetpack Compose com APIs de Android (notificações, vibração, câmera/galeria, SharedPreferences, Koin Android, Navigation do Android, Vico). Nada disso roda no iPhone como está.
- **Coisas de plataforma:** armazenamento local (peso, agenda, medidas, tema), notificações do descanso, vibração, compartilhar, escolher foto, sessão do Supabase salva no aparelho, fontes.
- **Conta Apple e Mac.** Para instalar num iPhone de outra pessoa é preciso assinar o app com uma conta Apple Developer (US$ 99/ano) e distribuir pelo **TestFlight** (até 10.000 testadores por link). Com um Apple ID grátis dá para instalar só no próprio iPhone, pelo Xcode num Mac, e o app expira em 7 dias.

## Caminhos
1. **Compose Multiplatform (recomendado).** As telas já são Compose; o Compose Multiplatform roda o mesmo código no iOS. Plano: criar um módulo `:composeApp` com `commonMain` e ir movendo as telas do `:app` para lá, trocando o que é só Android por `expect/actual` (armazenamento, vibração, notificação, compartilhar, foto) e por bibliotecas multiplataforma (navegação do Compose Multiplatform, Koin multiplataforma, gráficos próprios em vez do Vico). O app Android passa a ser uma casca que chama o `:composeApp`; o iOS ganha um projeto Xcode pequeno (`iosApp/`) que mostra o `ComposeUIViewController`. Vantagem: uma tela só para os dois. Custo: mover 40 temas, ~30 telas e as decorações em Canvas (que funcionam no Compose Multiplatform porque não usam Android, exceto as que usam `nativeCanvas`, como o Matrix).
2. **SwiftUI usando o `:shared`.** Telas novas em Swift chamando os ViewModels/repositórios do framework `shared`. Visual nativo de iPhone, mas é escrever tudo de novo e manter duas interfaces.

## Ordem sugerida (Compose Multiplatform)
1. CI de iOS verde (feito: compila e testa o `:shared`).
2. Módulo `:composeApp` com tema, Login e Home; projeto `iosApp` mínimo; rodar no simulador pelo CI (`xcodebuild` no Mac do GitHub).
3. Treino ativo, histórico, recordes; armazenamento local multiplataforma (ex.: `multiplatform-settings`).
4. Social, temas animados, perfil e foto.
5. Conta Apple Developer + TestFlight (o CI pode subir o build com as chaves da Apple guardadas em secrets).
6. Apple Watch (watchOS) seria outro app, em SwiftUI, conversando com o iPhone pelo WatchConnectivity; fica para depois.

## Não testado
Nada de iOS foi visto num iPhone. O CI de iOS só garante que a lógica compila e que os testes passam no simulador.
