# Setup

## Requisitos

- Android Studio compatível com AGP 9.1.1;
- JDK 17;
- Android SDK Platform 36 e Build Tools 36.0.0;
- emulador ou dispositivo somente para gates instrumentados/físicos.

Identidade: `com.soturine.replicascan`, versão `0.4.0` (`versionCode 19`).

## Primeiros passos

```powershell
git clone https://github.com/Soturine/replicascan.git
cd replicascan
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Use tasks focadas durante o desenvolvimento. Antes do push de release, execute a qualificação consolidada de `AGENTS.md`. Não use `clean` por padrão: invalida cache sem melhorar a evidência.

Os testes instrumentados compilam localmente com `assembleDebugAndroidTest`; API 36 no GitHub é o gate final. Se um aparelho/AVD já estiver pronto, `connectedDebugAndroidTest` pode complementar, mas não configure um ambiente demorado apenas para simular esse gate.
