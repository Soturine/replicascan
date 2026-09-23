# ReplicaScan

![Android](https://img.shields.io/badge/platform-Android-2E7D8C)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-23414B)
![Version](https://img.shields.io/badge/version-0.4.0-E95F0C)
[![Android CI](https://github.com/Soturine/replicascan/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Soturine/replicascan/actions/workflows/android-ci.yml)
[![Deploy Pages](https://github.com/Soturine/replicascan/actions/workflows/pages.yml/badge.svg)](https://github.com/Soturine/replicascan/actions/workflows/pages.yml)

ReplicaScan é um scanner Android local-first: escaneia com o ML Kit Document Scanner, guarda as páginas só no aparelho, reconhece texto no próprio dispositivo e exporta PDF pesquisável, JPG ou PNG. Versão atual: v0.4.0.

- Repositório: <https://github.com/Soturine/replicascan>
- Releases: <https://github.com/Soturine/replicascan/releases>
- Site: <https://soturine.github.io/replicascan/>

## Produto

- **Escanear** abre o ML Kit Document Scanner (bordas, perspectiva, filtros e limpeza no aparelho, multipágina);
- importação pela galeria e, se o scanner não estiver disponível, foto pela câmera do sistema com crop manual;
- revisão com o documento em destaque: cortar, girar, ajustar visual (Original, Realçado, Cinza, Preto e branco) e texto;
- OCR no aparelho com um reconhecedor por idioma de escrita, artefato estruturado e cópia rápida;
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
- `feature-*`: home (scanner), editor, exportação, histórico, OCR e configurações.

Referências: [arquitetura](docs/architecture.md), [estado atual](docs/current-state.md), [lifecycle dos dados](docs/data-lifecycle.md), [threat model](docs/threat-model.md), [setup](docs/setup.md), [testes](docs/testing.md) e [constituição de engenharia](docs/engineering/ENGINEERING_CONSTITUTION.md).

## Desenvolvimento

Requisitos: JDK 17+, Android SDK Platform 36 e Build Tools 36.x. O scanner e o OCR usam Google Play services no aparelho.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lint
python tools/check_localization.py
python tools/check_branding.py
python tools/check_consistency.py
```

Cada push em `main` é qualificado pelo GitHub Actions (build, lint, testes, API 36 em dispositivo gerenciado, site e CodeQL). A release de avaliação é publicada pelo proprietário com `tools/release_artifact.py` e verificada pelo workflow ao receber a tag; o CI nunca cria tags. Consulte [docs/release.md](docs/release.md) e o [ADR 0003](docs/adr/0003-owner-published-evaluation-releases.md).

## Licença

O código corrente do ReplicaScan é proprietário e disponibilizado para visualização pública sob [LICENSE](LICENSE). Todos os direitos são reservados; disponibilidade pública do código não significa open source.

As releases históricas até `v0.3.1`, publicadas sob o nome anterior, permanecem sob Apache License 2.0 em seus respectivos commits, tags e artefatos. A fronteira e os componentes de terceiros estão documentados em [LICENSING.md](LICENSING.md) e [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Veja também [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [CHANGELOG.md](CHANGELOG.md) e [ROADMAP.md](ROADMAP.md).
