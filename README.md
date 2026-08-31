# ReplicaScan

![Android](https://img.shields.io/badge/platform-Android-2E7D8C)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-23414B)
![Version](https://img.shields.io/badge/version-0.4.0-E95F0C)
[![Android CI](https://github.com/Soturine/replicascan/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Soturine/replicascan/actions/workflows/android-ci.yml)
[![Deploy Pages](https://github.com/Soturine/replicascan/actions/workflows/pages.yml/badge.svg)](https://github.com/Soturine/replicascan/actions/workflows/pages.yml)

ReplicaScan é um scanner Android local-first: captura ou importa documentos, corrige perspectiva, revisa, executa OCR no aparelho e exporta em PDF, JPG ou PNG.

- Repositório: <https://github.com/Soturine/replicascan>
- Releases: <https://github.com/Soturine/replicascan/releases>
- Site: <https://soturine.github.io/replicascan/>

## Produto

- câmera própria com CameraX, detecção ao vivo, foco, flash e lote multipágina;
- ML Kit Document Scanner como alternativa assistida;
- crop perspectivo com fallback conservador e ajuste manual dos quatro cantos;
- cinco filtros com intenção clara e pipeline coerente entre preview e exportação;
- OCR local com qualidade explícita, trechos legíveis e cópia rápida;
- PDF pesquisável quando há OCR, JPG e PNG, com opções progressivas por formato;
- histórico local com título, tags, favoritos e busca Room FTS;
- 12 idiomas, incluindo árabe com RTL;
- raposa oficial consistente no launcher, onboarding e estados de processamento.

Não há conta, backend, sincronização ou upload obrigatório de documentos. Componentes do Google Play services/ML Kit podem contatar o Google para atualizações e métricas técnicas conforme os termos do fornecedor; veja [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Identidade Android

- produto: `ReplicaScan`
- versão: `0.4.0` (`versionCode 19`)
- `applicationId`: `com.soturine.replicascan`
- namespace base: `com.soturine.replicascan`
- exportações em Android 10+: `Downloads/ReplicaScan`

O novo `applicationId` faz a v0.4.0 instalar como um app diferente das builds históricas. Exporte documentos importantes da instalação antiga antes de removê-la.

## Arquitetura

O projeto é um monólito modular em Kotlin, Jetpack Compose e Material 3:

- `app`: bootstrap, navegação, onboarding e composição;
- `core-common`: modelos e contratos centrais;
- `core-data`: Room, DataStore, OCR, imagem e exportação;
- `core-ui`: tema e componentes reutilizáveis;
- `feature-*`: câmera, home, editor, exportação, histórico, OCR e configurações.

Referências: [arquitetura](docs/architecture.md), [estado atual](docs/current-state.md), [lifecycle dos dados](docs/data-lifecycle.md), [threat model](docs/threat-model.md), [setup](docs/setup.md), [testes](docs/testing.md) e [constituição de engenharia](docs/engineering/ENGINEERING_CONSTITUTION.md).

## Desenvolvimento

Requisitos: JDK 17, Android SDK Platform 36 e Build Tools 36.0.0.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
python tools/check_localization.py
python tools/check_branding.py
python tools/check_consistency.py
```

O pipeline de `main` valida, produz uma única vez o APK de avaliação, testa em API 36 e só então cria a tag anotada e a GitHub Release. API 35 permanece como verificação de compatibilidade agendada. Consulte [docs/release.md](docs/release.md).

## Licença

O código corrente do ReplicaScan é proprietário e disponibilizado para visualização pública sob [LICENSE](LICENSE). Todos os direitos são reservados; disponibilidade pública do código não significa open source.

As releases históricas até `v0.3.1`, publicadas sob o nome anterior, permanecem sob Apache License 2.0 em seus respectivos commits, tags e artefatos. A fronteira e os componentes de terceiros estão documentados em [LICENSING.md](LICENSING.md) e [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Veja também [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [CHANGELOG.md](CHANGELOG.md) e [ROADMAP.md](ROADMAP.md).
