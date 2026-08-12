# Setup

## Requisitos

- Android Studio recente com suporte a AGP 9.1.1
- JDK 17 ou superior compatível com AGP 9.1.1
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Emulador Android recente ou dispositivo físico
- `applicationId`: `com.soturine.scanora`

## Passos

1. Clone o repositório.
2. Abra a pasta raiz no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Confirme que o IDE está usando um JDK 17+ compatível.
5. Instale as plataformas Android exigidas se o Studio solicitar.
6. Rode a configuração `app`.

## Comandos úteis

```bash
./gradlew assembleDebug
./gradlew lint
./gradlew testDebugUnitTest
```

## Observações do ambiente desta entrega

- Android SDK instalado em `C:\Users\rafael\AppData\Local\Android\Sdk`
- Pacotes validados: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`
- Validação executada com:

```powershell
$env:JAVA_HOME="C:\Users\rafael\AppData\Local\Programs\Microsoft\jdk-17.0.10.7-hotspot"
$env:ANDROID_SDK_ROOT="C:\Users\rafael\AppData\Local\Android\Sdk"
.\gradlew.bat clean
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
.\gradlew.bat check
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat assembleDebugAndroidTest
```

- Os sete gates passaram nesta máquina em `2026-08-12` com a versão `0.2.7`.
- Os APKs de teste instrumentado foram compilados; a execução em Android ficou pendente porque não havia dispositivo ou AVD disponível.
