# Scanora

![Android](https://img.shields.io/badge/platform-Android-2E7D8C)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-23414B)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.03.00-DD8A2E)
![Version](https://img.shields.io/badge/version-0.3.0-E95F0C)
[![Android CI](https://github.com/Soturine/scanora/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Soturine/scanora/actions/workflows/android-ci.yml)
[![Deploy Pages](https://github.com/Soturine/scanora/actions/workflows/pages.yml/badge.svg)](https://github.com/Soturine/scanora/actions/workflows/pages.yml)

Scanora é um app Android de digitalização de documentos com foco em processamento local, OCR no dispositivo e um fluxo direto entre captura, revisão e exportação.

Repositório: https://github.com/Soturine/scanora  
Releases: https://github.com/Soturine/scanora/releases  
Site: https://soturine.github.io/scanora/

## O que o app já faz

- scanner rápido com `ML Kit Document Scanner` como fluxo principal direto na Home;
- importação de galeria pelo fluxo do Google quando suportada;
- captura manual com `CameraX` e importação direta como fallback editável;
- Home minimalista sem escolha obrigatória de tipo antes do scan;
- onboarding arrastável, ícone do app e estados contextuais com a raposa mascote do Scanora;
- interface em 12 idiomas: English, Português do Brasil, Español, Français, Italiano, العربية, Deutsch, Bahasa Indonesia, हिन्दी, Türkçe, 日本語 e 한국어, com RTL real no árabe;
- cópia das imagens de entrada para armazenamento interno antes de criar o lote local;
- detector local de documento com perfis geral/caderno/recibo, confiança tipada, `NO_DOCUMENT` e fallback conservador;
- reajuste automático do crop e editor manual mais confortável para acertos finos;
- filtros locais recalibrados para documento, cinza, cor e recibo com menos risco de estourar a página;
- pipeline de imagem unificado para preview, filtros, OCR e exportação derivarem da mesma página lógica;
- OCR local por script (latino, devanágari, japonês e coreano), com readiness do modelo, estrutura, fingerprint e artefato persistido;
- busca local por título, tags e OCR via Room FTS;
- exportação em PDF pesquisável quando há OCR, JPG e PNG, com A4/Letter/Auto e escolha progressiva por formato;
- pós-exportação com nome, tipo, tamanho, local salvo, abrir e compartilhar;
- histórico local com título, tags, favoritos e busca.
- banco sem migration destrutiva, schema versionado e lifecycle explícito dos arquivos privados;
- importação parcial com contagem de falhas, rollback e preservação da ordem;
- cache visual descartável com fallback para a fonte canônica;
- backup automático desativado e compartilhamento restrito aos diretórios de export.

## Proposta de valor

Scanora foi pensado para transformar páginas, contratos, cadernos e recibos em arquivos legíveis sem depender de upload obrigatório. O scanner rápido é o caminho principal, sem exigir que o usuário escolha o tipo antes de capturar, e o fluxo manual continua disponível quando o documento precisa de ajuste fino.

## Capturas

Capturas oficiais do app em aparelho real seguem em validação final.  
Nesta rodada, o material público foi alinhado ao fluxo real do produto sem substituir essa etapa por mockups artificiais.

## Stack

- Kotlin
- Android Gradle Plugin 9.1.1
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + Coroutines + Flow
- Room
- DataStore
- WorkManager
- CameraX
- ML Kit Document Scanner
- ML Kit Text Recognition

## Arquitetura

- `app`: bootstrap, navegação, onboarding e integração dos módulos
- `core-common`: modelos, contratos e use cases
- `core-data`: Room, DataStore, OCR, exportação e processamento de imagem
- `core-ui`: tema e componentes reutilizáveis
- `feature-*`: telas e ViewModels por contexto funcional

Referências técnicas:

- [docs/architecture.md](docs/architecture.md)
- [docs/current-state.md](docs/current-state.md)
- [docs/data-lifecycle.md](docs/data-lifecycle.md)
- [docs/threat-model.md](docs/threat-model.md)
- [docs/decisions.md](docs/decisions.md)
- [docs/setup.md](docs/setup.md)
- [docs/testing.md](docs/testing.md)
- [docs/publishing.md](docs/publishing.md)

## Como rodar

1. Abra o projeto no Android Studio com suporte a AGP 9.1.1.
2. Use JDK 17 ou superior compatível com AGP 9.
3. Instale Android SDK Platform 36 e Build Tools 36.0.0.
4. Rode `./gradlew assembleDebug` ou execute o módulo `app`.

Identidade do app:

- `applicationId`: `com.soturine.scanora`
- namespace base: `com.soturine.scanora`

## CI e Pages

- O workflow [Android CI](https://github.com/Soturine/scanora/actions/workflows/android-ci.yml) roda `check`, compila debug/release e executa os testes instrumentados de Room, busca e PDF em emulador API 35.
- CodeQL analisa Java/Kotlin e o setup Gradle valida o wrapper; Dependabot acompanha Gradle e GitHub Actions.
- O site público é publicado a partir de `site/`.
- Para o GitHub Pages funcionar no repositório publicado, ative em `Settings > Pages > Source: GitHub Actions`.

## Privacidade

- processamento local por padrão;
- OCR e filtros executados no dispositivo sempre que possível;
- sem backend obrigatório, login ou sincronização no MVP.
- scans, Room, OCR e preferências excluídos de cloud backup e device transfer automáticos;
- excluir uma página ou lote remove os arquivos privados gerenciados, mas preserva fotos externas e exports do usuário.

Política completa em [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Status

`0.3.0` transforma detecção, OCR, busca e exportação em um motor documental verificável: decisões de crop têm confiança explícita, OCR vira dado versionado e pesquisável, e o PDF pode carregar texto local sem alterar a aparência da página aprovada.

## Contribuir

Consulte [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), [SECURITY.md](SECURITY.md) e [ROADMAP.md](ROADMAP.md).
